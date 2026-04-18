/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer;

import io.brackit.query.atomic.QNm;
import io.brackit.query.compiler.AST;
import io.brackit.query.compiler.XQ;
import io.brackit.query.compiler.optimizer.walker.topdown.VectorizedGroupByDetection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that {@link VectorizedGroupByDetection} correctly annotates pipeline
 * AST nodes with vectorized scan properties.
 * <p>
 * Each test builds a manual pipeline AST (PipeExpr → Start → ForBind → ... → End)
 * and verifies the annotation keys set on the PipeExpr node.
 */
public class VectorizedGroupByDetectionTest {

  private VectorizedGroupByDetection stage;

  @BeforeEach
  void setUp() {
    stage = new VectorizedGroupByDetection();
  }

  // ==================== Helper methods to build AST fragments ====================

  /**
   * Builds a DerefExpr: $var.fieldName
   * Structure: DerefExpr( VariableRef($var), QNm(fieldName) )
   */
  private AST deref(String varName, String fieldName) {
    AST deref = new AST(XQ.DerefExpr);
    deref.addChild(new AST(XQ.VariableRef, new QNm(varName)));
    deref.addChild(new AST(XQ.DerefExpr, new QNm(fieldName)));
    return deref;
  }

  /** Integer literal node. */
  private AST intLit(long value) {
    return new AST(XQ.Int, String.valueOf(value));
  }

  /** String literal node. */
  private AST strLit(String value) {
    return new AST(XQ.Str, value);
  }

  /** Comparison node: left OP right (direct form, as in manual ASTs). */
  private AST comparison(int compType, AST left, AST right) {
    AST cmp = new AST(compType);
    cmp.addChild(left);
    cmp.addChild(right);
    return cmp;
  }

  /** Comparison wrapped in ComparisonExpr — matches real parser output. */
  private AST comparisonExpr(int compType, AST left, AST right) {
    AST cmpExpr = new AST(XQ.ComparisonExpr);
    cmpExpr.addChild(new AST(compType)); // operator token (no children)
    cmpExpr.addChild(left);
    cmpExpr.addChild(right);
    return cmpExpr;
  }

  /** End node with a dummy return expression. */
  private AST end() {
    AST end = new AST(XQ.End);
    end.addChild(new AST(XQ.VariableRef, new QNm("result")));
    return end;
  }

  /** ForBind with a typed variable binding and a dummy source, chaining to nextOp. */
  private AST forBind(String varName, AST nextOp) {
    AST fb = new AST(XQ.ForBind);
    AST tvb = new AST(XQ.TypedVariableBinding);
    tvb.addChild(new AST(XQ.Variable, new QNm(varName)));
    fb.addChild(tvb);
    fb.addChild(new AST(XQ.VariableRef, new QNm("source"))); // source expression
    fb.addChild(nextOp); // chain
    return fb;
  }

  /** Selection node: where predicate, chaining to nextOp. */
  private AST selection(AST predicate, AST nextOp) {
    AST sel = new AST(XQ.Selection);
    sel.addChild(predicate);
    sel.addChild(nextOp);
    return sel;
  }

  /** LetBind: let $varName := derefExpr, chaining to nextOp. */
  private AST letBind(String varName, AST valueExpr, AST nextOp) {
    AST lb = new AST(XQ.LetBind);
    AST tvb = new AST(XQ.TypedVariableBinding);
    tvb.addChild(new AST(XQ.Variable, new QNm(varName)));
    lb.addChild(tvb);
    lb.addChild(valueExpr);
    lb.addChild(nextOp); // chain
    return lb;
  }

  /** GroupBy node chaining to nextOp. */
  private AST groupBy(AST nextOp) {
    AST gb = new AST(XQ.GroupBy);
    AST spec = new AST(XQ.GroupBySpec);
    spec.addChild(new AST(XQ.VariableRef, new QNm("g")));
    gb.addChild(spec);
    AST dft = new AST(XQ.DftAggregateSpec);
    gb.addChild(dft);
    gb.addChild(nextOp);
    return gb;
  }

  /** OrderBy node with a single spec, chaining to nextOp. */
  private AST orderBy(AST fieldExpr, String direction, AST nextOp) {
    AST ob = new AST(XQ.OrderBy);
    AST spec = new AST(XQ.OrderBySpec);
    spec.addChild(fieldExpr);
    if (direction != null) {
      AST kind = new AST(XQ.OrderByKind, direction);
      spec.addChild(kind);
    }
    ob.addChild(spec);
    ob.addChild(nextOp);
    return ob;
  }

