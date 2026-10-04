package io.brackit.query.expr;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.brackit.query.BrackitQueryContext;
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
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.module.Functions;
import io.brackit.query.module.Namespaces;
import io.brackit.query.sequence.ItemSequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DefaultContextExecutionTest extends XQueryBaseTest {
  private QueryContext context(Item item) {
    QueryContext context = new BrackitQueryContext(store);
    context.bind(new QNm("f"),
                 new Functions().resolve(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "string-length"), 0));
    context.setContextItem(item);
    return context;
  }

  private Query query(String declaration, String body) {
    return new Query(declaration + " declare variable $f external; " + body);
  }

  private void checkLength(Query query, QueryContext context, int expected) {
    ResultChecker.dCheck(new Int32(expected), query.execute(context));
  }

  private void checkError(Query query, QueryContext context, QNm code) {
    QueryException error = assertThrows(QueryException.class, () -> query.evaluate(context));
    assertEquals(code, error.getCode());
  }

  @ParameterizedTest
  @ValueSource(strings = { "$f()", "string-length()" })
  void oneCompiledQueryReadsDifferentContextsAndRejectsLaterAbsence(String call) {
    Query query = query("", call);
    checkLength(query, context(new Str("a")), 1);
    checkLength(query, context(new Str("abcd")), 4);
    checkError(query, context(null), ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED);
    checkLength(query, context(new Str("xy")), 2);
  }

  @ParameterizedTest
  @ValueSource(strings = { "$f()", "string-length()" })
  void oneCompiledQueryRevalidatesTheSameCallerContext(String call) {
    Query query = query("declare context item as xs:string external;", call);
    QueryContext context = context(new Str("a"));
    checkLength(query, context, 1);
    context.setContextItem(new Str("abcd"));
    checkLength(query, context, 4);
    context.setContextItem(null);
    checkError(query, context, ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED);
    context.setContextItem(Int32.ONE);
    checkError(query, context, ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
    context.setContextItem(new Str("xy"));
    checkLength(query, context, 2);
  }

  @ParameterizedTest
  @ValueSource(strings = { "$f()", "string-length()" })
  void declaredDefaultOverridesHostItemsOnEveryExecution(String call) {
    Query query = query("declare context item as xs:string := 'abcd';", call);
    QueryContext context = context(new Str("a"));
    checkLength(query, context, 4);
    context.setContextItem(null);
    checkLength(query, context, 4);
    checkLength(query, context(Int32.ONE), 4);
  }

  @ParameterizedTest
  @ValueSource(strings = { "$f()", "string-length()" })
  void externalFallbackIsChosenAndValidatedForEveryExecution(String call) {
    Query query = query("declare context item as xs:string external := 'xy';", call);
    QueryContext context = context(null);
    checkLength(query, context, 2);
    context.setContextItem(new Str("a"));
    checkLength(query, context, 1);
    checkLength(query, context(new Str("abcd")), 4);
    context.setContextItem(Int32.ONE);
    checkError(query, context, ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
    context.setContextItem(null);
    checkLength(query, context, 2);
  }

  @ParameterizedTest
  @ValueSource(strings = { "$f()", "string-length()" })
  void invalidExternalFallbackCannotBeHiddenByAnEarlierHostItem(String call) {
    Query query = query("declare context item as xs:string external := 1;", call);
    QueryContext context = context(new Str("a"));
    checkLength(query, context, 1);
    context.setContextItem(null);
    checkError(query, context, ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
    checkLength(query, context(new Str("abcd")), 4);
    checkError(query, context(null), ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
  }

  @ParameterizedTest
  @ValueSource(strings = { "$f()", "string-length()" })
  void invalidDeclaredDefaultRemainsATypeErrorAcrossExecutions(String call) {
    Query query = query("declare context item as xs:string := 1;", call);
    checkError(query, context(new Str("a")), ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
    checkError(query, context(null), ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
  }

  @Test
  void defaultNodeIsInitializedOncePerExecutionIncludingOverlappingLazyResults() {
    Query query = new Query("declare context item as node() := <n/>; (position(), $$, $$)");
    try (Iter first = query.execute(ctx).iterate()) {
      assertEquals(Int32.ONE, first.next());
      Item firstNode = first.next();
      assertNotNull(firstNode);
      try (Iter second = query.execute(ctx).iterate()) {
        assertEquals(Int32.ONE, second.next());
        Item secondNode = second.next();
        assertNotNull(secondNode);
        assertNotSame(firstNode, secondNode);
        assertSame(firstNode, first.next());
        assertSame(secondNode, second.next());
        assertNull(first.next());
        assertNull(second.next());
      }
    }
  }

  @Test
  void initializedExternalContextSurvivesAnotherExecutionOnTheSameCaller() {
    Query query = query("declare context item as xs:string external;", "($f(), string-length())");
    QueryContext context = context(new Str("a"));
    try (Iter first = query.execute(context).iterate()) {
      assertEquals(Int32.ONE, first.next());
      context.setContextItem(new Str("abcd"));
      try (Iter second = query.execute(context).iterate()) {
        assertEquals(new Int32(4), second.next());
        assertEquals(Int32.ONE, first.next());
        assertEquals(new Int32(4), second.next());
        assertNull(first.next());
        assertNull(second.next());
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "$f()", "string-length()" })
  void localFocusShadowsModuleContextOnReusedQueries(String call) {
    String body = "count(('a','abcd')[?" + call + " eq 4])" + " + count(<r><n>a</n><n>abcd</n></r>/n[?" + call
        + " eq 4])";
    Query query = query("declare context item as xs:string external;", body);
    QueryContext context = context(new Str("host"));
    checkLength(query, context, 2);
    context.setContextItem(null);
    checkLength(query, context, 2);
    checkLength(query, context(Int32.ONE), 2);
  }

  @Test
  void ordinaryFunctionsDoNotInitializeAnUnusedDefault() {
    Query query = query("declare context item as node() := error();", "$f() and true()");
    QueryContext context = context(null);
    context.bind(new QNm("f"), new Functions().resolve(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "true"), 0));
    ResultChecker.dCheck(Bool.TRUE, query.execute(context));
    context.setContextItem(new Str("host"));
    ResultChecker.dCheck(Bool.TRUE, query.execute(context));
  }

  @Test
  void evaluationAndSerializationStartFreshExecutions() {
    Query query = query("", "($f(), string-length())");
    QueryContext context = context(new Str("a"));
    ResultChecker.dCheck(new ItemSequence(Int32.ONE, Int32.ONE), query.evaluate(context));
    context.setContextItem(new Str("abcd"));
    ResultChecker.dCheck(new ItemSequence(new Int32(4), new Int32(4)), query.evaluate(context));
    try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PrintStream stream = new PrintStream(out)) {
      context.setContextItem(new Str("xy"));
      query.serialize(context, stream);
      assertEquals("2 2", out.toString(StandardCharsets.UTF_8));
      out.reset();
      context.setContextItem(new Str("abc"));
      query.serialize(context, stream);
      assertEquals("3 3", out.toString(StandardCharsets.UTF_8));
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<Item> concurrentRead(Query query, QueryContext context, CyclicBarrier firstRead) throws Exception {
    try (Iter iter = query.execute(context).iterate()) {
      Item first = iter.next();
      firstRead.await(10, TimeUnit.SECONDS);
      Item second = iter.next();
      assertNull(iter.next());
      return List.of(first, second);
    }
  }

  @Test
  void concurrentCompiledQueryExecutionsUseTheirOwnExternalContext() throws Exception {
    Query query = query("", "($f(), string-length())");
    QueryContext firstContext = context(new Str("a"));
    QueryContext secondContext = context(new Str("abcd"));
    CyclicBarrier firstRead = new CyclicBarrier(2);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> concurrentRead(query, firstContext, firstRead));
      var second = executor.submit(() -> concurrentRead(query, secondContext, firstRead));
      assertEquals(List.of(Int32.ONE, Int32.ONE), first.get(15, TimeUnit.SECONDS));
      assertEquals(List.of(new Int32(4), new Int32(4)), second.get(15, TimeUnit.SECONDS));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void concurrentDefaultsKeepIdentityWithinEachExecution(boolean sameCaller) throws Exception {
    Query query = new Query("declare context item as node() := <n/>; ($$, $$)");
    QueryContext firstContext = context(null);
    QueryContext secondContext = sameCaller ? firstContext : context(null);
    CyclicBarrier firstRead = new CyclicBarrier(2);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> concurrentRead(query, firstContext, firstRead));
      var second = executor.submit(() -> concurrentRead(query, secondContext, firstRead));
      List<Item> firstNodes = first.get(15, TimeUnit.SECONDS);
      List<Item> secondNodes = second.get(15, TimeUnit.SECONDS);
      assertSame(firstNodes.get(0), firstNodes.get(1));
      assertSame(secondNodes.get(0), secondNodes.get(1));
      assertNotSame(firstNodes.get(0), secondNodes.get(0));
    }
  }
}
