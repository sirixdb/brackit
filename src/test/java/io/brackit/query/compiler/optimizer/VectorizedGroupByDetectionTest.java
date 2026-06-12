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

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link VectorizedGroupByDetection} correctly annotates pipeline
 * AST nodes with vectorized scan properties.
 * <p>
 * Each test builds a manual pipeline AST (PipeExpr → Start → ForBind → ... → End)
 * and verifies the annotation keys set on the PipeExpr node.
 * <p>
 * Filter predicates are verified via the generic {@link PredicateNode} tree
 * stored under {@link VectorizedScanAnnotation#PREDICATE_TREE}; the legacy
 * shape-specific {@code FILTER_*} / {@code FILTER2_*} / {@code FILTER_BOOL_FIELD}
 * annotation constants were removed as part of the move to a single Umbra-style
 * generic predicate-tree SPI.
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

  /** End node wrapping an arbitrary return expression. */
  private AST endReturning(AST returnExpr) {
    AST end = new AST(XQ.End);
    end.addChild(returnExpr);
    return end;
  }

  /**
   * End node returning the canonical vectorized group-by record:
   * {@code {"<field>": $groupVar, "count": count($loopVar)}} — the exact shape
   * required for the legacy single-key group-by claim.
   */
  private AST endReturningGroupCount(String field, String groupVar, String loopVar) {
    return endReturningGroupCountMulti(new String[] { field }, new String[] { groupVar }, "count", loopVar);
  }

  /**
   * End node returning the generalized group-by record:
   * {@code {outNames[0]: $groupVars[0], ..., countName: count($loopVar)}}.
   */
  private AST endReturningGroupCountMulti(String[] outNames, String[] groupVars, String countName, String loopVar) {
    AST obj = new AST(XQ.ObjectConstructor);
    for (int i = 0; i < outNames.length; i++) {
      AST keyField = new AST(XQ.KeyValueField);
      keyField.addChild(strLit(outNames[i]));
      keyField.addChild(new AST(XQ.VariableRef, new QNm(groupVars[i])));
      obj.addChild(keyField);
    }
    AST countField = new AST(XQ.KeyValueField);
    countField.addChild(strLit(countName));
    AST countCall = new AST(XQ.FunctionCall, new QNm("count"));
    countCall.addChild(new AST(XQ.VariableRef, new QNm(loopVar)));
    countField.addChild(countCall);
    obj.addChild(countField);
    return endReturning(obj);
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

  /** GroupBy node with a single grouping spec on {@code keyVar}, chaining to nextOp. */
  private AST groupBy(String keyVar, AST nextOp) {
    return groupByKeys(new String[] { keyVar }, nextOp);
  }

  /** GroupBy node with one grouping spec per key variable, chaining to nextOp. */
  private AST groupByKeys(String[] keyVars, AST nextOp) {
    AST gb = new AST(XQ.GroupBy);
    for (String keyVar : keyVars) {
      AST spec = new AST(XQ.GroupBySpec);
      spec.addChild(new AST(XQ.VariableRef, new QNm(keyVar)));
      gb.addChild(spec);
    }
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

  /** Fetch and assert the pipe carries a PREDICATE_TREE. */
  private PredicateNode predicateTree(AST pipe) {
    PredicateNode p = (PredicateNode) pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE);
    assertNotNull(p, "expected PREDICATE_TREE annotation on pipe");
    return p;
  }

  /** Assert the predicate is a NumCmp with the given field/op/value. */
  private void assertNumCmp(PredicateNode p, String field, String op, long value) {
    assertInstanceOf(PredicateNode.NumCmp.class, p, "expected NumCmp predicate");
    PredicateNode.NumCmp nc = (PredicateNode.NumCmp) p;
    assertEquals(field, nc.field());
    assertEquals(op, nc.op());
    assertEquals(value, nc.value());
  }

  /** Assert the predicate is a StrEq with the given field/value. */
  private void assertStrEq(PredicateNode p, String field, String value) {
    assertInstanceOf(PredicateNode.StrEq.class, p, "expected StrEq predicate");
    PredicateNode.StrEq s = (PredicateNode.StrEq) p;
    assertEquals(field, s.field());
    assertEquals(value, s.value());
  }

  // ==================== Tests ====================

  @Test
  void groupByPattern() {
    // for $u in SRC let $c := $u.city group by $c return {"city": $c, "count": count($u)}
    AST pipe = pipeExpr(forBind("u",
                                letBind("c",
                                        deref("u", "city"),
                                        groupBy("c", endReturningGroupCount("city", "c", "u")))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertEquals("city", pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
  }

  @Test
  void filteredCountPattern() {
    // for $u in SRC where $u.age > 30 return $u
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)),
                                          endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertNumCmp(predicateTree(pipe), "age", "gt", 30L);
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
  }

  @Test
  void filteredGroupByPattern() {
    // for $u in SRC where $u.age > 30 let $c := $u.city group by $c
    // return {"city": $c, "count": count($u)}
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)),
                                          letBind("c",
                                                  deref("u", "city"),
                                                  groupBy("c", endReturningGroupCount("city", "c", "u"))))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertEquals("city", pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
    assertNumCmp(predicateTree(pipe), "age", "gt", 30L);
    // Should NOT be flagged as count since group-by is present
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
  }

  @Test
  void multiKeyGroupByWithSingleKeyReturnFailsClosed() {
    // for $u let $c := $u.city, $s := $u.state group by $c, $s return {"city": $c, "count": count($u)}
    // The return covers only ONE of the two grouping keys — the emitted records could
    // not represent the two-key grouping. Neither claim may fire. (Historically this
    // shape returned the single-first-key grouping — WRONG RESULTS.)
    AST pipe = pipeExpr(forBind("u",
                                letBind("c",
                                        deref("u", "city"),
                                        letBind("s",
                                                deref("u", "state"),
                                                groupByKeys(new String[] { "c", "s" },
                                                            endReturningGroupCount("city", "c", "u"))))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY_MULTI));
  }

  @Test
  void twoKeyCanonicalReturnClaimsMulti() {
    // for $u let $d := $u.dept, $c := $u.city group by $d, $c
    // return {"d": $d, "c": $c, "n": count($u)}  — the exact bench groupBy2Keys shape
    AST pipe = pipeExpr(forBind("u",
                                letBind("d",
                                        deref("u", "dept"),
                                        letBind("c",
                                                deref("u", "city"),
                                                groupByKeys(new String[] { "d", "c" },
                                                            endReturningGroupCountMulti(new String[] { "d", "c" },
                                                                                        new String[] { "d", "c" },
                                                                                        "n",
                                                                                        "u"))))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY_MULTI));
    assertArrayEquals(new String[] { "dept", "city" },
                      (String[]) pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELDS));
    assertArrayEquals(new String[] { "d", "c" },
                      (String[]) pipe.getProperty(VectorizedScanAnnotation.GROUPBY_OUT_NAMES));
    assertEquals("n", pipe.getProperty(VectorizedScanAnnotation.GROUPBY_COUNT_NAME));
    // The legacy single-key claim must NOT fire for a two-key grouping.
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
  }

  @Test
  void twoKeyReturnOrderDefinesOutputOrder() {
    // return {"c": $c, "d": $d, ...} — RETURN-clause order wins, not let/spec order.
    AST pipe = pipeExpr(forBind("u",
                                letBind("d",
                                        deref("u", "dept"),
                                        letBind("c",
                                                deref("u", "city"),
                                                groupByKeys(new String[] { "d", "c" },
                                                            endReturningGroupCountMulti(new String[] { "c", "d" },
                                                                                        new String[] { "c", "d" },
                                                                                        "n",
                                                                                        "u"))))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY_MULTI));
    assertArrayEquals(new String[] { "city", "dept" },
                      (String[]) pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELDS));
    assertArrayEquals(new String[] { "c", "d" },
                      (String[]) pipe.getProperty(VectorizedScanAnnotation.GROUPBY_OUT_NAMES));
  }

  @Test
  void renamedSingleKeyClaimsMultiOnly() {
    // for $u let $d := $u.dept group by $d return {"d": $d, "n": count($u)} — output
    // names differ from the canonical {dept, count}, so only the generalized claim fires.
    AST pipe = pipeExpr(forBind("u",
                                letBind("d",
                                        deref("u", "dept"),
                                        groupBy("d",
                                                endReturningGroupCountMulti(new String[] { "d" },
                                                                            new String[] { "d" },
                                                                            "n",
                                                                            "u")))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY_MULTI));
    assertArrayEquals(new String[] { "dept" }, (String[]) pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELDS));
    assertArrayEquals(new String[] { "d" }, (String[]) pipe.getProperty(VectorizedScanAnnotation.GROUPBY_OUT_NAMES));
    assertEquals("n", pipe.getProperty(VectorizedScanAnnotation.GROUPBY_COUNT_NAME));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
  }

  @Test
  void multiKeyDuplicateOutNamesFailsClosed() {
    // return {"x": $d, "x": $c, "n": count($u)} — duplicate object keys change the shape.
    AST pipe = pipeExpr(forBind("u",
                                letBind("d",
                                        deref("u", "dept"),
                                        letBind("c",
                                                deref("u", "city"),
                                                groupByKeys(new String[] { "d", "c" },
                                                            endReturningGroupCountMulti(new String[] { "x", "x" },
                                                                                        new String[] { "d", "c" },
                                                                                        "n",
                                                                                        "u"))))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY_MULTI));
  }

  @Test
  void multiKeyRepeatedKeyVarFailsClosed() {
    // return {"a": $d, "b": $d, "n": count($u)} — $c never appears; $d twice.
    AST pipe = pipeExpr(forBind("u",
                                letBind("d",
                                        deref("u", "dept"),
                                        letBind("c",
                                                deref("u", "city"),
                                                groupByKeys(new String[] { "d", "c" },
                                                            endReturningGroupCountMulti(new String[] { "a", "b" },
                                                                                        new String[] { "d", "d" },
                                                                                        "n",
                                                                                        "u"))))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY_MULTI));
  }

  @Test
  void orderByAscending() {
    // for $u in SRC order by $u.name ascending return $u
    AST pipe = pipeExpr(forBind("u", orderBy(deref("u", "name"), "ascending", endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
    assertEquals("name", pipe.getProperty(VectorizedScanAnnotation.ORDER_FIELD));
    assertEquals("ascending", pipe.getProperty(VectorizedScanAnnotation.ORDER_DIRECTION));
  }

  @Test
  void orderByDescending() {
    // for $u in SRC order by $u.score descending return $u
    AST pipe = pipeExpr(forBind("u", orderBy(deref("u", "score"), "descending", endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
    assertEquals("score", pipe.getProperty(VectorizedScanAnnotation.ORDER_FIELD));
    assertEquals("descending", pipe.getProperty(VectorizedScanAnnotation.ORDER_DIRECTION));
  }

  @Test
  void stringEqualityFilter() {
    // for $u where $u.city eq "NYC" return $u
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.ValueCompEQ, deref("u", "city"), strLit("NYC")),
                                          endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertStrEq(predicateTree(pipe), "city", "NYC");
  }

  @Test
  void compoundAndPredicate() {
    // for $u where $u.age > 30 and $u.city eq "NYC" return ...
    AST andExpr = new AST(XQ.AndExpr);
    andExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)));
    andExpr.addChild(comparison(XQ.ValueCompEQ, deref("u", "city"), strLit("NYC")));

    AST pipe = pipeExpr(forBind("u", selection(andExpr, end())));

    stage.rewrite(null, root(pipe));

    // PREDICATE_TREE is an And(NumCmp age gt 30, StrEq city "NYC")
    PredicateNode p = predicateTree(pipe);
    assertInstanceOf(PredicateNode.And.class, p);
    PredicateNode.And a = (PredicateNode.And) p;
    assertEquals(2, a.children().size());
    assertNumCmp(a.children().get(0), "age", "gt", 30L);
    assertStrEq(a.children().get(1), "city", "NYC");
  }

  @Test
  void reversedComparison() {
    // for $u where 30 < $u.age return ...  (equivalent to $u.age > 30)
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.GeneralCompLT, intLit(30), deref("u", "age")), end())));

    stage.rewrite(null, root(pipe));

    // The walker normalizes: 30 < $u.age → $u.age gt 30
    assertNumCmp(predicateTree(pipe), "age", "gt", 30L);
  }

  @Test
  void valueComparisonOperators() {
    // for $u where $u.score ge 100 return ...
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.ValueCompGE, deref("u", "score"), intLit(100)), end())));

    stage.rewrite(null, root(pipe));

    assertNumCmp(predicateTree(pipe), "score", "ge", 100L);
  }

  @Test
  void lessThanComparison() {
    // for $u where $u.age lt 18 return ...
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.ValueCompLT, deref("u", "age"), intLit(18)), end())));

    stage.rewrite(null, root(pipe));

    assertNumCmp(predicateTree(pipe), "age", "lt", 18L);
  }

  @Test
  void lessOrEqualComparison() {
    // for $u where $u.age <= 65 return ...
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.GeneralCompLE, deref("u", "age"), intLit(65)), end())));

    stage.rewrite(null, root(pipe));

    assertNumCmp(predicateTree(pipe), "age", "le", 65L);
  }

  @Test
  void plainForBindNoAnnotation() {
    // for $u in SRC return ...  — no filter, group-by, or order-by
    AST pipe = pipeExpr(forBind("u", end()));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE));
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
    // for $u where $u.age > 21 order by $u.name ascending return $u
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(21)),
                                          orderBy(deref("u", "name"), "ascending", endReturningVar("u")))));

    stage.rewrite(null, root(pipe));

    // Filter detected
    assertNumCmp(predicateTree(pipe), "age", "gt", 21L);

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

    assertStrEq(predicateTree(pipe), "status", "active");
  }

  @Test
  void groupByWithNoLetBindNoFields() {
    // GroupBy without preceding LetBind — groupFields will be empty, so no VECTORIZED_GROUPBY
    AST pipe = pipeExpr(forBind("u", groupBy("g", end())));

    stage.rewrite(null, root(pipe));

    // hasGroupBy=true but groupFields is empty → no annotation
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
  }

  @Test
  void nestedPipeExprBothAnnotated() {
    // Two PipeExpr nodes in a tree — both should be independently annotated
    AST innerPipe = pipeExpr(forBind("v",
                                     letBind("d",
                                             deref("v", "dept"),
                                             groupBy("d", endReturningGroupCount("dept", "d", "v")))));

    AST outerPipe = pipeExpr(forBind("u",
                                     selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(25)),
                                               endReturningVar("u"))));

    AST root = new AST(XQ.Start);
    root.addChild(outerPipe);
    root.addChild(innerPipe);

    stage.rewrite(null, root);

    // Outer: filtered count
    assertEquals(Boolean.TRUE, outerPipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertNumCmp(predicateTree(outerPipe), "age", "gt", 25L);

    // Inner: group-by
    assertEquals(Boolean.TRUE, innerPipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertEquals("dept", innerPipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
  }

  @Test
  void reversedGEComparison() {
    // for $u where 100 >= $u.score return ...  (equivalent to $u.score <= 100)
    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.GeneralCompGE, intLit(100), deref("u", "score")), end())));

    stage.rewrite(null, root(pipe));

    assertNumCmp(predicateTree(pipe), "score", "le", 100L);
  }

  @Test
  void threeKeyGroupByWithSingleKeyReturnFailsClosed() {
    // for $u let $a := $u.city, $b := $u.state, $c := $u.country group by $a, $b, $c
    // return {"city": $a, "count": count($u)} — return covers 1 of 3 keys.
    AST pipe = pipeExpr(forBind("u",
                                letBind("a",
                                        deref("u", "city"),
                                        letBind("b",
                                                deref("u", "state"),
                                                letBind("c",
                                                        deref("u", "country"),
                                                        groupByKeys(new String[] { "a", "b", "c" },
                                                                    endReturningGroupCount("city", "a", "u")))))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY_MULTI));
  }

  @Test
  void threeKeyCanonicalReturnClaimsMulti() {
    // for $u let $a := $u.city, $b := $u.state, $c := $u.country group by $a, $b, $c
    // return {"city": $a, "state": $b, "country": $c, "count": count($u)}
    AST pipe = pipeExpr(forBind("u",
                                letBind("a",
                                        deref("u", "city"),
                                        letBind("b",
                                                deref("u", "state"),
                                                letBind("c",
                                                        deref("u", "country"),
                                                        groupByKeys(new String[] { "a", "b", "c" },
                                                                    endReturningGroupCountMulti(new String[] { "city",
                                                                        "state", "country" },
                                                                                                new String[] { "a", "b",
                                                                                                    "c" },
                                                                                                "count",
                                                                                                "u")))))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY_MULTI));
    assertArrayEquals(new String[] { "city", "state", "country" },
                      (String[]) pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELDS));
    assertEquals("count", pipe.getProperty(VectorizedScanAnnotation.GROUPBY_COUNT_NAME));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
  }

  @Test
  void orderByWithDefaultDirection() {
    // for $u order by $u.name return $u  (no explicit direction → "ascending")
    AST pipe = pipeExpr(forBind("u", orderBy(deref("u", "name"), null, endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
    assertEquals("name", pipe.getProperty(VectorizedScanAnnotation.ORDER_FIELD));
    assertEquals("ascending", pipe.getProperty(VectorizedScanAnnotation.ORDER_DIRECTION));
  }

  @Test
  void groupByFollowedByOrderByFailsClosed() {
    // for $u where $u.age > 18 let $c := $u.city group by $c order by $u.name
    // return {"city": $c, "count": count($u)}
    // The vectorized group-by executor emits groups in ITS order — claiming this
    // pipeline would silently drop the requested ordering. The sorted-scan claim
    // can't stand either (group-by changes the tuple shape). Only the predicate
    // tree survives as information.
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(18)),
                                          letBind("c",
                                                  deref("u", "city"),
                                                  groupBy("c",
                                                          orderBy(deref("u", "name"),
                                                                  "ascending",
                                                                  endReturningGroupCount("city", "c", "u")))))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertNumCmp(predicateTree(pipe), "age", "gt", 18L);
  }

  // ==================== ComparisonExpr wrapper tests (real parser output) ====================

  @Test
  void comparisonExprWrappedFilter() {
    // Real parser output: Selection → ComparisonExpr(GeneralCompGT, DerefExpr, Int)
    AST pipe = pipeExpr(forBind("u",
                                selection(comparisonExpr(XQ.GeneralCompGT, deref("u", "age"), intLit(30)),
                                          endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertNumCmp(predicateTree(pipe), "age", "gt", 30L);
  }

  @Test
  void comparisonExprWrappedStringEq() {
    // Real parser output: ComparisonExpr(ValueCompEQ, DerefExpr, Str)
    AST pipe = pipeExpr(forBind("u",
                                selection(comparisonExpr(XQ.ValueCompEQ, deref("u", "city"), strLit("NYC")), end())));

    stage.rewrite(null, root(pipe));

    assertStrEq(predicateTree(pipe), "city", "NYC");
  }

  @Test
  void comparisonExprWithInt32Value() {
    // Int node with Int32 object value (as created by real parser)
    AST intNode = new AST(XQ.Int, new io.brackit.query.atomic.Int32(40));

    AST pipe = pipeExpr(forBind("u", selection(comparisonExpr(XQ.GeneralCompGT, deref("u", "age"), intNode), end())));

    stage.rewrite(null, root(pipe));

    assertNumCmp(predicateTree(pipe), "age", "gt", 40L);
  }

  @Test
  void comparisonExprFilteredGroupBy() {
    // Real parser: where $u.age > 30 (as ComparisonExpr) + group by + canonical return
    AST pipe = pipeExpr(forBind("u",
                                selection(comparisonExpr(XQ.GeneralCompGT, deref("u", "age"), intLit(30)),
                                          letBind("c",
                                                  deref("u", "city"),
                                                  groupBy("c", endReturningGroupCount("city", "c", "u"))))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertEquals("city", pipe.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
    assertNumCmp(predicateTree(pipe), "age", "gt", 30L);
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
    AST pipe = pipeExpr(forBind("u", letBind("d", deref("u", "dept"), groupBy("d", endReturningVar("d")))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE,
                 pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT_DISTINCT),
                 "count-distinct annotation must be set for `return $d` where $d is the group key");
    assertEquals("dept", pipe.getProperty(VectorizedScanAnnotation.COUNT_DISTINCT_FIELD));
  }

  @Test
  void countDistinctReturnMismatchNotAnnotated() {
    // for $u let $d := $u.dept group by $d return "x"  — return is not the group key
    AST pipe = pipeExpr(forBind("u", letBind("d", deref("u", "dept"), groupBy("d", endReturning(strLit("x"))))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT_DISTINCT),
               "count-distinct must not fire when return is not the group-key VarRef");
  }

  // ==================== PREDICATE_TREE annotation tests ====================
  // These cover the generic Umbra/DuckDB-style predicate-tree output — the
  // sole filter representation after removal of the legacy shape-specific
  // FILTER_* annotations.

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
    Set<String> fields = new HashSet<>();
    p.collectFields(fields);
    assertEquals(2, fields.size());
    assertTrue(fields.contains("age"));
    assertTrue(fields.contains("active"));
  }

  @Test
  void predicateTreeNoSelectionNotAnnotated() {
    // Pure group-by pattern with no WHERE clause → no PREDICATE_TREE.
    AST pipe = pipeExpr(forBind("u", letBind("c", deref("u", "city"), groupBy("c", end()))));
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

  // ==================== Fail-closed negative tests ====================
  // Every claim swaps the WHOLE pipeline for a vectorized executor with a fixed
  // output shape, so any mismatch between query and executor must produce NO
  // annotation — falling back to the generic pipeline is always correct.

  @Test
  void groupBySpecVarMismatchFailsClosed() {
    // for $u let $c := $u.city group by $g — the spec groups by a DIFFERENT variable
    // than the let-bound key; grouping by city would be wrong results.
    AST pipe = pipeExpr(forBind("u",
                                letBind("c",
                                        deref("u", "city"),
                                        groupBy("g", endReturningGroupCount("city", "c", "u")))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT_DISTINCT));
  }

  @Test
  void groupByComputedKeyFailsClosed() {
    // for $u let $c := upper-case($u.city) group by $c — the key is COMPUTED; grouping
    // by the raw `city` values (what lenient extraction used to find) is wrong results.
    AST upper = new AST(XQ.FunctionCall, new QNm("upper-case"));
    upper.addChild(deref("u", "city"));
    AST pipe = pipeExpr(forBind("u", letBind("c", upper, groupBy("c", endReturningGroupCount("city", "c", "u")))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT_DISTINCT));
  }

  @Test
  void groupByWrongReturnFieldNameFailsClosed() {
    // Canonical SHAPE but the first key is "town", not the grouping field "city" —
    // the executor emits {"city": ...}, which is not what the query returns.
    AST pipe = pipeExpr(forBind("u",
                                letBind("c",
                                        deref("u", "city"),
                                        groupBy("c", endReturningGroupCount("town", "c", "u")))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
  }

  @Test
  void groupByCountOverWrongVarFailsClosed() {
    // return {"city": $c, "count": count($c)} — counts the KEY var, not the group tuples.
    AST pipe = pipeExpr(forBind("u",
                                letBind("c",
                                        deref("u", "city"),
                                        groupBy("c", endReturningGroupCount("city", "c", "c")))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
  }

  @Test
  void unrepresentableSelectionBlocksGroupBy() {
    // where some-fn($u.age) ... group by — the filter can't ride along as a
    // PREDICATE_TREE, so a group-by claim would scan UNFILTERED. No claim.
    AST funcCall = new AST(XQ.FunctionCall, new QNm("some-fn"));
    funcCall.addChild(deref("u", "age"));
    AST pipe = pipeExpr(forBind("u",
                                selection(funcCall,
                                          letBind("c",
                                                  deref("u", "city"),
                                                  groupBy("c", endReturningGroupCount("city", "c", "u"))))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE));
  }

  @Test
  void unrepresentableSelectionBlocksOrderBy() {
    // where some-fn($u.age) order by $u.name return $u — same unfiltered-scan hazard.
    AST funcCall = new AST(XQ.FunctionCall, new QNm("some-fn"));
    funcCall.addChild(deref("u", "age"));
    AST pipe = pipeExpr(forBind("u",
                                selection(funcCall, orderBy(deref("u", "name"), "ascending", endReturningVar("u")))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
  }

  @Test
  void countDistinctWithSelectionFailsClosed() {
    // count(for $u where $u.age > 30 let $d := $u.dept group by $d return $d) —
    // an HLL cardinality sketch is unfiltered, so ANY selection blocks the claim.
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)),
                                          letBind("d", deref("u", "dept"), groupBy("d", endReturningVar("d"))))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT_DISTINCT));
    // The filter itself is representable — only the count-distinct claim is blocked.
    assertNumCmp(predicateTree(pipe), "age", "gt", 30L);
  }

  @Test
  void orderByTwoSpecsFailsClosed() {
    // order by $u.name, $u.age return $u — the executor sorts by ONE key; claiming
    // the first spec would break ties differently than the query asks.
    AST ob = new AST(XQ.OrderBy);
    for (String f : new String[] { "name", "age" }) {
      AST spec = new AST(XQ.OrderBySpec);
      spec.addChild(deref("u", f));
      ob.addChild(spec);
    }
    ob.addChild(endReturningVar("u"));
    AST pipe = pipeExpr(forBind("u", ob));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
  }

  @Test
  void orderByComputedKeyFailsClosed() {
    // order by lower-case($u.name) return $u — sorting by the raw field is wrong.
    AST lower = new AST(XQ.FunctionCall, new QNm("lower-case"));
    lower.addChild(deref("u", "name"));
    AST pipe = pipeExpr(forBind("u", orderBy(lower, "ascending", endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
  }

  @Test
  void orderByReturningDerefFailsClosed() {
    // order by $u.name return $u.name — the sorted scan emits FULL RECORDS, not a field.
    AST pipe = pipeExpr(forBind("u", orderBy(deref("u", "name"), "ascending", endReturning(deref("u", "name")))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
  }

  @Test
  void countSequenceReturnFailsClosed() {
    // where $u.age > 30 return ($u, $u) — doubles the count; must not claim.
    AST seq = new AST(XQ.SequenceExpr);
    seq.addChild(new AST(XQ.VariableRef, new QNm("u")));
    seq.addChild(new AST(XQ.VariableRef, new QNm("u")));
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)),
                                          endReturning(seq))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertNumCmp(predicateTree(pipe), "age", "gt", 30L);
  }

  @Test
  void aggregateDirectFieldReturn() {
    // for $u where $u.age > 30 return $u.amount — aggregate candidate (the enclosing
    // sum/avg/... call fills AGGREGATE_FUNC); the count claim must NOT fire since the
    // return is a field, which yields zero items for records lacking it.
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)),
                                          endReturning(deref("u", "amount")))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_AGGREGATE));
    assertEquals("amount", pipe.getProperty(VectorizedScanAnnotation.AGGREGATE_FIELD));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
  }

  @Test
  void aggregateCompositeReturnFailsClosed() {
    // return $u.a + $u.b — lenient extraction would aggregate over field `a` alone.
    AST sum = new AST(XQ.ArithmeticExpr);
    sum.addChild(deref("u", "a"));
    sum.addChild(deref("u", "b"));
    AST pipe = pipeExpr(forBind("u", endReturning(sum)));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_AGGREGATE));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.AGGREGATE_FIELD));
  }

  @Test
  void unknownChainOperatorFailsClosed() {
    // for $u where $u.age > 30 count $pos return $u — the walker doesn't model the
    // count clause; a claim would silently drop it. NOTHING may be annotated.
    AST countClause = new AST(XQ.Count);
    AST tvb = new AST(XQ.TypedVariableBinding);
    tvb.addChild(new AST(XQ.Variable, new QNm("pos")));
    countClause.addChild(tvb);
    countClause.addChild(endReturningVar("u"));
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)), countClause)));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_AGGREGATE));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE));
  }
}
