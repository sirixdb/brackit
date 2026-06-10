package io.brackit.query;

import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Dbl;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Str;
import io.brackit.query.node.d2linked.D2NodeFactory;
import io.brackit.query.jdm.node.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the pass-3 compiler/runtime audit: string equality against untyped/anyURI,
 * order-by empty/NaN key handling, aggregate overflow/NaN, union/except with an empty operand,
 * XQUF apply order, element-construction content merging, D2 sibling-division exhaustion, the
 * preceding axis, reverse-axis positional predicates, 'allowing empty', deep-equal comment
 * skipping, general comparison with an empty operand, and ±0.0 grouping.
 */
public class PassThreeCorrectnessTest extends XQueryBaseTest {

  private String evalString(String query) {
    var out = new java.io.ByteArrayOutputStream();
    new Query(query).serialize(ctx, new java.io.PrintStream(out));
    return out.toString().trim();
  }

  // H4: Str.eq rejected untyped atomics — '"literal" = element' was always false.
  @Test
  public void literalOnLeftEqualsUntypedNode() {
    ResultChecker.dCheck(Bool.TRUE, new Query("let $d := <t>Potter</t> return \"Potter\" = $d").execute(ctx));
    ResultChecker.dCheck(Bool.FALSE, new Query("let $d := <t>Potter</t> return \"Potter\" != $d").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE, new Query("\"http://a\" = xs:anyURI(\"http://a\")").execute(ctx));
  }

  // H1: both order keys empty -> next orderspec must decide (and the comparator contract holds).
  @Test
  public void orderByConsultsSecondaryKeyWhenFirstKeysAreEmpty() {
    assertEquals("1 2 3", evalString("for $i in (3,1,2) order by (if ($i gt 5) then $i else ()), $i return $i"));
  }

  // M1: NaN orders next to the empty sequence ('empty least' default: () < NaN < values).
  @Test
  public void orderByPlacesNaNBeforeValuesUnderEmptyLeast() {
    assertEquals("NaN 1", evalString("for $x in (1, xs:double('NaN')) order by $x empty least return string($x)"));
    assertEquals("1 NaN", evalString("for $x in (xs:double('NaN'), 1) order by $x empty greatest return string($x)"));
  }

  // H2: Int64 sum must escalate past long range instead of wrapping.
  @Test
  public void sumEscalatesToArbitraryPrecisionInsteadOfWrapping() {
    assertEquals("13835058055282163710", evalString("string(sum((9223372036854775806, 4611686018427387904)))"));
  }

  // H3: fn:min/fn:max return NaN when the sequence contains NaN.
  @Test
  public void minMaxReturnNaN() {
    assertEquals("NaN", evalString("string(min((1, xs:double('NaN'))))"));
    assertEquals("NaN", evalString("string(max((1, xs:double('NaN'))))"));
    // larger sequence to engage the buffered/SIMD double path
    assertEquals("NaN", evalString("string(min((for $i in 1 to 64 return xs:double($i), xs:double('NaN'))))"));
  }

  // H7: an empty operand must not empty union/except results.
  @Test
  public void unionAndExceptWithEmptyOperand() {
    ResultChecker.dCheck(new Int32(2), new Query("count((<a/>, <b/>) except ())").execute(ctx));
    ResultChecker.dCheck(new Int32(2), new Query("count(() | (<a/>, <b/>))").execute(ctx));
    ResultChecker.dCheck(new Int32(0), new Query("count(() except (<a/>, <b/>))").execute(ctx));
  }

  // H8: XQUF phase order — inserts apply BEFORE replaceNode.
  @Test
  public void updateApplyOrderInsertBeforeReplace() {
    assertEquals("<r><b/><a/></r>",
                 evalString("copy $c := <r><t/></r> modify (insert node <a/> after $c/t, replace node $c/t with <b/>) return $c"));
  }

