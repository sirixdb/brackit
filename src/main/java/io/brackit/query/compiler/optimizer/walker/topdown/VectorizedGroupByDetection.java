/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer.walker.topdown;

import io.brackit.query.atomic.QNm;
import io.brackit.query.compiler.AST;
import io.brackit.query.compiler.XQ;
import io.brackit.query.compiler.optimizer.PredicateNode;
import io.brackit.query.compiler.optimizer.Stage;
import io.brackit.query.compiler.optimizer.VectorizedScanAnnotation;
import io.brackit.query.module.StaticContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimizer stage that detects FLWOR patterns eligible for vectorized execution.
 * <p>
 * Detected patterns:
 * <ol>
 * <li>Group-by: {@code for $u in SRC let $c := $u.F group by $c return {"F": $c, "count": count($u)}}</li>
 * <li>Generalized group-by (multi-key and/or renamed outputs):
 * {@code for $u let $a := $u.F, $b := $u.G group by $a, $b return {"x": $a, "y": $b, "n": count($u)}}
 * — claimed via {@code VECTORIZED_GROUPBY_MULTI}, dispatch capability-gated</li>
 * <li>Filtered count: {@code for $u in SRC where $u.F > N return $u}</li>
 * <li>Filtered group-by: the group-by shapes above with a representable {@code where}</li>
 * <li>Sorted scan: {@code for $u in SRC order by $u.F descending return $u}</li>
 * <li>String equality filter: {@code for $u where $u.city eq "NYC" return $u}</li>
 * <li>Compound predicate (AND): {@code where $u.age > 30 and $u.city eq "NYC"}</li>
 * <li>Pure aggregate: {@code sum(for $u in SRC return $u.F)} (and avg/min/max/count)</li>
 * <li>Count-distinct: {@code count(for $u let $d := $u.F group by $d return $d)}</li>
 * </ol>
 * <p>
 * Every claim is FAIL-CLOSED: an annotation replaces the WHOLE pipeline with a
 * vectorized executor emitting a fixed result shape, so the stage only annotates
 * when the chain operators, the key/field sources (directly {@code $loopVar.field}),
 * and the return expression provably match what the executor emits. Anything else —
 * computed keys, unknown chain operators, unrepresentable predicates, non-canonical
 * returns — falls back to the generic (always correct) Volcano pipeline.
 */
public final class VectorizedGroupByDetection implements Stage {

  @Override
  public AST rewrite(StaticContext sctx, AST ast) {
    walkAndAnnotate(ast);
    return ast;
  }

  private void walkAndAnnotate(AST node) {
    if (node == null)
      return;
    if (node.getType() == XQ.PipeExpr) {
      tryAnnotate(node);
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      walkAndAnnotate(node.getChild(i));
    }
  }

  // ==================== Main pattern matcher ====================

