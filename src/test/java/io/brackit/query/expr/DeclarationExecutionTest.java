package io.brackit.query.expr;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.time.Duration;

import io.brackit.query.ErrorCode;
import io.brackit.query.Query;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.compiler.Bits;
import io.brackit.query.function.AbstractFunction;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.module.StaticContext;
import io.brackit.query.operator.TupleImpl;
import io.brackit.query.block.FJControl;
import io.brackit.query.util.forkjoin.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DeclarationExecutionTest extends XQueryBaseTest {
  @ParameterizedTest
  @ValueSource(strings = { "exists($x) and ($x is $$)", "$x is $$", "($x is $$) and exists($x)" })
  void globalsAndDefaultContextShareEachExecution(String body) {
    Query query = new Query("declare context item as node() := <n/>; declare variable $x := $$; " + body);
    for (int i = 0; i < 3; i++) {
      ResultChecker.dCheck(Bool.TRUE, query.execute(ctx));
      ResultChecker.dCheck(Bool.TRUE, query.evaluate(ctx));
      assertFalse(ctx.isBound(new QNm("x")));
    }
  }

  @Test
  void scalarGlobalReadsPreserveNodeIdentity() {
    Query query = new Query("declare variable $x := <n/>; $x is $x");
    ResultChecker.dCheck(Bool.TRUE, query.execute(ctx));
    ResultChecker.dCheck(Bool.TRUE, query.execute(ctx));
  }

  @Test
  void overlappingLazyExecutionsKeepTheirOwnGlobals() {
    Query query = new Query("declare context item as node() := <n/>; declare variable $x := $$; ($x, $$, $x)");
    try (Iter first = query.execute(ctx).iterate()) {
      Item firstNode = first.next();
      try (Iter second = query.execute(ctx).iterate()) {
        Item secondNode = second.next();
        assertNotNull(firstNode);
        assertNotNull(secondNode);
        assertNotSame(firstNode, secondNode);
        assertSame(firstNode, first.next());
        assertSame(secondNode, second.next());
        assertSame(firstNode, first.next());
        assertSame(secondNode, second.next());
        assertNull(first.next());
        assertNull(second.next());
      }
    }
  }

  @Test
  void externalValuesIncludingEmptyOverrideFallbackInBothConsumers() {
    QNm name = new QNm("x");
    Query query = new Query("declare variable $x as item()? external := error(); empty($x) and string($x) eq ''");
    ctx.bind(name, null);
    ResultChecker.dCheck(Bool.TRUE, query.execute(ctx));
    ResultChecker.dCheck(Bool.TRUE, query.execute(ctx));
    assertTrue(ctx.isBound(name));
    assertNull(ctx.resolve(name));
    Query stringQuery = new Query("declare variable $x as xs:string external := error(); string($x)");
    ctx.bind(name, new Str("first"));
    ResultChecker.dCheck(new Str("first"), stringQuery.execute(ctx));
    ctx.bind(name, new Str("second"));
    ResultChecker.dCheck(new Str("second"), stringQuery.execute(ctx));
    ctx.bind(name, Int32.ONE);
    QueryException error = assertThrows(QueryException.class, () -> stringQuery.evaluate(ctx));
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, error.getCode());
  }

  @Test
  void externalFallbackTracksTheExecutionContext() {
    Query query = new Query("declare variable $x as xs:string external := string($$); string($x)");
    ctx.setContextItem(new Str("first"));
    ResultChecker.dCheck(new Str("first"), query.execute(ctx));
    ctx.setContextItem(new Str("second"));
    ResultChecker.dCheck(new Str("second"), query.execute(ctx));
    ctx.setContextItem(null);
    QueryException error = assertThrows(QueryException.class, () -> query.evaluate(ctx));
    assertEquals(ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED, error.getCode());
  }

  @Test
  void emptyInitializerRunsOncePerExecutionAndRemainsLazy() {
    AtomicInteger calls = new AtomicInteger();
    ctx.bind(new QNm("init"), new AbstractFunction(new QNm("empty-initializer"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
      @Override
      public Sequence execute(StaticContext sctx, QueryContext context, Sequence[] args) {
        assertSame(ctx, context);
        calls.incrementAndGet();
        return null;
      }
    });
    String declarations = "declare variable $init external; declare variable $x := $init(); ";
    Query unused = new Query(declarations + "true()");
    ResultChecker.dCheck(Bool.TRUE, unused.execute(ctx));
    assertEquals(0, calls.get());
    Query used = new Query(declarations + "empty($x) and string($x) eq '' and empty($x)");
    ResultChecker.dCheck(Bool.TRUE, used.execute(ctx));
    assertEquals(1, calls.get());
    ResultChecker.dCheck(Bool.TRUE, used.execute(ctx));
    assertEquals(2, calls.get());
  }

  private List<Item> concurrentRead(Query query, CyclicBarrier firstRead) throws Exception {
    try (Iter iter = query.execute(ctx).iterate()) {
      Item first = iter.next();
      firstRead.await(10, TimeUnit.SECONDS);
      Item second = iter.next();
      Item third = iter.next();
      assertNull(iter.next());
      return List.of(first, second, third);
    }
  }

  @Test
  void globalInitializerCanReadAnotherDeclarationOnAWorker() {
    AtomicReference<DefaultCtxItem> contextItem = new AtomicReference<>();
    AtomicInteger calls = new AtomicInteger();
    ctx.bind(new QNm("init"), new AbstractFunction(new QNm("worker-initializer"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
      @Override
      public Sequence execute(StaticContext sctx, QueryContext context, Sequence[] args) {
        assertSame(ctx, context);
        calls.incrementAndGet();
        Item[] result = new Item[1];
        Task task = new Task() {
          @Override
          protected void doCompute() {
            result[0] = contextItem.get().evaluateToItem(context, TupleImpl.EMPTY_TUPLE);
          }
        };
        FJControl.submit(task).join();
        assertNull(task.getError());
        return result[0];
      }
    });
    Query query = new Query("declare context item as node() := <n/>; declare variable $init external; "
        + "declare variable $x := $init(); exists($x) and ($x is $$) and ($x is $x)");
    contextItem.set((DefaultCtxItem) query.getModule().getVariables().resolve(Bits.FS_DOT));
    assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
      ResultChecker.dCheck(Bool.TRUE, query.execute(ctx));
      ResultChecker.dCheck(Bool.TRUE, query.execute(ctx));
    });
    assertEquals(2, calls.get());
  }

  @Test
  void concurrentExecutionsOnTheSameBackendHaveSeparateDeclarationCaches() throws Exception {
    Query query = new Query("declare context item as node() := <n/>; declare variable $x := $$; ($x, $$, $x)");
    CyclicBarrier firstRead = new CyclicBarrier(2);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> concurrentRead(query, firstRead));
      var second = executor.submit(() -> concurrentRead(query, firstRead));
      List<Item> firstValues = first.get(15, TimeUnit.SECONDS);
      List<Item> secondValues = second.get(15, TimeUnit.SECONDS);
      assertSame(firstValues.get(0), firstValues.get(1));
      assertSame(firstValues.get(0), firstValues.get(2));
      assertSame(secondValues.get(0), secondValues.get(1));
      assertSame(secondValues.get(0), secondValues.get(2));
      assertNotSame(firstValues.get(0), secondValues.get(0));
    }
  }
}