  // H9: an atomic after a text-node item joins WITHOUT a space — and the SOURCE stays untouched.
  @Test
  public void constructorMergesTextNodeAndAtomicWithoutMutatingSource() {
    assertEquals("<e>ab</e>", evalString("let $x := <x>a</x> let $t := $x/text() return element e { $t, \"b\" }"));
    // the source element keeps its original value
    assertEquals("a", evalString("let $x := <x>a</x> let $e := element e { $x/text(), \"b\" } return string($x)"));
    // adjacent ATOMICS still join with a single space
    assertEquals("<e>a b</e>", evalString("element e { \"a\", \"b\" }"));
  }

  // H10: repeated insert-before at the same boundary must not exhaust sibling divisions
  // (the third insert hit the [3]-vs-[3,3] prefix case and threw IllegalArgumentException).
  @Test
  public void repeatedInsertBeforeSameBoundary() {
    var factory = new D2NodeFactory();
    Node<?> doc = factory.build(new io.brackit.query.node.parser.DocumentParser("<r><a/><b/></r>"));
    Node<?> r = doc.getFirstChild();
    Node<?> target = r.getFirstChild().getNextSibling(); // <b/>
    for (int i = 0; i < 5; i++) {
      Node<?> fresh = factory.build(new io.brackit.query.node.parser.DocumentParser("<n/>")).getFirstChild();
      target = target.insertBefore(fresh);
    }
    int count = 0;
    try (var children = r.getChildren()) {
      while (children.next() != null) {
        count++;
      }
    }
    assertEquals(7, count);
  }

  // H11: the preceding axis excludes ancestors and must not drop legitimate nodes.
  @Test
  public void precedingAxisExcludesAncestors() {
    assertEquals("x b",
                 evalString("let $d := <r><x/><a><b/><c/></a></r> "
                     + "return string-join(for $n in $d//c/preceding::* return name($n), ' ')"));
  }

  // (H12 — positional predicates on reverse axes — is fixed in StepExpr, but the JSONiq
  // dialect this engine parses has no step-predicate syntax ('[..]' is array access), so the
  // path is not expressible from query level; no query test possible.)

  // H5: 'allowing empty' binds one empty tuple instead of crashing compilation.
  @Test
  public void allowingEmptyBindsEmptyTuple() {
    assertEquals("e", evalString("for $x allowing empty in () return \"e\""));
    // lazy-empty bind sequence (exercises the operator NPE path)
    assertEquals("done", evalString("for $x allowing empty in subsequence((1, 2), 5) return \"done\""));
    // non-empty behaves exactly like a plain for
    assertEquals("1 2", evalString("for $x allowing empty in (1, 2) return $x"));
  }

  // M2: trailing comments/PIs are ignored by fn:deep-equal.
  @Test
  public void deepEqualIgnoresTrailingComments() {
    ResultChecker.dCheck(Bool.TRUE, new Query("deep-equal(<x/>, <x><!--c--></x>)").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE, new Query("deep-equal(<x><!--c--><a/></x>, <x><a/><?pi d?></x>)").execute(ctx));
  }

  // M7: a general comparison with an empty operand is false(), not ().
  @Test
  public void generalComparisonWithEmptyOperandIsFalse() {
    ResultChecker.dCheck(new Int32(1), new Query("count(() = 1)").execute(ctx));
    ResultChecker.dCheck(Bool.FALSE, new Query("() = 1").execute(ctx));
  }

  // M5: -0.0 and +0.0 group together.
  @Test
  public void negativeZeroGroupsWithPositiveZero() {
    // one group containing both zeros: the non-grouping variable $x has 2 members
    assertEquals("2",
                 evalString("for $x in (xs:double('-0'), xs:double('0')) let $k := $x group by $k return count($x)"));
    // direct grouping: a single group (two groups would serialize as "1 1")
    assertEquals("1", evalString("for $x in (xs:double('-0'), xs:double('0')) group by $x return count($x)"));
  }

  // M9: unsupported order-by collations are rejected (XQST0076), not silently ignored.
  @Test
  public void orderByUnsupportedCollationRejected() {
    QueryException ex = assertThrows(QueryException.class,
                                     () -> new Query("for $x in (2,1) order by $x collation \"http://example.com/c\" return $x").execute(ctx));
    assertEquals(ErrorCode.ERR_UNKNOWN_COLLATION_IN_FLWOR_CLAUSE, ex.getCode());
  }
}
