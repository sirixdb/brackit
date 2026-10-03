package io.brackit.query.function;

import io.brackit.query.ErrorCode;
import io.brackit.query.Query;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.module.Functions;
import io.brackit.query.module.Namespaces;
import io.brackit.query.sequence.ItemSequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DynamicFunctionContextTest extends XQueryBaseTest {
  @ParameterizedTest
  @ValueSource(strings = { "true", "false" })
  void zeroArgumentBuiltinsDoNotReceiveFilterFocus(String name) {
    ctx.bind(new QNm("f"), new Functions().resolve(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, name), 0));
    String declaration = "declare variable $f external; ";
    boolean keep = name.equals("true");
    ResultChecker.dCheck(keep ? new ItemSequence(Int32.ZERO, Int32.ONE, new Int32(2)) : null,
                         new Query(declaration + "(0,1,2)[?$f()]").execute(ctx));
    ResultChecker.dCheck(keep ? Int32.ZERO : null, new Query(declaration + "0[?$f()]").execute(ctx));
    ResultChecker.dCheck(new Int32(keep ? 2 : 0),
                         new Query(declaration + "count(<r><a/><b/><c/></r>/c/preceding-sibling::*[?$f()])").execute(ctx));
  }

  @Test
  void zeroArgumentCollectionUsesDefaultCollectionInsidePredicates() {
    ctx.setDefaultNodeCollection(storeDocument("default", "<default/>"));
    ctx.bind(new QNm("f"), new Functions().resolve(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "collection"), 0));
    String declaration = "declare variable $f external; ";
    ResultChecker.dCheck(new ItemSequence(new Str("missing"), new Str("other")),
                         new Query(declaration + "('missing','other')[?exists($f())]").execute(ctx));
    ResultChecker.dCheck(new Str("missing"), new Query(declaration + "'missing'[?exists($f())]").execute(ctx));
    ResultChecker.dCheck(new Int32(2),
                         new Query(declaration + "count(<r><a/><b/><c/></r>/c/preceding-sibling::*[?exists($f())])").execute(ctx));
  }

  @Test
  void explicitBuiltinArgumentsRemainSeparateFromFocus() {
    ctx.bind(new QNm("f"), new Functions().resolve(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "boolean"), 1));
    ResultChecker.dCheck(null,
                         new Query("declare variable $f external; (0,1,2)[?$$ and $f(false())]").execute(ctx));
  }

  private void bindStringLength() {
    ctx.bind(new QNm("f"),
             new Functions().resolve(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "string-length"), 0));
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void declaredContextOverridesConflictingOrAbsentHostContext(boolean hostPresent) {
    bindStringLength();
    ctx.setContextItem(hostPresent ? new Str("a") : null);
    String declaration = "declare context item as xs:string := 'abcd'; declare variable $f external; ";
    ResultChecker.dCheck(Bool.TRUE, new Query(declaration + "$f() eq 4 and string-length() eq 4").execute(ctx));
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void externalContextUsesHostOrItsDeclaredFallback(boolean hostPresent) {
    bindStringLength();
    ctx.setContextItem(hostPresent ? new Str("a") : null);
    String declaration = "declare context item as xs:string external := 'abcd'; declare variable $f external; ";
    int expected = hostPresent ? 1 : 4;
    ResultChecker.dCheck(Bool.TRUE,
                         new Query(declaration + "$f() eq " + expected + " and string-length() eq " + expected).execute(ctx));
  }

  @ParameterizedTest
  @ValueSource(strings = { "declare context item as xs:integer := 'abcd';",
      "declare context item as node() := 'abcd';", "declare context item as node() external;" })
  void declaredAndExternalContextTypesAreValidated(String declaration) {
    bindStringLength();
    ctx.setContextItem(new Str("a"));
    for (String call : List.of("$f()", "string-length()")) {
      QueryException error = assertThrows(QueryException.class,
                                         () -> new Query(declaration + " declare variable $f external; " + call).evaluate(ctx));
      assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, error.getCode());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "declare context item as xs:string external := 1;",
      "declare context item as node() external := 'abcd';" })
  void externalFallbackTypesAreValidatedWithoutAHostItem(String declaration) {
    bindStringLength();
    ctx.setContextItem(null);
    for (String call : List.of("$f()", "string-length()")) {
      QueryException error = assertThrows(QueryException.class,
                                         () -> new Query(declaration + " declare variable $f external; " + call).evaluate(ctx));
      assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, error.getCode());
    }
  }

  @Test
  void ordinaryHostContextRemainsAvailable() {
    bindStringLength();
    ctx.setContextItem(new Str("abc"));
    ResultChecker.dCheck(Bool.TRUE,
                         new Query("declare variable $f external; $f() eq 3 and string-length() eq 3").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE,
                         new Query("declare context item as xs:string external; declare variable $f external; $f() eq 3 and string-length() eq 3").execute(ctx));
  }

  @Test
  void contextIndependentZeroArgumentCallsDoNotRequireAContext() {
    ctx.bind(new QNm("f"),
             new Functions().resolve(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "true"), 0));
    ctx.setContextItem(null);
    ResultChecker.dCheck(Bool.TRUE, new Query("declare variable $f external; $f()").execute(ctx));
  }

  @ParameterizedTest
  @ValueSource(strings = { "", "declare context item as xs:string external;" })
  void absentContextRetainsItsQueryError(String declaration) {
    bindStringLength();
    ctx.setContextItem(null);
    for (String call : List.of("$f()", "string-length()")) {
      QueryException error = assertThrows(QueryException.class,
                                         () -> new Query(declaration + " declare variable $f external; " + call).evaluate(ctx));
      assertEquals(ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED, error.getCode());
    }
  }

  @Test
  void filterFocusShadowsTheDeclaredModuleContext() {
    bindStringLength();
    ctx.setContextItem(new Str("host"));
    String declaration = "declare context item as xs:string := 'xyz'; declare variable $f external; ";
    ResultChecker.dCheck(new Str("abcd"), new Query(declaration + "('a','abcd')[?$f() eq 4]").execute(ctx));
    ResultChecker.dCheck(new Str("abcd"), new Query(declaration + "('a','abcd')[?string-length() eq 4]").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE,
                         new Query(declaration + "count(('a','abcd')[?$f() eq 4]) eq 1 and $f() eq 3 and string-length() eq 3").execute(ctx));
  }

  @Test
  void pathPredicateFocusIsResolvedWithoutAHostContext() {
    bindStringLength();
    ctx.setContextItem(null);
    String declaration = "declare variable $f external; ";
    ResultChecker.dCheck(Bool.TRUE,
                         new Query(declaration + "count(<r><n>a</n><n>abcd</n></r>/n[?$f() eq 4]) eq 1").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE,
                         new Query(declaration + "count(<r><n>a</n><n>abcd</n></r>/n[?string-length() eq 4]) eq 1").execute(ctx));
  }

  @Test
  void pathStepFocusIsResolvedWithoutAHostContext() {
    bindStringLength();
    ctx.setContextItem(null);
    String declaration = "declare variable $f external; ";
    ItemSequence expected = new ItemSequence(Int32.ONE, new Int32(4));
    ResultChecker.dCheck(expected, new Query(declaration + "<r><n>a</n><n>abcd</n></r>/n/($f())").execute(ctx));
    ResultChecker.dCheck(expected, new Query(declaration + "<r><n>a</n><n>abcd</n></r>/n/(string-length())").execute(ctx));
  }
}