  /** Wraps a chain in PipeExpr → Start → chain. */
  private AST pipeExpr(AST chain) {
    AST pipe = new AST(XQ.PipeExpr);
    AST start = new AST(XQ.Start);
    start.addChild(chain);
    pipe.addChild(start);
    return pipe;
  }

  /** Wraps the full AST so the stage can walk it (needs a root). */
  private AST root(AST pipeExpr) {
    AST root = new AST(XQ.Start); // arbitrary wrapper
    root.addChild(pipeExpr);
    return root;
  }

  // ==================== Tests ====================

  @Test
  void groupByPattern() {
    // for $u in SRC let $c := $u.city group by $c return ...
    AST pipe = pipeExpr(forBind("u", letBind("c", deref("u", "city"), groupBy(end()))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertEquals("city", pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
  }

  @Test
  void filteredCountPattern() {
    // for $u in SRC where $u.age > 30 return ...
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)), end())));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertEquals("age", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("gt", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals(30L, pipe.getProperty(VectorizedScanAnnotation.FILTER_VALUE));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
  }

  @Test
  void filteredGroupByPattern() {
    // for $u in SRC where $u.age > 30 let $c := $u.city group by $c return ...
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)),
                                          letBind("c", deref("u", "city"), groupBy(end())))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertEquals("city", pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
    assertEquals("age", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("gt", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals(30L, pipe.getProperty(VectorizedScanAnnotation.FILTER_VALUE));
    // Should NOT be flagged as count since group-by is present
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
  }

  @Test
  void multiKeyGroupByPattern() {
    // for $u let $c := $u.city, $s := $u.state group by $c, $s return ...
    AST pipe = pipeExpr(forBind("u",
                                letBind("c", deref("u", "city"), letBind("s", deref("u", "state"), groupBy(end())))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertEquals("city", pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
    String[] extra = (String[]) pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELDS_EXTRA);
    assertNotNull(extra);
    assertEquals(1, extra.length);
    assertEquals("state", extra[0]);
  }

  @Test
  void orderByAscending() {
    // for $u in SRC order by $u.name ascending return ...
    AST pipe = pipeExpr(forBind("u", orderBy(deref("u", "name"), "ascending", end())));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
    assertEquals("name", pipe.getProperty(VectorizedScanAnnotation.ORDER_FIELD));
    assertEquals("ascending", pipe.getProperty(VectorizedScanAnnotation.ORDER_DIRECTION));
  }

  @Test
  void orderByDescending() {
    // for $u in SRC order by $u.score descending return ...
    AST pipe = pipeExpr(forBind("u", orderBy(deref("u", "score"), "descending", end())));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
    assertEquals("score", pipe.getProperty(VectorizedScanAnnotation.ORDER_FIELD));
    assertEquals("descending", pipe.getProperty(VectorizedScanAnnotation.ORDER_DIRECTION));
  }

  @Test
  void stringEqualityFilter() {
    // for $u where $u.city eq "NYC" return ...
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.ValueCompEQ, deref("u", "city"), strLit("NYC")), end())));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertEquals("city", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("eq", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals("NYC", pipe.getProperty(VectorizedScanAnnotation.FILTER_STRING_VALUE));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.FILTER_VALUE));
  }

  @Test
  void compoundAndPredicate() {
    // for $u where $u.age > 30 and $u.city eq "NYC" return ...
    AST andExpr = new AST(XQ.AndExpr);
    andExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)));
    andExpr.addChild(comparison(XQ.ValueCompEQ, deref("u", "city"), strLit("NYC")));

    AST pipe = pipeExpr(forBind("u", selection(andExpr, end())));

    stage.rewrite(null, root(pipe));

    // First filter: age > 30
    assertEquals("age", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("gt", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals(30L, pipe.getProperty(VectorizedScanAnnotation.FILTER_VALUE));

    // Second filter: city eq "NYC"
    assertEquals("city", pipe.getProperty(VectorizedScanAnnotation.FILTER2_FIELD));
    assertEquals("eq", pipe.getProperty(VectorizedScanAnnotation.FILTER2_OP));
    assertEquals("NYC", pipe.getProperty(VectorizedScanAnnotation.FILTER2_STRING_VALUE));
  }

  @Test
  void reversedComparison() {
    // for $u where 30 < $u.age return ...  (equivalent to $u.age > 30)
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.GeneralCompLT, intLit(30), deref("u", "age")), end())));

    stage.rewrite(null, root(pipe));

    assertEquals("age", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("gt", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP)); // reversed from lt
    assertEquals(30L, pipe.getProperty(VectorizedScanAnnotation.FILTER_VALUE));
  }

  @Test
  void valueComparisonOperators() {
    // for $u where $u.score ge 100 return ...
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.ValueCompGE, deref("u", "score"), intLit(100)), end())));

    stage.rewrite(null, root(pipe));

    assertEquals("score", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("ge", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals(100L, pipe.getProperty(VectorizedScanAnnotation.FILTER_VALUE));
  }

  @Test
  void lessThanComparison() {
    // for $u where $u.age lt 18 return ...
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.ValueCompLT, deref("u", "age"), intLit(18)), end())));

    stage.rewrite(null, root(pipe));

    assertEquals("age", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("lt", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals(18L, pipe.getProperty(VectorizedScanAnnotation.FILTER_VALUE));
  }

  @Test
  void lessOrEqualComparison() {
    // for $u where $u.age <= 65 return ...
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.GeneralCompLE, deref("u", "age"), intLit(65)), end())));

    stage.rewrite(null, root(pipe));

    assertEquals("age", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("le", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals(65L, pipe.getProperty(VectorizedScanAnnotation.FILTER_VALUE));
  }

  @Test
  void plainForBindNoAnnotation() {
    // for $u in SRC return ...  — no filter, group-by, or order-by
    AST pipe = pipeExpr(forBind("u", end()));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
  }

  @Test
  void emptyPipeExprNoAnnotation() {
    // PipeExpr with no children
    AST pipe = new AST(XQ.PipeExpr);
    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
  }

  @Test
  void filterWithOrderByDetectsBoth() {
    // for $u where $u.age > 21 order by $u.name ascending return ...
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(21)),
                                          orderBy(deref("u", "name"), "ascending", end()))));

    stage.rewrite(null, root(pipe));

    // Filter detected
    assertEquals("age", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("gt", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals(21L, pipe.getProperty(VectorizedScanAnnotation.FILTER_VALUE));

    // Order-by detected
    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
    assertEquals("name", pipe.getProperty(VectorizedScanAnnotation.ORDER_FIELD));
    assertEquals("ascending", pipe.getProperty(VectorizedScanAnnotation.ORDER_DIRECTION));

    // Should NOT be flagged as count since order-by is present
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
  }

  @Test
  void generalCompEQ() {
    // for $u where $u.status = "active" return ...
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompEQ, deref("u", "status"), strLit("active")),
                                          end())));

    stage.rewrite(null, root(pipe));

    assertEquals("status", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("eq", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals("active", pipe.getProperty(VectorizedScanAnnotation.FILTER_STRING_VALUE));
  }

  @Test
  void groupByWithNoLetBindNoFields() {
    // GroupBy without preceding LetBind — groupFields will be empty, so no VECTORIZED_GROUPBY
    AST pipe = pipeExpr(forBind("u", groupBy(end())));

    stage.rewrite(null, root(pipe));

    // hasGroupBy=true but groupFields is empty → no annotation
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
  }

  @Test
  void nestedPipeExprBothAnnotated() {
    // Two PipeExpr nodes in a tree — both should be independently annotated
    AST innerPipe = pipeExpr(forBind("v", letBind("d", deref("v", "dept"), groupBy(end()))));

    AST outerPipe = pipeExpr(forBind("u",
                                     selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(25)), end())));

    AST root = new AST(XQ.Start);
    root.addChild(outerPipe);
    root.addChild(innerPipe);

    stage.rewrite(null, root);

    // Outer: filtered count
    assertEquals(Boolean.TRUE, outerPipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertEquals("age", outerPipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));

    // Inner: group-by
    assertEquals(Boolean.TRUE, innerPipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertEquals("dept", innerPipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
  }

  @Test
  void reversedGEComparison() {
    // for $u where 100 >= $u.score return ...  (equivalent to $u.score <= 100)
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.GeneralCompGE, intLit(100), deref("u", "score")), end())));

    stage.rewrite(null, root(pipe));

    assertEquals("score", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("le", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP)); // ge reversed to le
    assertEquals(100L, pipe.getProperty(VectorizedScanAnnotation.FILTER_VALUE));
  }

  @Test
  void threeKeyGroupBy() {
    // for $u let $a := $u.city, $b := $u.state, $c := $u.country group by ... return ...
    AST pipe = pipeExpr(forBind("u",
                                letBind("a",
                                        deref("u", "city"),
                                        letBind("b",
                                                deref("u", "state"),
                                                letBind("c", deref("u", "country"), groupBy(end()))))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertEquals("city", pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
    String[] extra = (String[]) pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELDS_EXTRA);
    assertNotNull(extra);
    assertEquals(2, extra.length);
    assertEquals("state", extra[0]);
    assertEquals("country", extra[1]);
  }

  @Test
  void orderByWithDefaultDirection() {
    // for $u order by $u.name return ...  (no explicit direction → "ascending")
    AST pipe = pipeExpr(forBind("u", orderBy(deref("u", "name"), null, end())));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
    assertEquals("name", pipe.getProperty(VectorizedScanAnnotation.ORDER_FIELD));
    assertEquals("ascending", pipe.getProperty(VectorizedScanAnnotation.ORDER_DIRECTION));
  }

  @Test
  void filterAndGroupByAndOrderByAllDetected() {
    // for $u where $u.age > 18 let $c := $u.city group by $c order by $u.name return ...
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(18)),
                                          letBind("c",
                                                  deref("u", "city"),
                                                  groupBy(orderBy(deref("u", "name"), "ascending", end()))))));

    stage.rewrite(null, root(pipe));

    // All three patterns detected
    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertEquals("city", pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
    assertEquals("age", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("gt", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
    assertEquals("name", pipe.getProperty(VectorizedScanAnnotation.ORDER_FIELD));
    // Not a count pattern (group-by is present)
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
  }

  // ==================== ComparisonExpr wrapper tests (real parser output) ====================

  @Test
  void comparisonExprWrappedFilter() {
    // Real parser output: Selection → ComparisonExpr(GeneralCompGT, DerefExpr, Int)
    AST pipe = pipeExpr(forBind("u",
                                selection(comparisonExpr(XQ.GeneralCompGT, deref("u", "age"), intLit(30)), end())));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertEquals("age", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("gt", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals(30L, pipe.getProperty(VectorizedScanAnnotation.FILTER_VALUE));
  }

  @Test
  void comparisonExprWrappedStringEq() {
    // Real parser output: ComparisonExpr(ValueCompEQ, DerefExpr, Str)
    AST pipe = pipeExpr(forBind("u",
                                selection(comparisonExpr(XQ.ValueCompEQ, deref("u", "city"), strLit("NYC")), end())));

    stage.rewrite(null, root(pipe));

    assertEquals("city", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("eq", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals("NYC", pipe.getProperty(VectorizedScanAnnotation.FILTER_STRING_VALUE));
  }

  @Test
  void comparisonExprWithInt32Value() {
    // Int node with Int32 object value (as created by real parser)
    AST intNode = new AST(XQ.Int, new io.brackit.query.atomic.Int32(40));

    AST pipe = pipeExpr(forBind("u", selection(comparisonExpr(XQ.GeneralCompGT, deref("u", "age"), intNode), end())));

    stage.rewrite(null, root(pipe));

    assertEquals("age", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("gt", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals(40L, pipe.getProperty(VectorizedScanAnnotation.FILTER_VALUE));
  }

  @Test
  void comparisonExprFilteredGroupBy() {
    // Real parser: where $u.age > 30 (as ComparisonExpr) + group by
    AST pipe = pipeExpr(forBind("u",
                                selection(comparisonExpr(XQ.GeneralCompGT, deref("u", "age"), intLit(30)),
                                          letBind("c", deref("u", "city"), groupBy(end())))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertEquals("city", pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
    assertEquals("age", pipe.getProperty(VectorizedScanAnnotation.FILTER_FIELD));
    assertEquals("gt", pipe.getProperty(VectorizedScanAnnotation.FILTER_OP));
    assertEquals(30L, pipe.getProperty(VectorizedScanAnnotation.FILTER_VALUE));
  }

  /** End node whose return expression is a VariableRef to the given name. */
  private AST endReturningVar(String varName) {
    AST end = new AST(XQ.End);
    end.addChild(new AST(XQ.VariableRef, new QNm(varName)));
    return end;
  }

  @Test
  void countDistinctGroupByLetReturnsKey() {
    // for $u let $d := $u.dept group by $d return $d  — the exact bench shape
    AST pipe = pipeExpr(forBind("u", letBind("d", deref("u", "dept"), groupBy(endReturningVar("d")))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE,
                 pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT_DISTINCT),
                 "count-distinct annotation must be set for `return $d` where $d is the group key");
    assertEquals("dept", pipe.getProperty(VectorizedScanAnnotation.COUNT_DISTINCT_FIELD));
  }

  @Test
  void countDistinctReturnMismatchNotAnnotated() {
    // for $u let $d := $u.dept group by $d return "x"  — return is not the group key
    AST end = new AST(XQ.End);
    end.addChild(strLit("x"));
    AST pipe = pipeExpr(forBind("u", letBind("d", deref("u", "dept"), groupBy(end))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT_DISTINCT),
               "count-distinct must not fire when return is not the group-key VarRef");
  }

  // ==================== PREDICATE_TREE annotation tests ====================
  // These cover the generic Umbra/DuckDB-style predicate-tree output produced
  // alongside the legacy shape-specific FILTER_* annotations.

  @Test
  void predicateTreeSingleNumCmp() {
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(40)), end())));
    stage.rewrite(null, root(pipe));

    PredicateNode p = (PredicateNode) pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE);
    assertNotNull(p);
    assertTrue(p instanceof PredicateNode.NumCmp);
    PredicateNode.NumCmp nc = (PredicateNode.NumCmp) p;
    assertEquals("age", nc.field());
    assertEquals("gt", nc.op());
    assertEquals(40L, nc.value());
  }

  @Test
  void predicateTreeStringEquality() {
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.ValueCompEQ, deref("u", "city"), strLit("NYC")), end())));
    stage.rewrite(null, root(pipe));

    PredicateNode p = (PredicateNode) pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE);
    assertTrue(p instanceof PredicateNode.StrEq);
    PredicateNode.StrEq s = (PredicateNode.StrEq) p;
    assertEquals("city", s.field());
    assertEquals("NYC", s.value());
  }

  @Test
  void predicateTreeAndBoolConjunct() {
    // where $u.age > 40 and $u.active — NumCmp AND BoolRef
    AST andExpr = new AST(XQ.AndExpr);
    andExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(40)));
    andExpr.addChild(deref("u", "active"));
    AST pipe = pipeExpr(forBind("u", selection(andExpr, end())));
    stage.rewrite(null, root(pipe));

    PredicateNode p = (PredicateNode) pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE);
    assertTrue(p instanceof PredicateNode.And);
    PredicateNode.And a = (PredicateNode.And) p;
    assertEquals(2, a.children().size());
    assertTrue(a.children().get(0) instanceof PredicateNode.NumCmp);
    assertTrue(a.children().get(1) instanceof PredicateNode.BoolRef);
    assertEquals("active", ((PredicateNode.BoolRef) a.children().get(1)).field());
  }

  @Test
  void predicateTreeRangeAndBool() {
    // where $u.age > 30 and $u.age < 50 and $u.active
    AST innerAnd = new AST(XQ.AndExpr);
    innerAnd.addChild(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)));
    innerAnd.addChild(comparison(XQ.GeneralCompLT, deref("u", "age"), intLit(50)));
    AST outerAnd = new AST(XQ.AndExpr);
    outerAnd.addChild(innerAnd);
    outerAnd.addChild(deref("u", "active"));
    AST pipe = pipeExpr(forBind("u", selection(outerAnd, end())));
    stage.rewrite(null, root(pipe));

    PredicateNode p = (PredicateNode) pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE);
    assertNotNull(p);
    java.util.Set<String> fields = new java.util.HashSet<>();
    p.collectFields(fields);
    assertEquals(2, fields.size());
    assertTrue(fields.contains("age"));
    assertTrue(fields.contains("active"));
  }

  @Test
  void predicateTreeNoSelectionNotAnnotated() {
    // Pure group-by pattern with no WHERE clause → no PREDICATE_TREE.
    AST pipe = pipeExpr(forBind("u", letBind("c", deref("u", "city"), groupBy(end()))));
    stage.rewrite(null, root(pipe));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE));
  }

  @Test
  void predicateTreeUnrepresentableIsNotAnnotated() {
    // A predicate that the walker can't represent — the annotation must be
    // absent (the `predicateRepresentable` guard fails-closed).
    // Shape: where some-function($u.age) — unknown type, not a comparison.
    AST funcCall = new AST(XQ.FunctionCall, new QNm("some-fn"));
    funcCall.addChild(deref("u", "age"));
    AST pipe = pipeExpr(forBind("u", selection(funcCall, end())));
    stage.rewrite(null, root(pipe));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE));
  }
}