  private void tryAnnotate(AST pipeExpr) {
    if (pipeExpr.getChildCount() < 1)
      return;
    AST chain = pipeExpr.getChild(0);
    if (chain.getType() != XQ.Start || chain.getChildCount() < 1) {
      return;
    }
    // The chain may have outer LetBinds (e.g. `let $doc := jn:doc(...) for $u in $doc[] ...`)
    // that wrap the actual ForBind. Walk down through LetBinds to find the ForBind.
    AST forBind = chain.getLastChild();
    while (forBind != null && forBind.getType() == XQ.LetBind) {
      forBind = forBind.getLastChild();
    }
    if (forBind == null || forBind.getType() != XQ.ForBind)
      return;

    final QNm loopVar = extractLoopVarName(forBind);

    List<String> groupFields = new ArrayList<>();
    // Declared variable names for LetBinds that feed group-by — used to verify
    // group-by / count-distinct patterns against the GroupBySpec and return expr.
    List<QNm> letBindVars = new ArrayList<>();
    // Variables named by GroupBySpec clauses. A claim must match them against the
    // let-bound key variable — otherwise the query groups by something else entirely.
    List<QNm> groupSpecVars = new ArrayList<>();
    boolean groupSpecsExtractable = true;
    String orderField = null;
    String orderDirection = null;
    boolean hasGroupBy = false;
    boolean hasOrderBy = false;
    boolean hasSelection = false;
    int orderByCount = 0;
    // Any claim replaces the WHOLE pipeline, so a chain operator this walker doesn't
    // model (count/window/join clauses, ...) would be silently dropped by the
    // executor. Unknown operator → no claims at all.
    boolean chainUnderstood = true;

    // Selection predicates accumulated across the chain. Multiple Selection
    // operators AND-conjoin by pipeline semantics. If ANY selection is not
    // representable as a PredicateNode we drop the annotation entirely, forcing
    // the generic Volcano pipeline — fail closed.
    List<PredicateNode> predicateConjuncts = new ArrayList<>();
    boolean predicateRepresentable = true;

    AST current = forBind.getLastChild();
    while (current != null && current.getType() != XQ.End) {
      switch (current.getType()) {
        case XQ.Selection -> {
          hasSelection = true;
          if (predicateRepresentable && current.getChildCount() >= 1) {
            PredicateNode pn = extractPredicate(current.getChild(0));
            if (pn == null) {
              predicateRepresentable = false;
            } else {
              predicateConjuncts.add(pn);
            }
          }
        }
        case XQ.LetBind -> {
          if (current.getChildCount() >= 2) {
            // Strict: the key source must be DIRECTLY `$loopVar.field`. Descending into a
            // wrapper would extract `city` out of `upper-case($u.city)` and the executor
            // would group by the RAW field values — silently wrong results.
            String field = directLoopVarDerefField(current.getChild(1), loopVar);
            if (field != null) {
              groupFields.add(field);
              letBindVars.add(extractLetBindVarName(current));
            }
          }
        }
        case XQ.GroupBy -> {
          hasGroupBy = true;
          groupSpecsExtractable &= extractGroupSpecVars(current, groupSpecVars);
        }
        case XQ.OrderBy -> {
          hasOrderBy = true;
          orderByCount++;
          var order = extractOrderBy(current, loopVar);
          if (order != null) {
            orderField = order.field;
            orderDirection = order.direction;
          }
        }
        default -> chainUnderstood = false;
      }
      current = current.getLastChild();
    }

    final AST returnExpr = current != null && current.getChildCount() > 0 ? current.getChild(0) : null;

    // If the return expression is DIRECTLY `$loopVar.field`, capture the field. An
    // enclosing sum()/avg()/min()/max() call uses this to vectorize (see
    // Compiler.functionCall interception). Direct only: recursing into the subtree
    // would turn `return $u.a + $u.b` into an aggregate over field `a` alone.
    final String returnField = directLoopVarDerefField(returnExpr, loopVar);

    // ---- Annotate based on detected pattern ----

    if (!chainUnderstood) {
      return;
    }

    // SOUND-ANCHOR GUARD: anchor-based executors iterate records via ONE
    // field's slots and never visit a record lacking that field. A predicate
    // that can hold on such a record (e.g. `$u.a > 1 or $u.b > 1` for a record
    // carrying only `b`, or any shape whose Not/Or combination is satisfiable
    // with some field absent) would silently lose matches on sparse data. Only
    // claim predicates for which SOME referenced field provably excludes
    // records missing it — the executor anchors there. No sound anchor → the
    // whole selection is unrepresentable → generic (always correct) pipeline.
    if (predicateRepresentable && !predicateConjuncts.isEmpty() && PredicateNode.and(predicateConjuncts)
                                                                                .findSoundAnchorField() == null) {
      predicateRepresentable = false;
    }

    final boolean hasPredicate = predicateRepresentable && !predicateConjuncts.isEmpty();
    if (hasPredicate) {
      pipeExpr.setProperty(VectorizedScanAnnotation.PREDICATE_TREE, PredicateNode.and(predicateConjuncts));
    }

    // Exactly one group-by key, let-bound from `$loopVar.field`, and the GroupBySpec
    // groups by precisely that variable. (Used by the count-distinct claim below.)
    final boolean singleGroupKey = hasGroupBy && groupFields.size() == 1 && letBindVars.getFirst() != null
        && groupSpecsExtractable && groupSpecVars.size() == 1 && letBindVars.getFirst()
                                                                            .equals(groupSpecVars.getFirst());

    // FAIL-CLOSED: a group-by claim replaces the pipeline with an executor emitting one
    // record per distinct key combination, shaped exactly like the query's return
    // clause. Claim ONLY when the return expression is the generalized canonical shape
    // {name1: $k1, ..., nameM: $kM, countName: count($loop)} whose key variables are
    // the let-bound direct-deref vars and match the GroupBySpec exactly, and no
    // order-by follows (the executor emits groups in ITS order, dropping the requested
    // one). A selection is fine only when representable — it is then carried via
    // PREDICATE_TREE and applied by the executor. Historical context: the original
    // detection claimed multi-key group-bys while the executor emitted the
    // single-first-key grouping with canonical field names — silently wrong results.
    if (hasGroupBy && groupSpecsExtractable && predicateRepresentable && !hasOrderBy) {
      final GroupReturnShape shape = matchCanonicalGroupReturn(returnExpr,
                                                               letBindVars,
                                                               groupFields,
                                                               groupSpecVars,
                                                               loopVar);
      if (shape != null) {
        pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY_MULTI, Boolean.TRUE);
        pipeExpr.setProperty(VectorizedScanAnnotation.GROUPBY_FIELDS, shape.sourceFields);
        pipeExpr.setProperty(VectorizedScanAnnotation.GROUPBY_OUT_NAMES, shape.outNames);
        pipeExpr.setProperty(VectorizedScanAnnotation.GROUPBY_COUNT_NAME, shape.countName);
        // Canonical single-key shape ({<field>: $key, "count": count($loop)}) ALSO gets
        // the legacy claim — executors keep their specialized single-key fast paths.
        if (shape.sourceFields.length == 1 && shape.outNames[0].equals(shape.sourceFields[0]) && "count".equals(
                                                                                                                shape.countName)) {
          pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY, Boolean.TRUE);
          pipeExpr.setProperty(VectorizedScanAnnotation.GROUPBY_FIELD, shape.sourceFields[0]);
        }
      }
    }

    // Filtered count: the pipeline counts MATCHING RECORDS, so the return expression
    // must be exactly the loop variable (one item per tuple, never empty):
    // `return ($u, $u)` doubles the count and `return $u.maybeAbsent` yields zero
    // items for records lacking the field — both silently wrong.
    if (hasPredicate && !hasGroupBy && !hasOrderBy && isSoleLoopVarRef(returnExpr, loopVar)) {
      pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_COUNT, Boolean.TRUE);
    }

    // Extract the loop variable's source path prefix (the expression supplying
    // $u). Executors combine it with per-predicate field names to scope queries
    // to a specific tree path — eliminates false matches against same-name
    // fields nested at other depths. Absent if the source isn't a simple path
    // expression; executors then fall back to a path-agnostic tree-walk.
    if (forBind.getChildCount() >= 2) {
      final String[] sourcePath = extractPathPrefix(forBind.getChild(1));
      if (sourcePath != null) {
        pipeExpr.setProperty(VectorizedScanAnnotation.SOURCE_PATH_PREFIX, sourcePath);
      }
    }

    // Sorted scan emits FULL RECORDS sorted by ONE direct `$loopVar.field` key — only
    // claim it for exactly one order-by with exactly one spec, a return expression that
    // is exactly the loop variable, no group-by (the executor would drop the grouping),
    // and a representable (or absent) selection.
    if (hasOrderBy && orderByCount == 1 && !hasGroupBy && predicateRepresentable && orderField != null
        && isSoleLoopVarRef(returnExpr, loopVar)) {
      pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY, Boolean.TRUE);
      pipeExpr.setProperty(VectorizedScanAnnotation.ORDER_FIELD, orderField);
      pipeExpr.setProperty(VectorizedScanAnnotation.ORDER_DIRECTION, orderDirection);
    }

    // Pure aggregate candidate: ForBind -> Return($u.field), no group-by, no order-by.
    // A predicate, if present, is carried via PREDICATE_TREE — the enclosing
    // sum/avg/min/max/count() call fills AGGREGATE_FUNC at compile time and the
    // dispatcher routes to executePredicateAggregate.
    if (!hasGroupBy && !hasOrderBy && predicateRepresentable && returnField != null) {
      pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_AGGREGATE, Boolean.TRUE);
      pipeExpr.setProperty(VectorizedScanAnnotation.AGGREGATE_FIELD, returnField);
    }

    // Count-distinct candidate: a single-key group-by whose return expression is
    // exactly the group key variable, e.g.:
    //   count(for $u in SRC let $d := $u.F group by $d return $d)
    // When wrapped in count(), the tuple count equals the number of distinct $d
    // values → answerable directly from a cardinality sketch (HLL) at query time.
    // Correctness guard: the return must be a VarRef whose QNm matches the first
    // group-by let-bind's declared variable; otherwise count(...) could return
    // a multiple of the distinct-count (e.g., return ($d, $d)). Also disallow
    // selections of ANY kind (representable or not) — an HLL is unfiltered.
    if (singleGroupKey && !hasOrderBy && !hasSelection) {
      final QNm returnVar = current != null ? extractSoleVariableRef(current) : null;
      if (returnVar != null && returnVar.equals(letBindVars.getFirst())) {
        pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_COUNT_DISTINCT, Boolean.TRUE);
        pipeExpr.setProperty(VectorizedScanAnnotation.COUNT_DISTINCT_FIELD, groupFields.getFirst());
      }
    }
  }

  /**
   * Field name iff {@code expr} is DIRECTLY {@code $loopVar.field} — a DerefExpr whose
   * base is a VariableRef of {@code loopVar}. No descent into subtrees: extracting a
   * field from inside a wrapping expression (function call, arithmetic, nested deref)
   * would make the executor compute over different values than the query asks for.
   */
  private String directLoopVarDerefField(final AST expr, final QNm loopVar) {
    if (expr == null || loopVar == null || expr.getType() != XQ.DerefExpr || expr.getChildCount() < 2)
      return null;
    final AST base = expr.getChild(0);
    if (base.getType() != XQ.VariableRef || !loopVar.equals(base.getValue() instanceof QNm qnm ? qnm : null))
      return null;
    return qnmLocalName(expr.getChild(expr.getChildCount() - 1).getValue());
  }

  /**
   * Collect the variables named by a GroupBy's {@link XQ#GroupBySpec} children into
   * {@code out}. Returns {@code false} if any spec is not a simple variable reference
   * — callers must then fail closed (no claim).
   */
  private boolean extractGroupSpecVars(final AST groupByNode, final List<QNm> out) {
    boolean extractable = true;
    for (int i = 0; i < groupByNode.getChildCount(); i++) {
      final AST child = groupByNode.getChild(i);
      if (child.getType() != XQ.GroupBySpec) {
        continue;
      }
      final AST ref = child.getChildCount() > 0 ? child.getChild(0) : null;
      if (ref != null && ref.getType() == XQ.VariableRef && ref.getValue() instanceof QNm qnm) {
        out.add(qnm);
      } else {
        extractable = false;
      }
    }
    return extractable;
  }

  /** The ForBind's declared loop-variable name — {@code null} if not extractable. */
  private QNm extractLoopVarName(final AST forBind) {
    if (forBind.getChildCount() < 1)
      return null;
    final AST binding = forBind.getChild(0);
    if (binding.getType() != XQ.TypedVariableBinding || binding.getChildCount() < 1)
      return null;
    final Object val = binding.getChild(0).getValue();
    return val instanceof QNm qnm ? qnm : null;
  }

  /** {@code true} iff {@code expr} is exactly a {@link XQ#VariableRef} of {@code loopVar}. */
  private boolean isSoleLoopVarRef(final AST expr, final QNm loopVar) {
    return loopVar != null && expr != null && expr.getType() == XQ.VariableRef && loopVar.equals(expr
                                                                                                     .getValue() instanceof QNm qnm
                                                                                                         ? qnm
                                                                                                         : null);
  }

  /** Extracted generalized group-by return shape — aligned source fields + output names. */
  private static final class GroupReturnShape {
    final String[] sourceFields;
    final String[] outNames;
    final String countName;

    GroupReturnShape(final String[] sourceFields, final String[] outNames, final String countName) {
      this.sourceFields = sourceFields;
      this.outNames = outNames;
      this.countName = countName;
    }
  }

  /**
   * Match the return expression against the generalized canonical group-by-count shape:
   * {@code {name1: $k1, ..., nameM: $kM, countName: count($loopVar)}} where
   * <ul>
   * <li>every {@code $k_i} is a let-bound direct {@code $loopVar.field} variable,</li>
   * <li>the {@code $k_i} are pairwise distinct and cover the GroupBySpec variables
   * EXACTLY (same count, same set — and the let-bound key vars are exactly the spec
   * vars, no extras: a dead let-bind is rare and falling back is always correct),</li>
   * <li>all output names ({@code name_i} and {@code countName}) are string literals
   * and pairwise distinct (duplicate object keys would change the record shape).</li>
   * </ul>
   * Returns the aligned shape in RETURN-clause order, or {@code null} if anything
   * deviates — the caller then leaves the pipeline to the generic (correct) path.
   */
  private GroupReturnShape matchCanonicalGroupReturn(final AST returnExpr, final List<QNm> letBindVars,
      final List<String> letBindFields, final List<QNm> groupSpecVars, final QNm loopVar) {
    final int keyCount = groupSpecVars.size();
    if (returnExpr == null || loopVar == null || keyCount < 1 || returnExpr.getType() != XQ.ObjectConstructor
        || returnExpr.getChildCount() != keyCount + 1) {
      return null;
    }
    // Strict bijection prerequisite: the let-bound direct-deref key vars ARE the spec vars.
    if (letBindVars.size() != keyCount || letBindVars.contains(null) || !new java.util.HashSet<>(letBindVars)
                                                                                                             .containsAll(groupSpecVars)
        || new java.util.HashSet<>(groupSpecVars).size() != keyCount) {
      return null;
    }
    final String[] sourceFields = new String[keyCount];
    final String[] outNames = new String[keyCount];
    final java.util.Set<String> seenNames = new java.util.HashSet<>();
    final java.util.Set<QNm> seenVars = new java.util.HashSet<>();
    for (int i = 0; i < keyCount; i++) {
      final AST kv = returnExpr.getChild(i);
      if (kv.getType() != XQ.KeyValueField || kv.getChildCount() != 2) {
        return null;
      }
      final String outName = stringLiteralValue(kv.getChild(0));
      final AST value = kv.getChild(1);
      if (outName == null || !seenNames.add(outName) || value.getType() != XQ.VariableRef || !(value
                                                                                                    .getValue() instanceof QNm keyVar)
          || !seenVars.add(keyVar)) {
        return null;
      }
      final int letIdx = letBindVars.indexOf(keyVar);
      if (letIdx < 0) {
        return null;
      }
      sourceFields[i] = letBindFields.get(letIdx);
      outNames[i] = outName;
    }
    // {..., countName: count($loopVar)}
    final AST countKv = returnExpr.getChild(keyCount);
    if (countKv.getType() != XQ.KeyValueField || countKv.getChildCount() != 2) {
      return null;
    }
    final String countName = stringLiteralValue(countKv.getChild(0));
    if (countName == null || !seenNames.add(countName)) {
      return null;
    }
    final AST countCall = countKv.getChild(1);
    if (countCall.getType() != XQ.FunctionCall || countCall.getChildCount() != 1 || !(countCall
                                                                                               .getValue() instanceof QNm fn)
        || !"count".equals(fn.getLocalName()) || !isSoleLoopVarRef(countCall.getChild(0), loopVar)) {
      return null;
    }
    return new GroupReturnShape(sourceFields, outNames, countName);
  }

  /** String value of a string-literal AST node — {@code null} for anything else. */
  private static String stringLiteralValue(final AST node) {
    if (node == null || node.getType() != XQ.Str) {
      return null;
    }
    final Object val = node.getValue();
    if (val instanceof String s)
      return s;
    if (val instanceof io.brackit.query.atomic.Str str)
      return str.stringValue();
    return val != null ? val.toString() : null;
  }

  /** Get the declared QNm of a LetBind's variable binding — {@code null} if absent. */
  private QNm extractLetBindVarName(final AST letBind) {
    if (letBind.getChildCount() < 1)
      return null;
    final AST varBinding = letBind.getChild(0);
    if (varBinding.getChildCount() < 1)
      return null;
    final Object val = varBinding.getChild(0).getValue();
    return val instanceof QNm qnm ? qnm : null;
  }

  /**
   * Return the QNm of a {@link XQ#VariableRef} that is the sole non-structural node
   * in the return subtree. Returns {@code null} if there are zero or multiple VarRefs,
   * or if any non-VarRef expression node (e.g. FunctionCall, ArithmeticOp) is present
   * — preventing mis-detection of {@code return ($d, $d)} or {@code return f($d)}.
   */
  private QNm extractSoleVariableRef(final AST node) {
    final QNm[] found = { null };
    final boolean[] tooComplex = { false };
    scanForSoleVarRef(node, found, tooComplex);
    return tooComplex[0] ? null : found[0];
  }

  private void scanForSoleVarRef(final AST node, final QNm[] found, final boolean[] tooComplex) {
    if (node == null || tooComplex[0])
      return;
    final int type = node.getType();
    if (type == XQ.VariableRef) {
      final Object val = node.getValue();
      if (val instanceof QNm qnm) {
        if (found[0] != null && !found[0].equals(qnm)) {
          tooComplex[0] = true;
        } else {
          found[0] = qnm;
        }
      }
      return;
    }
    // Expression-bearing node types disqualify the pattern — `return $d + 1`,
    // `return concat($d, "x")`, `return ($d, $d)` etc.
    switch (type) {
      case XQ.FunctionCall, XQ.ArithmeticExpr, XQ.DerefExpr, XQ.PathExpr, XQ.SequenceExpr, XQ.RangeExpr, XQ.IfExpr,
          XQ.LetBind, XQ.ComparisonExpr -> {
        tooComplex[0] = true;
        return;
      }
      default -> {
        /* structural / pass-through — recurse into children */ }
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      scanForSoleVarRef(node.getChild(i), found, tooComplex);
      if (tooComplex[0])
        return;
    }
  }

  // ==================== Source path-prefix extraction ====================

  /**
   * Extract the loop variable's source path as a list of step names. Each
   * array-descent ({@code [...][]}) is emitted as {@code "[]"}; each field
   * dereference emits the field's local name. The bottom of the traversal
   * must be a {@link XQ#VariableRef} or a {@link XQ#FunctionCall} whose result
   * is implicitly the document root (e.g. {@code jn:doc(...)}); both terminate
   * the prefix, meaning the path starts at whatever $doc root the query binds.
   *
   * <p>Returns {@code null} for any unrepresentable shape (arithmetic,
   * sequence constructor, if/then/else, etc.). Callers use absence of the
   * {@link VectorizedScanAnnotation#SOURCE_PATH_PREFIX} annotation as a
   * signal to use a path-agnostic fallback.
   *
   * <p>Production queries typically parse to one of:
   * <pre>
   * for $u in $doc[] → ArrayAccess(VariableRef, -1) ⇒ ["[]"]
   * for $u in $doc.items[] → ArrayAccess(DerefExpr, -1) ⇒ ["items", "[]"]
   * for $u in $doc[].items[] → ArrayAccess(DerefExpr(ArrayAccess(...), items), -1)
   * ⇒ ["[]", "items", "[]"]
   * for $u in $doc.items → DerefExpr(VariableRef, items) ⇒ ["items"]
   * for $u in jn:doc('db','r')[] → ArrayAccess(FunctionCall, -1) ⇒ ["[]"]
   * </pre>
   */
  private String[] extractPathPrefix(final AST sourceExpr) {
    if (sourceExpr == null) {
      return null;
    }
    final List<String> reversed = new ArrayList<>(4);
    AST current = sourceExpr;
    while (current != null) {
      final int type = current.getType();
      if (type == XQ.ArrayAccess) {
        // ArrayAccess(subject, index). Treat any index as a full-descent marker:
        // path-prefix scoping doesn't distinguish element positions, only depth.
        reversed.add("[]");
        current = current.getChildCount() > 0 ? current.getChild(0) : null;
        continue;
      }
      if (type == XQ.DerefExpr && current.getChildCount() >= 2) {
        final AST fieldNode = current.getChild(current.getChildCount() - 1);
        final String name = qnmLocalName(fieldNode.getValue());
        if (name == null) {
          return null;
        }
        reversed.add(name);
        current = current.getChild(0);
        continue;
      }
      if (type == XQ.VariableRef || type == XQ.FunctionCall) {
        // Terminal — the path starts from this root. The variable / function
        // identity itself is not part of the path scoping, since Sirix's
        // pathNodeKey paths are rooted at the document.
        break;
      }
      // Any other AST shape (arithmetic, comparison, sequence constructor,
      // if-expr, etc.) is not representable as a simple path prefix.
      return null;
    }
    // Reverse: traversal built the list innermost-first (closest to leaf),
    // callers want outermost-first (from document root toward the field).
    final String[] prefix = new String[reversed.size()];
    for (int i = 0; i < reversed.size(); i++) {
      prefix[i] = reversed.get(reversed.size() - 1 - i);
    }
    return prefix;
  }

  /** Extract the local name from a QNm or String value on an AST node; null otherwise. */
  private static String qnmLocalName(final Object value) {
    if (value instanceof QNm qnm) {
      return qnm.getLocalName();
    }
    if (value instanceof String s) {
      return s;
    }
    return null;
  }

  // ==================== Generic predicate-tree extraction ====================

  /**
   * Recursively build a {@link PredicateNode} from an AST predicate subtree.
   * Returns {@code null} if any part of the predicate isn't representable —
   * callers should treat {@code null} as "fall back to generic pipeline"
   * rather than silently dropping the unrepresentable clause.
   *
   * <p>Supported shapes:
   * <ul>
   * <li>{@code AndExpr(a, b, ...)} → {@link PredicateNode.And}
   * <li>{@code OrExpr(a, b, ...)} → {@link PredicateNode.Or}
   * <li>{@code NotExpr(x)} / {@code fn:not($x)} → {@link PredicateNode.Not}
   * <li>{@code $u.f OP literal} → {@link PredicateNode.NumCmp} / {@link PredicateNode.StrEq}
   * <li>{@code $u.f} (bare deref, EBV on JSON boolean) → {@link PredicateNode.BoolRef}
   * </ul>
   */
  private PredicateNode extractPredicate(AST node) {
    if (node == null)
      return null;

    final int type = node.getType();

    if (type == XQ.AndExpr) {
      List<PredicateNode> kids = new ArrayList<>(node.getChildCount());
      for (int i = 0; i < node.getChildCount(); i++) {
        PredicateNode c = extractPredicate(node.getChild(i));
        if (c == null)
          return null;
        kids.add(c);
      }
      return PredicateNode.and(kids);
    }
    if (type == XQ.OrExpr) {
      List<PredicateNode> kids = new ArrayList<>(node.getChildCount());
      for (int i = 0; i < node.getChildCount(); i++) {
        PredicateNode c = extractPredicate(node.getChild(i));
        if (c == null)
          return null;
        kids.add(c);
      }
      return PredicateNode.or(kids);
    }

    // Bare deref: EBV of a JSON boolean field.
    if (type == XQ.DerefExpr) {
      String bf = directDerefFieldName(node);
      return bf != null ? new PredicateNode.BoolRef(bf) : null;
    }

    // Comparison: either a direct GeneralCompGT(left, right) or
    // ComparisonExpr(cmpKind, left, right).
    String op = getComparisonOp(type);
    AST leftOperand, rightOperand;
    if (op != null && node.getChildCount() >= 2) {
      leftOperand = node.getChild(0);
      rightOperand = node.getChild(1);
    } else if (type == XQ.ComparisonExpr && node.getChildCount() >= 3) {
      op = getComparisonOp(node.getChild(0).getType());
      leftOperand = node.getChild(1);
      rightOperand = node.getChild(2);
    } else {
      return null;
    }
    if (op == null || leftOperand == null)
      return null;

    // $u.F OP literal — field on left.
    String field = directDerefFieldName(leftOperand);
    if (field != null) {
      PredicateNode numCmp = numericComparison(field, op, rightOperand);
      if (numCmp != null)
        return numCmp;
      String sv = extractStringLiteral(rightOperand);
      if (sv != null && "eq".equals(op))
        return new PredicateNode.StrEq(field, sv);
      return null;
    }
    // literal OP $u.F — field on right. Reverse the comparison direction.
    field = directDerefFieldName(rightOperand);
    if (field != null) {
      PredicateNode numCmp = numericComparison(field, reverseOp(op), leftOperand);
      if (numCmp != null)
        return numCmp;
      String sv = extractStringLiteral(leftOperand);
      if (sv != null && "eq".equals(op))
        return new PredicateNode.StrEq(field, sv);
    }
    return null;
  }

  private String getComparisonOp(int type) {
    return switch (type) {
      case XQ.GeneralCompGT, XQ.ValueCompGT -> "gt";
      case XQ.GeneralCompLT, XQ.ValueCompLT -> "lt";
      case XQ.GeneralCompGE, XQ.ValueCompGE -> "ge";
      case XQ.GeneralCompLE, XQ.ValueCompLE -> "le";
      case XQ.GeneralCompEQ, XQ.ValueCompEQ -> "eq";
      default -> null;
    };
  }

  private String reverseOp(String op) {
    return switch (op) {
      case "gt" -> "lt";
      case "lt" -> "gt";
      case "ge" -> "le";
      case "le" -> "ge";
      default -> op;
    };
  }

  // ==================== OrderBy extraction ====================

  private record OrderInfo(String field, String direction) {
  }

  private OrderInfo extractOrderBy(AST orderBy, QNm loopVar) {
    // Exactly ONE sort spec — the executor sorts by a single key, so claiming the
    // first spec of `order by $u.a, $u.b` would break ties differently than asked.
    int specCount = 0;
    for (int i = 0; i < orderBy.getChildCount(); i++) {
      if (orderBy.getChild(i).getType() == XQ.OrderBySpec) {
        specCount++;
      }
    }
    if (specCount != 1 || orderBy.getChildCount() < 1)
      return null;
    AST spec = orderBy.getChild(0);
    if (spec.getType() != XQ.OrderBySpec)
      return null;

    // Strict: the sort key must be DIRECTLY `$loopVar.field` — descending into a
    // wrapper (`order by fn:lower-case($u.name)`) would sort by the raw field values.
    String field = spec.getChildCount() >= 1 ? directLoopVarDerefField(spec.getChild(0), loopVar) : null;
    String direction = "ascending";
    for (int i = 1; i < spec.getChildCount(); i++) {
      if (spec.getChild(i).getType() == XQ.OrderByKind) {
        Object val = spec.getChild(i).getValue();
        if (val != null)
          direction = val.toString().toLowerCase();
      }
    }
    return field != null ? new OrderInfo(field, direction) : null;
  }

  // ==================== Field name extraction ====================

  /** Field name for a node that is DIRECTLY a DerefExpr — no descent into children. */
  private String directDerefFieldName(AST node) {
    if (node == null || node.getType() != XQ.DerefExpr || node.getChildCount() < 2)
      return null;
    final AST fieldNode = node.getChild(node.getChildCount() - 1);
    final Object value = fieldNode.getValue();
    if (value instanceof QNm qnm)
      return qnm.getLocalName();
    if (value instanceof String s)
      return s;
    return null;
  }

  // ==================== Literal extraction ====================

  /** {@code Long.MIN_VALUE} / {@code Long.MAX_VALUE} as decimals, for exact-long range checks. */
  private static final java.math.BigDecimal LONG_MIN = java.math.BigDecimal.valueOf(Long.MIN_VALUE);
  private static final java.math.BigDecimal LONG_MAX = java.math.BigDecimal.valueOf(Long.MAX_VALUE);

  /**
   * Build the comparison leaf for {@code field <op> literal} — or {@code null}
   * (fail closed, generic pipeline) when the literal cannot be represented
   * EXACTLY in the vectorized predicate encoding.
   *
   * <p>Literal handling (the interpreter is the spec):
   * <ul>
   * <li>{@code xs:integer} ({@link XQ#Int}): exact {@code long} →
   * {@link PredicateNode.NumCmp}; out-of-long-range integers fail closed.
   * The historical code truncated via {@code Number#longValue()} —
   * silently changing semantics for big literals.</li>
   * <li>{@code xs:double} ({@link XQ#Dbl}): finite values →
   * {@link PredicateNode.FpCmp}. The interpreter compares every numeric
   * document value against an xs:double in double space
   * ({@code Double.compare(v.doubleValue(), lit)} in
   * {@code Int64#cmp}/{@code Dbl#cmp}/{@code Dec#cmp}) — exactly the FpCmp
   * contract, including the interpreter-sanctioned precision loss for
   * integers above {@code 2^53}. NaN / ±INF literals fail closed.</li>
   * <li>{@code xs:decimal} ({@link XQ#Dec}): integral decimals that fit in a
   * long → {@link PredicateNode.NumCmp} (exact, and identical semantics for
   * every document value type). Anything else →
   * {@link PredicateNode.DecCmp} carrying the EXACT
   * {@link java.math.BigDecimal}: the interpreter compares integer and
   * decimal document values against an xs:decimal exactly in decimal space
   * ({@code IntNumeric extends DecNumeric}), so any lossy double image
   * would silently change results. The historical code truncated
   * {@code 9.99} to {@code 9}.</li>
   * </ul>
   */
  private PredicateNode numericComparison(String field, String op, AST node) {
    if (node == null || op == null)
      return null;
    final int type = node.getType();
    if (type == XQ.Int) {
      final Long lv = exactLongOf(node.getValue());
      return lv != null ? new PredicateNode.NumCmp(field, op, lv) : null;
    }
    if (type == XQ.Dbl) {
      final Double d = doubleOf(node.getValue());
      if (d == null || !Double.isFinite(d))
        return null;
      return new PredicateNode.FpCmp(field, op, d);
    }
    if (type == XQ.Dec) {
      final java.math.BigDecimal c = decimalOf(node.getValue());
      if (c == null)
        return null;
      // Integral and long-representable → exact NumCmp.
      if (c.stripTrailingZeros().scale() <= 0 && c.compareTo(LONG_MIN) >= 0 && c.compareTo(LONG_MAX) <= 0) {
        return new PredicateNode.NumCmp(field, op, c.longValueExact());
      }
      return new PredicateNode.DecCmp(field, op, c);
    }
    // Any other node type is not a plain numeric literal — fail closed. (The
    // historical catch-all read a Number value off ANY node type and truncated.)
    return null;
  }

  /** Exact long of an integer-literal value object; {@code null} when lossy or not numeric. */
  private static Long exactLongOf(Object val) {
    if (val instanceof io.brackit.query.atomic.Numeric num) {
      try {
        return num.decimalValue().longValueExact();
      } catch (ArithmeticException e) {
        return null;
      }
    }
    if (val instanceof Long || val instanceof Integer || val instanceof Short || val instanceof Byte) {
      return ((Number) val).longValue();
    }
    if (val instanceof String s) {
      try {
        return Long.parseLong(s);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }

  /** Double of a double-literal value object; {@code null} when not numeric. */
  private static Double doubleOf(Object val) {
    if (val instanceof io.brackit.query.atomic.Numeric num)
      return num.doubleValue();
    if (val instanceof Number n)
      return n.doubleValue();
    if (val instanceof String s) {
      try {
        return Double.parseDouble(s);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }

  /** Exact decimal of a decimal-literal value object; {@code null} when not numeric. */
  private static java.math.BigDecimal decimalOf(Object val) {
    if (val instanceof io.brackit.query.atomic.Numeric num)
      return num.decimalValue();
    if (val instanceof java.math.BigDecimal bd)
      return bd;
    if (val instanceof Number n)
      return new java.math.BigDecimal(n.toString());
    if (val instanceof String s) {
      try {
        return new java.math.BigDecimal(s);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }

  private String extractStringLiteral(AST node) {
    if (node == null)
      return null;
    if (node.getType() == XQ.Str) {
      Object val = node.getValue();
      if (val instanceof String s)
        return s;
      // Brackit Str type
      if (val instanceof io.brackit.query.atomic.Str str)
        return str.stringValue();
      if (val != null)
        return val.toString();
    }
    return null;
  }
}
