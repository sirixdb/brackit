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
import io.brackit.query.function.json.JSONFun;
import io.brackit.query.module.Namespaces;
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

  /**
   * A predicate field must be DIRECTLY {@code $loopVar.field}. A nested deref names a value one
   * level down, but the annotation has no way to say so: dropping the prefix made
   * {@code $u.inner.age gt 5} compile to the same {@code NumCmp[field=age, op=gt, value=5]} as
   * {@code $u.age gt 5}, so an executor evaluating the annotation against the loop record's direct
   * children silently compared the OUTER age — a wrong answer with nothing left for the consumer to
   * notice. Declining costs the fast path; guessing cost the answer.
   */
  @Test
  void nestedDerefPredicateIsNotAnnotated() {
    // for $u where $u.inner.age > 5 return $u   —   DerefExpr(DerefExpr($u, inner), age)
    AST nested = new AST(XQ.DerefExpr);
    nested.addChild(deref("u", "inner"));
    nested.addChild(new AST(XQ.DerefExpr, new QNm("age")));

    AST pipe = pipeExpr(forBind("u", selection(comparison(XQ.GeneralCompGT, nested, intLit(5)), endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE),
               "a nested deref must not be annotated as a flat field");
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT),
               "with no representable predicate the pipeline must not be claimed at all");
  }

  /** Same rule for a reference to some OTHER variable in scope: not the loop record's field. */
  @Test
  void foreignVariableDerefPredicateIsNotAnnotated() {
    // for $u where $other.age > 5 return $u
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("other", "age"), intLit(5)),
                                          endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE),
               "a field of a different variable must not be annotated as the loop record's field");
  }

  /** The bare-deref (EBV) arm takes the same rule. */
  @Test
  void nestedBareDerefPredicateIsNotAnnotated() {
    // for $u where $u.inner.flag return $u
    AST nested = new AST(XQ.DerefExpr);
    nested.addChild(deref("u", "inner"));
    nested.addChild(new AST(XQ.DerefExpr, new QNm("flag")));

    AST pipe = pipeExpr(forBind("u", selection(nested, endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE),
               "a nested bare deref must not be annotated as a flat boolean field");
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

  // ==================== floating-point literals (FpCmp) ====================

  /** Decimal literal node — matches the parser ({@code new AST(XQ.Dec, new Dec(str))}). */
  private AST decLit(String value) {
    return new AST(XQ.Dec, new io.brackit.query.atomic.Dec(new java.math.BigDecimal(value)));
  }

  /** Double literal node — matches the parser ({@code new AST(XQ.Dbl, new Dbl(str))}). */
  private AST dblLit(String value) {
    return new AST(XQ.Dbl, new io.brackit.query.atomic.Dbl(Double.parseDouble(value)));
  }

  /** Assert the predicate is an FpCmp with the given field/op/value (bit-exact). */
  private void assertFpCmp(PredicateNode p, String field, String op, double value) {
    assertInstanceOf(PredicateNode.FpCmp.class, p, "expected FpCmp predicate");
    PredicateNode.FpCmp fc = (PredicateNode.FpCmp) p;
    assertEquals(field, fc.field());
    assertEquals(op, fc.op());
    assertEquals(Double.doubleToRawLongBits(value),
                 Double.doubleToRawLongBits(fc.value()),
                 "FpCmp value must be bit-exact: expected " + value + " got " + fc.value());
  }

  /** Assert the predicate is a DecCmp with the given field/op/value (exact decimal). */
  private void assertDecCmp(PredicateNode p, String field, String op, String value) {
    assertInstanceOf(PredicateNode.DecCmp.class, p, "expected DecCmp predicate");
    PredicateNode.DecCmp dc = (PredicateNode.DecCmp) p;
    assertEquals(field, dc.field());
    assertEquals(op, dc.op());
    assertEquals(0,
                 new java.math.BigDecimal(value).compareTo(dc.value()),
                 "DecCmp value must be exact: expected " + value + " got " + dc.value());
  }

  @Test
  void decimalLiteralBecomesExactDecCmp() {
    // for $u where $u.score gt 9.99 return $u — historically TRUNCATED to `score > 9`
    // (silently wrong results); now carried as an EXACT DecCmp (the interpreter
    // compares integer/decimal document values against an xs:decimal exactly).
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "score"), decLit("9.99")),
                                          endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertDecCmp(predicateTree(pipe), "score", "gt", "9.99");
  }

  @Test
  void doubleLiteralBecomesFpCmp() {
    // for $u where $u.score ge 2.5e0 return $u
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.ValueCompGE, deref("u", "score"), dblLit("2.5e0")),
                                          endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertFpCmp(predicateTree(pipe), "score", "ge", 2.5d);
  }

  @Test
  void integralDecimalBecomesExactNumCmp() {
    // for $u where $u.age le 10.0 return $u — 10.0 is an exact long; integer
    // comparison and the interpreter's decimal comparison agree for every value.
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompLE, deref("u", "age"), decLit("10.0")),
                                          endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertNumCmp(predicateTree(pipe), "age", "le", 10L);
  }

  @Test
  void reversedDecimalComparisonNormalizes() {
    // for $u where 9.99 lt $u.score return $u → score gt 9.99
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompLT, decLit("9.99"), deref("u", "score")),
                                          endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertDecCmp(predicateTree(pipe), "score", "gt", "9.99");
  }

  @Test
  void fractionalEqualityBecomesDecCmp() {
    // for $u where $u.score eq 2.5 return $u — eq over a fractional literal is
    // representable; integral columns simply never match it.
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.ValueCompEQ, deref("u", "score"), decLit("2.5")),
                                          endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertDecCmp(predicateTree(pipe), "score", "eq", "2.5");
  }

  @Test
  void hugeIntegerLiteralFailsClosed() {
    // where $u.x gt 18446744073709551616 (= 2^64, not long-representable):
    // the historical Number#longValue() truncation changed semantics silently.
    AST huge = new AST(XQ.Int, new io.brackit.query.atomic.Int(new java.math.BigDecimal("18446744073709551616")));
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "x"), huge), endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE));
  }

  @Test
  void nonFiniteDoubleLiteralFailsClosed() {
    // where $u.x lt xs:double NaN / INF — NaN comparison semantics (always false,
    // even for lt+gt combined) are not representable; fail closed.
    for (double bad : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
      AST lit = new AST(XQ.Dbl, new io.brackit.query.atomic.Dbl(bad));
      AST pipe = pipeExpr(forBind("u",
                                  selection(comparison(XQ.GeneralCompLT, deref("u", "x"), lit), endReturningVar("u"))));

      stage.rewrite(null, root(pipe));

      assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT), "literal " + bad);
      assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE), "literal " + bad);
    }
  }

  @Test
  void decimalCrossingIntegerBoundaryUnderDoubleRoundingStaysExact() {
    // 9.9999999999999999999 rounds to double 10.0 — a double image would flip
    // `x gt 9.9999999999999999999` for x=10. DecCmp carries the EXACT decimal,
    // so the claim is safe.
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT,
                                                     deref("u", "x"),
                                                     decLit("9.9999999999999999999")), endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertDecCmp(predicateTree(pipe), "x", "gt", "9.9999999999999999999");
  }

  @Test
  void fractionalDecimalBeyondTwoPow53StaysExact() {
    // |c| >= 2^53: longs near c are not double-exact — DecCmp keeps the exact
    // decimal so the comparison never degrades to a double image.
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "x"), decLit("9007199254740993.5")),
                                          endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertDecCmp(predicateTree(pipe), "x", "gt", "9007199254740993.5");
  }

  @Test
  void integralDecimalBeyondTwoPow53StaysExactNumCmp() {
    // 9007199254740993 (2^53 + 1) is integral and long-representable → exact
    // NumCmp; the long comparison is exact even though the double image isn't.
    AST pipe = pipeExpr(forBind("u",
                                selection(comparison(XQ.GeneralCompGT, deref("u", "x"), decLit("9007199254740993")),
                                          endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertNumCmp(predicateTree(pipe), "x", "gt", 9007199254740993L);
  }

  // ==================== OR / sparse-anchor fail-closed ====================

  @Test
  void orAcrossDifferentFieldsFailsClosed() {
    // where $u.a > 1 or $u.b > 1 — a record carrying only `b` satisfies the
    // disjunction but is invisible to an anchor-based scan on `a` (and vice
    // versa). No field is a sound anchor → no claim.
    AST orExpr = new AST(XQ.OrExpr);
    orExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "a"), intLit(1)));
    orExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "b"), intLit(1)));
    AST pipe = pipeExpr(forBind("u", selection(orExpr, endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE));
  }

  @Test
  void orOnSameFieldStillClaimed() {
    // where $u.a gt 900 or $u.a lt 5 — every disjunct references `a`; a record
    // missing `a` fails both → `a` is a sound anchor → claimable.
    AST orExpr = new AST(XQ.OrExpr);
    orExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "a"), intLit(900)));
    orExpr.addChild(comparison(XQ.GeneralCompLT, deref("u", "a"), intLit(5)));
    AST pipe = pipeExpr(forBind("u", selection(orExpr, endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    PredicateNode p = predicateTree(pipe);
    assertInstanceOf(PredicateNode.Or.class, p);
  }

  @Test
  void orAcrossFieldsWithSoundConjunctStillClaimed() {
    // where ($u.a > 1 or $u.b > 1) and $u.c > 0 — `c` is a sound anchor (records
    // missing `c` fail the conjunction), so the predicate stays representable.
    AST orExpr = new AST(XQ.OrExpr);
    orExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "a"), intLit(1)));
    orExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "b"), intLit(1)));
    AST andExpr = new AST(XQ.AndExpr);
    andExpr.addChild(orExpr);
    andExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "c"), intLit(0)));
    AST pipe = pipeExpr(forBind("u", selection(andExpr, endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
    PredicateNode p = predicateTree(pipe);
    assertInstanceOf(PredicateNode.And.class, p);
    assertEquals("c", p.findSoundAnchorField());
  }

  @Test
  void orAcrossDifferentFieldsBlocksGroupByClaim() {
    // The unsound predicate must also veto group-by / aggregate claims, not just
    // the filtered count.
    AST orExpr = new AST(XQ.OrExpr);
    orExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "a"), intLit(1)));
    orExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "b"), intLit(1)));
    AST pipe = pipeExpr(forBind("u",
                                selection(orExpr,
                                          letBind("c",
                                                  deref("u", "city"),
                                                  groupBy("c", endReturningGroupCount("city", "c", "u"))))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY_MULTI));
    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE));
  }

  // ==================== fn:not ====================

  /**
   * {@code fn:not} over a representable argument produces a {@link PredicateNode.Not}.
   *
   * <p>The class javadoc listed negation among the supported shapes and {@link PredicateNode.Not}
   * has always existed with every consumer handling it — but no branch in {@code extractPredicate}
   * ever built one, so any {@code where} touching {@code not(...)} returned null and dropped the
   * annotation whole. This is the arm that closes that gap.
   */
  @Test
  void negatedBooleanFieldProducesNotNode() {
    // for $u where not($u.active) and $u.age gt 30 return $u
    //
    // Conjoined with a numeric leaf on purpose: a BARE not($u.active) is correctly refused by the
    // sound-anchor guard (see negatedBooleanFieldAloneIsNotAnchorable), so asserting the Not node
    // on its own would assert against a tree the walker is right to throw away.
    final AST andExpr = new AST(XQ.AndExpr);
    andExpr.addChild(fnNot(Namespaces.FN_NSURI, deref("u", "active")));
    andExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)));

    final AST pipe = pipeExpr(forBind("u", selection(andExpr, endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    final PredicateNode p = predicateTree(pipe);
    assertInstanceOf(PredicateNode.And.class, p);
    final PredicateNode.And a = (PredicateNode.And) p;
    assertEquals(2, a.children().size());
    assertInstanceOf(PredicateNode.Not.class, a.children().get(0));
    final PredicateNode.Not n = (PredicateNode.Not) a.children().get(0);
    assertInstanceOf(PredicateNode.BoolRef.class, n.child());
    assertEquals("active", ((PredicateNode.BoolRef) n.child()).field());
    assertNumCmp(a.children().get(1), "age", "gt", 30L);
    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT));
  }

  /**
   * An unprefixed {@code not(...)} must be recognised too — and it is the form that matters.
   *
   * <p>{@code ExprAnalyzer#functionCall} expands an unprefixed call to the JSONiq default function
   * namespace and leaves it there; only an explicitly written {@code fn:not} ever carries
   * {@code FN_NSURI}. A namespace check written against {@code FN_NSURI} alone compiles, passes the
   * prefixed test above, and is dead code for every query a user actually writes.
   */
  @Test
  void unprefixedNotIsRecognisedInTheDefaultFunctionNamespace() {
    final AST andExpr = new AST(XQ.AndExpr);
    andExpr.addChild(fnNot(Namespaces.DEFAULT_FN_NSURI, deref("u", "active")));
    andExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)));

    final AST pipe = pipeExpr(forBind("u", selection(andExpr, endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    final PredicateNode.And a = (PredicateNode.And) predicateTree(pipe);
    assertInstanceOf(PredicateNode.Not.class, a.children().get(0));
  }

  /**
   * A bare negated boolean stays unclaimed: it holds for records that lack the field.
   *
   * <p>{@code not($u.active)} over a record with no {@code active} is {@code not(())}, whose EBV is
   * {@code not(false) = true}. An anchor-based scan iterates the {@code active} column's slots and
   * never visits such a record, so claiming this shape would undercount on sparse data — the exact
   * De Morgan case {@link PredicateNode#excludesRecordsMissingField} documents. The negation
   * support makes the walker able to SEE the shape; the guard is what decides it.
   */
  @Test
  void negatedBooleanFieldAloneIsNotAnchorable() {
    final AST pipe = pipeExpr(forBind("u",
                                      selection(fnNot(Namespaces.DEFAULT_FN_NSURI, deref("u", "active")),
                                                endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE),
               "a bare negation matches records missing the field and has no sound scan anchor");
    assertNull(pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT),
               "with no representable predicate the pipeline must not be claimed at all");
  }

  /** Same for a negated comparison: {@code not($u.age gt 30)} is true when {@code age} is absent. */
  @Test
  void negatedComparisonAloneIsNotAnchorable() {
    final AST pipe = pipeExpr(forBind("u",
                                      selection(fnNot(Namespaces.DEFAULT_FN_NSURI,
                                                      comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30))),
                                                endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE));
  }

  /**
   * A {@code not} from some other namespace is a different function and must not become a negation.
   *
   * <p>Matching on the local name alone would compile {@code jn:not($u.active)} — or any user
   * module's {@code not} — into a negation the executor then evaluates as one, inverting an answer
   * on behalf of a function whose body nobody looked at.
   */
  @Test
  void notFromAnotherNamespaceIsNotANegation() {
    final AST andExpr = new AST(XQ.AndExpr);
    andExpr.addChild(fnNot(JSONFun.JSON_NSURI, deref("u", "active")));
    andExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)));

    final AST pipe = pipeExpr(forBind("u", selection(andExpr, endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE),
               "only fn:not is a negation; an unknown function makes the whole predicate " + "unrepresentable");
  }

  /** Arity is part of the match: a two-argument {@code not} is not {@code fn:not}. */
  @Test
  void twoArgumentNotIsNotANegation() {
    final AST call = new AST(XQ.FunctionCall, new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "not"));
    call.addChild(deref("u", "active"));
    call.addChild(deref("u", "other"));

    final AST andExpr = new AST(XQ.AndExpr);
    andExpr.addChild(call);
    andExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)));

    final AST pipe = pipeExpr(forBind("u", selection(andExpr, endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE));
  }

  /** An unrepresentable argument still fails closed, rather than yielding a tree that lies. */
  @Test
  void notOverAnUnrepresentableArgumentIsRejected() {
    final AST nested = new AST(XQ.DerefExpr);
    nested.addChild(deref("u", "inner"));
    nested.addChild(new AST(XQ.DerefExpr, new QNm("flag")));

    final AST andExpr = new AST(XQ.AndExpr);
    andExpr.addChild(fnNot(Namespaces.DEFAULT_FN_NSURI, nested));
    andExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "age"), intLit(30)));

    final AST pipe = pipeExpr(forBind("u", selection(andExpr, endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE),
               "a negation over a nested deref must not be claimed as a flat-field negation");
  }

  /**
   * Grouping parens are their content: {@code (a and b) or c} arrives with a ParenthesizedExpr
   * around the AndExpr, and the walker must see through it. Before this arm existed, adding parens
   * to an otherwise representable predicate silently dropped the whole annotation.
   */
  @Test
  void parenthesizedBranchIsTransparent() {
    // where ($u.year ge 1940 and $u.year le 1950) or $u.year gt 2000 — all on ONE field, so the
    // disjunction is soundly anchored on it.
    final AST inner = new AST(XQ.AndExpr);
    inner.addChild(comparison(XQ.GeneralCompGE, deref("u", "year"), intLit(1940)));
    inner.addChild(comparison(XQ.GeneralCompLE, deref("u", "year"), intLit(1950)));
    final AST paren = new AST(XQ.ParenthesizedExpr);
    paren.addChild(inner);
    final AST orExpr = new AST(XQ.OrExpr);
    orExpr.addChild(paren);
    orExpr.addChild(comparison(XQ.GeneralCompGT, deref("u", "year"), intLit(2000)));

    final AST pipe = pipeExpr(forBind("u", selection(orExpr, endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    final PredicateNode p = predicateTree(pipe);
    assertInstanceOf(PredicateNode.Or.class, p);
    final PredicateNode.Or or = (PredicateNode.Or) p;
    assertEquals(2, or.children().size());
    assertInstanceOf(PredicateNode.And.class,
                     or.children().get(0),
                     "the parenthesized conjunction must survive as a conjunction");
  }

  /** A multi-child paren is a sequence constructor, not a predicate — still unrepresentable. */
  @Test
  void multiChildParenIsNotAPredicate() {
    final AST paren = new AST(XQ.ParenthesizedExpr);
    paren.addChild(comparison(XQ.GeneralCompGT, deref("u", "year"), intLit(2000)));
    paren.addChild(comparison(XQ.GeneralCompLT, deref("u", "year"), intLit(1950)));

    final AST pipe = pipeExpr(forBind("u", selection(paren, endReturningVar("u"))));

    stage.rewrite(null, root(pipe));

    assertNull(pipe.getProperty(VectorizedScanAnnotation.PREDICATE_TREE),
               "a sequence's EBV is not a predicate's; the annotation must be dropped");
  }

  /** {@code fn:not(...)} node in the given function namespace. */
  private AST fnNot(final String nsUri, final AST argument) {
    final AST call = new AST(XQ.FunctionCall, new QNm(nsUri, Namespaces.FN_PREFIX, "not"));
    call.addChild(argument);
    return call;
  }
}
