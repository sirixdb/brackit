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
import io.brackit.query.compiler.optimizer.SourceRef;
import io.brackit.query.compiler.optimizer.Stage;
import io.brackit.query.compiler.optimizer.VectorizedScanAnnotation;
import io.brackit.query.function.json.JSONFun;
import io.brackit.query.module.Namespaces;
import io.brackit.query.module.StaticContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  /** Guard against pathological ASTs when resolving a scan source through variable bindings. */
  private static final int MAX_UNWRAP_STEPS = 64;

  /**
   * Whether the backend this optimizer plans for can DECOMPOSE a predicate — evaluate each branch
   * over its own anchor and combine the per-branch results — rather than scanning from one global
   * anchor. Consumed by the anchor guard in {@code tryAnnotate}.
   *
   * <p>Per INSTANCE, supplied by the optimizer that builds this stage, deliberately NOT a static
   * flag. The capability belongs to the backend being planned for, and one process can host more
   * than one: a JVM running a decomposing backend alongside a plain {@code CompileChain} must not
   * have the plain one's queries annotated with claims only the other can serve. A static would
   * also latch on class initialization — an unrelated field read on the wiring class would enable
   * it process-wide, with no way back.
   */
  private final boolean decomposablePredicatesSupported;

  /** A stage for a backend that scans from a single anchor: the conservative default. */
  public VectorizedGroupByDetection() {
    this(false);
  }

  /**
   * @param decomposablePredicatesSupported whether the target backend decomposes predicates
   */
  public VectorizedGroupByDetection(final boolean decomposablePredicatesSupported) {
    this.decomposablePredicatesSupported = decomposablePredicatesSupported;
  }

  @Override
  public AST rewrite(StaticContext sctx, AST ast) {
    // Resolve a scan's source document (a `for $u in $doc[]` reaches its `jn:doc(...)` only through the
    // `let $doc := ...` binding), so collect every visible for/let binding once up front and thread the
    // map into the per-PipeExpr annotation.
    final Map<Object, AST> variableBindings = new HashMap<>();
    collectVariableBindings(ast, variableBindings);
    walkAndAnnotate(ast, variableBindings);
    withdrawRegroupedSources(ast, Set.of(), List.of());
    return ast;
  }

  /**
   * Withdraws any claim whose scan source is a variable a {@code group by} has already rebound.
   *
   * <p>A {@code group by} rebinds every non-grouping variable of its FLWOR to the sequence of THAT
   * group's values. {@link #resolveSourceRef} finds the document a scan reads by following the
   * variable to its binding clause, and that walk cannot see the grouping, so for
   *
   * <pre>
   * for $h in jn:doc('db','res')[]
   * let $k := $h.region
   * group by $k
   * return {"k": $k, "s": sum(for $x in $h return $x.width)}
   * </pre>
   *
   * the inner pipeline's source resolves back through {@code $h} to the whole document. A backend
   * that accepts that source folds every record for every group and answers each one with the global
   * sum — silently, and only for the shapes that read a field: {@code count($h)} builds no inner
   * pipeline and stays correct.
   *
   * <p>Running as a second pass keeps the resolution itself untouched: this only has to say that a
   * source it already produced cannot be proven, which {@link SourceRef#unknown()} states and every
   * compile-time gate already fails closed on. The claim on the grouping pipeline ITSELF is
   * unaffected — that one is annotated at the {@code PipeExpr}, above the {@code GroupBy}, where its
   * own source is still exactly what it says it is.
   *
   * @param node          the subtree to walk
   * @param regroupedVars variables an enclosing {@code group by} has rebound
   * @param chainVars     variables bound so far by the CURRENT pipeline's clauses
   */
  private static void withdrawRegroupedSources(final AST node, final Set<Object> regroupedVars,
      final List<Object> chainVars) {
    if (node.getType() == XQ.PipeExpr && !regroupedVars.isEmpty()) {
      withdrawIfSourceIsRegrouped(node, regroupedVars);
    }
    final List<Object> childChainVars = extendChainVars(node, chainVars);
    final Set<Object> childRegrouped = extendRegroupedVars(node, regroupedVars, chainVars);
    for (int i = 0, n = node.getChildCount(); i < n; i++) {
      withdrawRegroupedSources(node.getChild(i), childRegrouped, childChainVars);
    }
  }

  /**
   * The variables bound by the current pipeline's clauses, extended by the one {@code node} binds.
   * A {@link XQ#Start} resets the list: a nested pipeline opens its own clause scope, and the outer
   * variables it can still see are already accounted for in {@code regroupedVars}.
   */
  private static List<Object> extendChainVars(final AST node, final List<Object> chainVars) {
    final int type = node.getType();
    if (type == XQ.Start) {
      return List.of();
    }
    if (type != XQ.ForBind && type != XQ.LetBind || node.getChildCount() < 2) {
      return chainVars;
    }
    final Object varKey = bindingVariableKey(node.getChild(0));
    if (varKey == null) {
      return chainVars;
    }
    final List<Object> extended = new ArrayList<>(chainVars.size() + 1);
    extended.addAll(chainVars);
    extended.add(varKey);
    return extended;
  }

  /** A {@code GroupBy} rebinds every variable its pipeline has bound so far. */
  private static Set<Object> extendRegroupedVars(final AST node, final Set<Object> regroupedVars,
      final List<Object> chainVars) {
    if (node.getType() != XQ.GroupBy || chainVars.isEmpty()) {
      return regroupedVars;
    }
    final Set<Object> extended = new HashSet<>(regroupedVars);
    extended.addAll(chainVars);
    return extended;
  }

  /**
   * Replaces an annotated pipeline's source with {@link SourceRef#unknown()} when it scans a
   * regrouped variable. Unknown rather than removed: a missing source ref means "unannotated, admit
   * by executor default", which is the opposite of what this has to say.
   */
  private static void withdrawIfSourceIsRegrouped(final AST pipeExpr, final Set<Object> regroupedVars) {
    if (pipeExpr.getProperty(VectorizedScanAnnotation.SOURCE_REF) == null) {
      return;
    }
    final AST forBind = forBindOf(pipeExpr);
    if (forBind == null || forBind.getChildCount() < 2) {
      return;
    }
    final Object sourceVar = sourceVariableOrNull(forBind.getChild(1));
    if (sourceVar != null && regroupedVars.contains(sourceVar)) {
      pipeExpr.setProperty(VectorizedScanAnnotation.SOURCE_REF, SourceRef.unknown());
    }
  }

  /** The {@code ForBind} at the end of a pipeline's clause chain, or {@code null}. */
  private static AST forBindOf(final AST pipeExpr) {
    if (pipeExpr.getChildCount() < 1) {
      return null;
    }
    final AST chain = pipeExpr.getChild(0);
    if (chain.getType() != XQ.Start || chain.getChildCount() < 1) {
      return null;
    }
    AST forBind = chain.getLastChild();
    while (forBind != null && forBind.getType() == XQ.LetBind) {
      forBind = forBind.getLastChild();
    }
    return forBind != null && forBind.getType() == XQ.ForBind ? forBind : null;
  }

  /**
   * The variable a binding expression ultimately reads, looking through the deref / array-access /
   * filter layers a source path is written with; {@code null} when it is not rooted in a variable.
   */
  private static Object sourceVariableOrNull(final AST binding) {
    AST current = binding;
    for (int step = 0; current != null && step < MAX_UNWRAP_STEPS; step++) {
      final int type = current.getType();
      if (type == XQ.VariableRef) {
        return current.getValue();
      }
      if (type != XQ.DerefExpr && type != XQ.ArrayAccess && type != XQ.FilterExpr) {
        return null;
      }
      current = current.getChildCount() < 1 ? null : current.getChild(0);
    }
    return null;
  }

  private void walkAndAnnotate(AST node, Map<Object, AST> variableBindings) {
    if (node == null)
      return;
    if (node.getType() == XQ.PipeExpr) {
      tryAnnotate(node, variableBindings);
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      walkAndAnnotate(node.getChild(i), variableBindings);
    }
  }

  // ==================== Main pattern matcher ====================

  private void tryAnnotate(AST pipeExpr, Map<Object, AST> variableBindings) {
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
            PredicateNode pn = extractPredicate(current.getChild(0), loopVar);
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

    // ... or an ARITHMETIC expression over two of the loop variable's fields. Kept strictly apart
    // from returnField: `return $u.a * $u.b` must never be claimed as an aggregate over `a`, which
    // is what descending into the subtree for a field name would produce.
    final String[] returnBinary = binaryLoopVarDeref(returnExpr, loopVar);

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
    //
    // A DECOMPOSING backend needs less than that. Inclusion-exclusion counts each Or branch over
    // its OWN anchor and combines the per-branch results, so `$u.a > 1 or $u.b > 1` still visits a
    // record carrying only `b` — via `b`. A bare negation is served as the complement of its child
    // over all records, which likewise never requires the child's field to be present. Shapes that
    // admit such a decomposition are claimed too; see PredicateNode#isDecomposablyAnchorable, which
    // is a statement about the SHAPE, not a promise that a given executor implements one. A backend
    // that claims decomposition it has not implemented will under-count exactly as before.
    //
    // TWO levels of anchoring, because a backend that decomposes does not necessarily decompose for
    // every claim kind. STRICT means one field's slots enumerate every candidate record, which any
    // anchored scan can serve. DECOMPOSABLE means only that per-branch anchoring would work — true
    // of a cross-field disjunction — and is claimed for the predicate COUNT alone, whose executor
    // combines per-branch results. Aggregates, group-bys and sorted scans keep the strict
    // requirement: each reads a value per surviving record and so needs the anchor the scan is
    // actually driven by. Claiming more for them does not make them faster; it moves the refusal
    // from the optimizer to a query-time exception.
    boolean predicateStrictlyAnchored = predicateRepresentable;
    if (predicateRepresentable && !predicateConjuncts.isEmpty()) {
      final PredicateNode tree = PredicateNode.and(predicateConjuncts);
      if (tree.findSoundAnchorField() == null) {
        predicateStrictlyAnchored = false;
        if (!(decomposablePredicatesSupported && tree.isDecomposablyAnchorable())) {
          predicateRepresentable = false;
        }
      }
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
    if (hasGroupBy && groupSpecsExtractable && predicateStrictlyAnchored && !hasOrderBy) {
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
      // Document identity of the scan source. Set unconditionally (the translator only consults it once
      // a vectorized claim exists) so a resource-bound executor can decline a scan over a document it is
      // not bound to — see VectorizedExecutor#acceptsSource.
      pipeExpr.setProperty(VectorizedScanAnnotation.SOURCE_REF,
                           resolveSourceRef(forBind.getChild(1), variableBindings));
    }

    // Sorted scan emits FULL RECORDS sorted by ONE direct `$loopVar.field` key — only
    // claim it for exactly one order-by with exactly one spec, a return expression that
    // is exactly the loop variable, no group-by (the executor would drop the grouping),
    // and a representable (or absent) selection.
    if (hasOrderBy && orderByCount == 1 && !hasGroupBy && predicateStrictlyAnchored && orderField != null
        && isSoleLoopVarRef(returnExpr, loopVar)) {
      pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY, Boolean.TRUE);
      pipeExpr.setProperty(VectorizedScanAnnotation.ORDER_FIELD, orderField);
      pipeExpr.setProperty(VectorizedScanAnnotation.ORDER_DIRECTION, orderDirection);
    }

    // Pure aggregate candidate: ForBind -> Return($u.field), no group-by, no order-by.
    // A predicate, if present, is carried via PREDICATE_TREE — the enclosing
    // sum/avg/min/max/count() call fills AGGREGATE_FUNC at compile time and the
    // dispatcher routes to executePredicateAggregate.
    if (!hasGroupBy && !hasOrderBy && predicateStrictlyAnchored && returnField != null) {
      pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_AGGREGATE, Boolean.TRUE);
      pipeExpr.setProperty(VectorizedScanAnnotation.AGGREGATE_FIELD, returnField);
    } else if (!hasGroupBy && !hasOrderBy && predicateStrictlyAnchored && returnBinary != null) {
      // Same claim, arithmetic return. AGGREGATE_FIELD stays unset — the two properties are
      // alternatives and a backend reading the wrong one would aggregate one column where the
      // query asked for a product.
      pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_AGGREGATE, Boolean.TRUE);
      pipeExpr.setProperty(VectorizedScanAnnotation.AGGREGATE_BINARY, returnBinary);
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
   * {@code {left, op, right}} iff {@code expr} is {@code $loopVar.a OP $loopVar.b} for an operator
   * whose column-at-a-time evaluation is exactly its record-at-a-time one; {@code null} otherwise.
   *
   * <p>Multiplication, addition and subtraction only. Division is excluded deliberately: over the
   * integers a JSON document holds it is not closed, and {@code sum(a div b)} over a column of
   * exact decimals is not the sum of the doubles a columnar kernel would produce — a difference the
   * generic pipeline would not have. Both operands must be DIRECT derefs of the loop variable, for
   * the reason {@link #directLoopVarDerefField} gives: anything else computes over other values
   * than the query names.
   */
  private String[] binaryLoopVarDeref(final AST expr, final QNm loopVar) {
    if (expr == null || expr.getType() != XQ.ArithmeticExpr || expr.getChildCount() != 3) {
      return null;
    }
    final String op = switch (expr.getChild(0).getType()) {
      case XQ.MultiplyOp -> "*";
      case XQ.AddOp -> "+";
      case XQ.SubtractOp -> "-";
      default -> null;
    };
    if (op == null) {
      return null;
    }
    final String left = directLoopVarDerefField(expr.getChild(1), loopVar);
    final String right = directLoopVarDerefField(expr.getChild(2), loopVar);
    return left == null || right == null ? null : new String[] { left, op, right };
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

  // ==================== Source-document identity extraction ====================

  /**
   * Collect every {@link XQ#ForBind}/{@link XQ#LetBind} binding in the tree into {@code out}, keyed by
   * the declared variable's QNm. First (outermost) binding wins on shadowing — a heuristic that only
   * ever costs precision (a misresolved source yields {@link SourceRef#unknown()}, which fails closed).
   */
  private static void collectVariableBindings(final AST node, final Map<Object, AST> out) {
    if ((node.getType() == XQ.ForBind || node.getType() == XQ.LetBind) && node.getChildCount() >= 2) {
      final Object varKey = bindingVariableKey(node.getChild(0));
      if (varKey != null) {
        out.putIfAbsent(varKey, node.getChild(1));
      }
    }
    for (int i = 0, n = node.getChildCount(); i < n; i++) {
      collectVariableBindings(node.getChild(i), out);
    }
  }

  /**
   * The variable QNm bound by a {@code For}/{@code LetBind}'s first child (a
   * {@link XQ#TypedVariableBinding} whose own first child, the {@code Variable}, carries the QNm that a
   * {@link XQ#VariableRef} later resolves against). Falls back to the node's own value defensively.
   */
  private static Object bindingVariableKey(final AST typedVariableBinding) {
    if (typedVariableBinding.getChildCount() > 0) {
      return typedVariableBinding.getChild(0).getValue();
    }
    return typedVariableBinding.getValue();
  }

  /**
   * Resolve a loop variable's source expression down to the document it reads from, following
   * deref/array/filter layers and variable bindings, and classify it as a {@link SourceRef}. Never
   * {@code null}: an unresolvable, dynamic, cyclic, collection, or non-document source resolves to
   * {@link SourceRef#unknown()} so a resource-bound executor fails closed.
   */
  private SourceRef resolveSourceRef(final AST binding, final Map<Object, AST> variableBindings) {
    final Set<Object> resolvingVars = new HashSet<>(4);
    AST current = binding;
    for (int step = 0; current != null && step < MAX_UNWRAP_STEPS; step++) {
      switch (current.getType()) {
        case XQ.DerefExpr, XQ.ArrayAccess, XQ.FilterExpr -> {
          if (current.getChildCount() < 1) {
            return SourceRef.unknown();
          }
          current = current.getChild(0);
        }
        case XQ.VariableRef -> {
          final Object varKey = current.getValue();
          if (varKey == null || !resolvingVars.add(varKey)) {
            return SourceRef.unknown(); // unresolved or cyclic — cannot prove a single document
          }
          final AST resolved = variableBindings.get(varKey);
          if (resolved == null) {
            // No binding inside the query tree — typically an EXTERNAL variable bound at
            // execution time (or a for-loop/outer variable). The document identity is not
            // provable at compile time, but it IS verifiable at runtime by resolving the name
            // through the QueryContext — so classify as VARIABLE (runtime-checkable), not
            // UNKNOWN (hopeless). Compile-time gates still fail closed on VARIABLE.
            return varKey instanceof QNm variableName ? SourceRef.variable(variableName) : SourceRef.unknown();
          }
          current = resolved;
        }
        case XQ.ContextItemExpr -> {
          return SourceRef.contextItem(); // the caller's own bound read transaction
        }
        case XQ.FunctionCall -> {
          return functionCallSourceRef(current);
        }
        default -> {
          return SourceRef.unknown();
        }
      }
    }
    return SourceRef.unknown();
  }

  /**
   * Classify a {@link XQ#FunctionCall} scan source. A {@code jn:doc}/{@code jn:open} with literal
   * database and resource arguments (and, if present, a literal integer revision) yields a concrete
   * {@link SourceRef#document}; a dynamic argument, any other {@code jn:} opener (collection /
   * multi-revision — it spans more than one resource/revision), or a non-JSON function yields
   * {@link SourceRef#unknown()}.
   */
  private SourceRef functionCallSourceRef(final AST call) {
    if (!(call.getValue() instanceof QNm qnm) || !JSONFun.JSON_NSURI.equals(qnm.getNamespaceURI())) {
      return SourceRef.unknown();
    }
    final String local = qnm.getLocalName();
    if (!"doc".equals(local) && !"open".equals(local)) {
      return SourceRef.unknown();
    }
    if (call.getChildCount() < 2) {
      return SourceRef.unknown();
    }
    final String databaseName = stringLiteralValue(call.getChild(0));
    final String resourceName = stringLiteralValue(call.getChild(1));
    if (databaseName == null || resourceName == null) {
      return SourceRef.unknown(); // dynamic (non-literal) database/resource — unprovable
    }
    if (call.getChildCount() == 2) {
      return SourceRef.document(databaseName, resourceName, SourceRef.LATEST_REVISION);
    }
    final Integer revision = literalRevision(call.getChild(2));
    if (revision == null) {
      return SourceRef.unknown(); // dynamic revision — unprovable
    }
    return SourceRef.document(databaseName, resourceName, revision);
  }

  /** The exact int of a literal integer revision argument; {@code null} for anything non-literal/lossy. */
  private static Integer literalRevision(final AST node) {
    if (node == null || node.getType() != XQ.Int) {
      return null;
    }
    final Long lv = exactLongOf(node.getValue());
    if (lv == null || lv < Integer.MIN_VALUE || lv > Integer.MAX_VALUE) {
      return null;
    }
    return lv.intValue();
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
   *
   * <p>Field references must be DIRECTLY {@code $loopVar.field}, which is why {@code loopVar} is
   * threaded down here rather than each operand being read on its own. A nested deref
   * ({@code $u.inner.age}) or a reference to some other variable in scope
   * ({@code $other.age}) names a value the executor will not be looking at: it evaluates the
   * annotation against the loop record's DIRECT children, so {@code $u.inner.age gt 5} would
   * otherwise compile to the same {@code NumCmp[field=age, op=gt, value=5]} as
   * {@code $u.age gt 5}, and the executor would compare the outer {@code age} — silently, with
   * nothing left in the annotation for a consumer to notice. Declining costs the fast path;
   * guessing costs the answer.
   */
  private PredicateNode extractPredicate(AST node, QNm loopVar) {
    if (node == null)
      return null;

    final int type = node.getType();

    // Parentheses are grouping, not semantics: `($u.a ge 1 and $u.a le 2) or $u.a gt 9` arrives
    // with its left branch wrapped, and dropping the whole predicate over a pair of brackets left
    // an otherwise representable shape on the generic pipeline. Exactly one child: anything else
    // inside parentheses is a sequence, not a boolean, and stays unrepresentable.
    if (type == XQ.ParenthesizedExpr) {
      return node.getChildCount() == 1 ? extractPredicate(node.getChild(0), loopVar) : null;
    }

    // fn:not(...) — the only function call claimed here, and only in a function namespace we own,
    // so a user-defined `not` is never mistaken for it. Note that a negation cannot anchor on its
    // own child's field: a record MISSING `x` satisfies `not($u.x gt 5)`, since `not(false)` is
    // true. Representability is therefore NOT decided here — the tree is handed to the anchor
    // rules in PredicateNode, which either find a sound anchor elsewhere in the surrounding
    // conjunction or require the backend to serve the negation as a complement.
    if (type == XQ.FunctionCall && node.getChildCount() == 1 && node.getValue() instanceof QNm fn && "not".equals(fn
                                                                                                                    .getLocalName())
        && (Namespaces.FN_NSURI.equals(fn.getNamespaceURI()) || Namespaces.DEFAULT_FN_NSURI.equals(fn
                                                                                                     .getNamespaceURI()))) {
      final PredicateNode negated = extractPredicate(node.getChild(0), loopVar);
      return negated == null ? null : new PredicateNode.Not(negated);
    }

    // some $g in $u.field[] satisfies $g eq "literal" — membership in an array-valued field.
    if (type == XQ.QuantifiedExpr) {
      return arrayContains(node, loopVar);
    }

    if (type == XQ.AndExpr) {
      List<PredicateNode> kids = new ArrayList<>(node.getChildCount());
      for (int i = 0; i < node.getChildCount(); i++) {
        PredicateNode c = extractPredicate(node.getChild(i), loopVar);
        if (c == null)
          return null;
        kids.add(c);
      }
      return PredicateNode.and(kids);
    }
    if (type == XQ.OrExpr) {
      List<PredicateNode> kids = new ArrayList<>(node.getChildCount());
      for (int i = 0; i < node.getChildCount(); i++) {
        PredicateNode c = extractPredicate(node.getChild(i), loopVar);
        if (c == null)
          return null;
        kids.add(c);
      }
      return PredicateNode.or(kids);
    }

    // Bare deref: EBV of a JSON boolean field.
    if (type == XQ.DerefExpr) {
      String bf = directLoopVarDerefField(node, loopVar);
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
    String field = directLoopVarDerefField(leftOperand, loopVar);
    if (field != null) {
      PredicateNode numCmp = numericComparison(field, op, rightOperand);
      if (numCmp != null)
        return numCmp;
      String sv = extractStringLiteral(rightOperand);
      if (sv != null) {
        if ("eq".equals(op))
          return new PredicateNode.StrEq(field, sv);
        if ("ne".equals(op))
          return new PredicateNode.StrNe(field, sv);
      }
      return null;
    }
    // literal OP $u.F — field on right. Reverse the comparison direction.
    field = directLoopVarDerefField(rightOperand, loopVar);
    if (field != null) {
      PredicateNode numCmp = numericComparison(field, reverseOp(op), leftOperand);
      if (numCmp != null)
        return numCmp;
      String sv = extractStringLiteral(leftOperand);
      if (sv != null) {
        // Both eq and ne are symmetric, so the reversal reverseOp() applies to the ordering
        // operators is a no-op here and the literal side does not change the leaf.
        if ("eq".equals(op))
          return new PredicateNode.StrEq(field, sv);
        if ("ne".equals(op))
          return new PredicateNode.StrNe(field, sv);
      }
    }
    return null;
  }

  /**
   * {@code some $g in $u.field[] satisfies $g eq "literal"} as a {@link PredicateNode.ArrayContains}.
   *
   * <p>EXISTENTIAL only. {@code every $g in ... satisfies ...} is vacuously TRUE on a record whose
   * array is empty — and on one that has no such field at all — so it cannot anchor on that field,
   * which is the property every backend here relies on to visit candidate records at all.
   *
   * <p>The index must be the EMPTY sequence: {@code $u.f[]} iterates every element, while
   * {@code $u.f[[1]]} names one, and reading the second as the first would test a different value.
   */
  private PredicateNode arrayContains(final AST node, final QNm loopVar) {
    if (!ARRAY_CONTAINS_CLAIMED) {
      return null;
    }
    if (node.getChildCount() != 3 || node.getChild(0).getType() != XQ.SomeQuantifier) {
      return null;
    }
    final AST binding = node.getChild(1);
    if (binding.getType() != XQ.QuantifiedBinding || binding.getChildCount() != 2) {
      return null;
    }
    final AST typedBinding = binding.getChild(0);
    if (typedBinding.getType() != XQ.TypedVariableBinding || typedBinding.getChildCount() < 1) {
      return null;
    }
    final AST variable = typedBinding.getChild(0);
    if (variable.getType() != XQ.Variable || !(variable.getValue() instanceof QNm boundVar)) {
      return null;
    }
    final AST access = binding.getChild(1);
    if (access.getType() != XQ.ArrayAccess || access.getChildCount() != 2) {
      return null;
    }
    final AST index = access.getChild(1);
    if (index.getType() != XQ.SequenceExpr || index.getChildCount() != 0) {
      return null;
    }
    final String field = directLoopVarDerefField(access.getChild(0), loopVar);
    if (field == null) {
      return null;
    }
    final AST comparison = node.getChild(2);
    if (comparison.getType() != XQ.ComparisonExpr || comparison.getChildCount() != 3) {
      return null;
    }
    final int cmp = comparison.getChild(0).getType();
    if (cmp != XQ.ValueCompEQ && cmp != XQ.GeneralCompEQ) {
      return null;
    }
    String value = literalComparedToBoundVar(comparison.getChild(1), comparison.getChild(2), boundVar);
    if (value == null) {
      value = literalComparedToBoundVar(comparison.getChild(2), comparison.getChild(1), boundVar);
    }
    return value == null ? null : new PredicateNode.ArrayContains(field, value);
  }

  /**
   * Whether the array-membership shape is claimed for backends at all.
   *
   * <p>On, with a kill switch. The claim was not free to make: an anchored backend reaches
   * candidate records through the anchor field's slots, and a field whose value is an ARRAY was not
   * returned by that lookup at all — measured, the scan visited nothing and counted zero where the
   * interpreter counted 686. That gap is closed on the storage side (the name-key column now uses
   * the field-name ROLE predicate rather than the primitive-layout one); the switch stays so the
   * claim can be taken back without a rebuild if a shape turns up that a backend mishandles.
   */
  private static final boolean ARRAY_CONTAINS_CLAIMED = !"false".equalsIgnoreCase(System.getProperty(
                                                                                                     "brackit.predicate.arrayContains",
                                                                                                     "true"));

  /** The string literal, when {@code varSide} IS the quantifier's bound variable. */
  private String literalComparedToBoundVar(final AST varSide, final AST literalSide, final QNm boundVar) {
    if (varSide.getType() != XQ.VariableRef || !boundVar.equals(varSide.getValue())) {
      return null;
    }
    return extractStringLiteral(literalSide);
  }

  private String getComparisonOp(int type) {
    return switch (type) {
      case XQ.GeneralCompGT, XQ.ValueCompGT -> "gt";
      case XQ.GeneralCompLT, XQ.ValueCompLT -> "lt";
      case XQ.GeneralCompGE, XQ.ValueCompGE -> "ge";
      case XQ.GeneralCompLE, XQ.ValueCompLE -> "le";
      case XQ.GeneralCompEQ, XQ.ValueCompEQ -> "eq";
      case XQ.GeneralCompNE, XQ.ValueCompNE -> "ne";
      default -> null;
    };
  }

  /** {@code ne} is symmetric, like {@code eq}, so the default carries it through unchanged. */
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
