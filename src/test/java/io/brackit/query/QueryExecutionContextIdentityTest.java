package io.brackit.query;

import java.util.concurrent.atomic.AtomicInteger;

import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.QNm;
import io.brackit.query.block.FJControl;
import io.brackit.query.compiler.Bits;
import io.brackit.query.expr.DefaultCtxItem;
import io.brackit.query.function.AbstractFunction;
import io.brackit.query.function.DynamicFunctionExpr;
import io.brackit.query.function.FunctionExpr;
import io.brackit.query.jdm.Function;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jdm.node.NodeStore;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.module.MainModule;
import io.brackit.query.module.StaticContext;
import io.brackit.query.operator.TupleImpl;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.LazySequence;
import io.brackit.query.util.forkjoin.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutionContextIdentityTest extends XQueryBaseTest {
  private static final class BackendContext extends BrackitQueryContext {
    private final int marker = 123;

    BackendContext(NodeStore store) {
      super(store);
    }

    @Override
    public void applyUpdates() {
      assertNotNull(QueryExecution.current());
      super.applyUpdates();
    }
  }

  private Query query(Function function, boolean dynamic) {
    MainModule module = new MainModule();
    module.setExpr(dynamic ? new DynamicFunctionExpr(null, function, null) : new FunctionExpr(null, function));
    return new Query(module);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void extensionConsumersReceiveTheBackendContextDuringEagerAndLazyAccess(boolean dynamic) {
    BackendContext backend = new BackendContext(store);
    AtomicInteger calls = new AtomicInteger();
    Function function = new AbstractFunction(new QNm("probe"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
      @Override
      public Sequence execute(StaticContext sctx, QueryContext received, Sequence[] args) {
        assertSame(backend, received);
        assertEquals(123, ((BackendContext) received).marker);
        return new LazySequence() {
          @Override
          public Iter iterate() {
            assertSame(backend, received);
            return new BaseIter() {
              private int position;

              @Override
              public Item next() {
                assertSame(backend, received);
                assertEquals(123, ((BackendContext) received).marker);
                calls.incrementAndGet();
                return ++position <= 3 ? new Int32(position) : null;
              }

              @Override
              public void close() {
                assertSame(backend, received);
              }
            };
          }
        };
      }
    };
    Query query = query(function, dynamic);
    Sequence result = query.execute(backend);
    assertEquals(0, calls.get());
    assertEquals(Int32.ONE, result.get(Int32.ONE));
    assertEquals(new Int32(3), result.size());
    try (Iter iter = result.iterate()) {
      iter.skip(Int32.ONE);
      Iter.Split split = iter.split(1, 2);
      try (Iter head = split.head) {
        assertEquals(new Int32(2), head.next());
        assertEquals(new Int32(3), head.next());
        assertNull(head.next());
      }
      if (split.tail != null) {
        split.tail.close();
      }
    }
    query.evaluate(backend);
    assertTrue(calls.get() > 3);
  }

  @Test
  void forkedTasksAndLazyResultsShareOnlyTheirOwnExecutionDefault() {
    BackendContext backend = new BackendContext(store);
    Query declarationQuery = new Query("declare context item as node() := <n/>; $$");
    DefaultCtxItem declaration = (DefaultCtxItem) declarationQuery.getModule().getVariables().resolve(Bits.FS_DOT);
    Function function = new AbstractFunction(new QNm("parallel-probe"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
      @Override
      public Sequence execute(StaticContext sctx, QueryContext received, Sequence[] args) {
        assertSame(backend, received);
        return new LazySequence() {
          @Override
          public Iter iterate() {
            return new BaseIter() {
              private int position;

              @Override
              public Item next() {
                if (++position > 2) {
                  return null;
                }
                Item[] item = new Item[1];
                Task task = new Task() {
                  @Override
                  protected void doCompute() {
                    assertSame(backend, received);
                    item[0] = declaration.evaluateToItem(received, new TupleImpl());
                  }
                };
                FJControl.submit(task).join();
                assertNull(task.getError());
                return item[0];
              }

              @Override
              public void close() {
              }
            };
          }
        };
      }
    };
    Query query = query(function, false);
    try (Iter first = query.execute(backend).iterate(); Iter second = query.execute(backend).iterate()) {
      Item firstNode = first.next();
      Item secondNode = second.next();
      assertNotSame(firstNode, secondNode);
      assertSame(firstNode, first.next());
      assertSame(secondNode, second.next());
    }
  }
}
