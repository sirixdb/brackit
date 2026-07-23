/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.brackit.query.compiler.translator;

import java.util.ArrayList;
import java.util.List;

import io.brackit.query.atomic.QNm;
import io.brackit.query.compiler.optimizer.PredicateNode;
import io.brackit.query.compiler.optimizer.SourceRef;
import io.brackit.query.compiler.optimizer.VectorizedExecutor;
import io.brackit.query.compiler.optimizer.VectorizedScanAnnotation;
import io.brackit.query.expr.PipeExpr;
import io.brackit.query.expr.RuntimeSourceGatedExpr;
import io.brackit.query.expr.VectorizedGroupByExpr;
import io.brackit.query.operator.Count;
import io.brackit.query.operator.ForBind;
import io.brackit.query.operator.GroupBy;
import io.brackit.query.operator.NLJoin;
import io.brackit.query.operator.SpillableGroupBy;
import io.brackit.query.util.Cmp;
import io.brackit.query.util.aggregator.Aggregate;
import io.brackit.query.util.sort.Ordering;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.compiler.AST;
import io.brackit.query.compiler.XQ;
import io.brackit.query.operator.Check;
import io.brackit.query.operator.LetBind;
import io.brackit.query.operator.Operator;
import io.brackit.query.operator.OrderBy;
import io.brackit.query.operator.Select;
import io.brackit.query.operator.Start;
import io.brackit.query.operator.TableJoin;
import io.brackit.query.operator.morsel.MorselPipeline;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.type.SequenceType;

/**
 * Sequential (top-down) pipeline strategy for compiling PipeExpr nodes.
 * Extracted from TopDownTranslator.
 */
public class SequentialPipelineStrategy implements PipelineStrategy {

  /**
   * Process-wide vectorized executor — set by bjq or SirixDB at startup.
   * Retained for backwards compatibility with bench harnesses; the
   * preferred entry point for per-compile scopes is
   * {@link #setThreadVectorizedExecutor(VectorizedExecutor)}, which
   * sets a thread-local fallback that takes precedence over this static
   * field when present.
   */
  private static volatile VectorizedExecutor vectorizedExecutor;

  /**
   * Per-thread executor override. Preferred over the static field for
   * client-driven compilation (e.g. SirixDB's {@code SirixCompileChain})
   * because:
   * <ul>
   * <li>no global "last writer wins" race when multiple threads compile
   * queries against different resources in parallel;</li>
   * <li>the executor reference is captured in the compiled
   * {@link VectorizedGroupByExpr} at compile time, so the
   * thread-local only needs to be live during compile — after that,
   * the {@code Expr} is self-sufficient and can run on any thread.</li>
   * </ul>
   *
   * <p>Read order in {@link #tryVectorizedExpr(AST, boolean)}: thread-local
   * → static. A caller that has set neither gets no fast path.
   */
  private static final ThreadLocal<VectorizedExecutor> threadLocalExecutor = new ThreadLocal<>();

  /**
   * When {@code true}, PipeExprs that fall out of the vectorized fast path and
   * do not contain pipeline breakers (GroupBy, OrderBy, Join) are wrapped in a
   * {@link MorselPipeline} for DuckDB-style morsel-driven parallel fan-out.
   * Off by default — callers opt in via {@link #setMorselEnabled(boolean)}.
   */
  private static volatile boolean morselEnabled;

  /** Register a vectorized executor for automatic optimization. */
  public static void setVectorizedExecutor(VectorizedExecutor executor) {
    vectorizedExecutor = executor;
  }

  /**
   * Set a thread-local vectorized executor override. Takes precedence over
   * the process-wide static. Callers must clear via
   * {@link #clearThreadVectorizedExecutor()} in a {@code finally} to avoid
   * leaking the executor reference across thread-pool worker reuse.
   */
  public static void setThreadVectorizedExecutor(VectorizedExecutor executor) {
    if (executor == null) {
      threadLocalExecutor.remove();
    } else {
      threadLocalExecutor.set(executor);
    }
  }

  /** Clear any thread-local executor set via {@link #setThreadVectorizedExecutor}. */
  public static void clearThreadVectorizedExecutor() {
    threadLocalExecutor.remove();
  }

  /** Enable or disable morsel-driven parallel fan-out for the sequential fallback path. */
  public static void setMorselEnabled(boolean enabled) {
    morselEnabled = enabled;
  }

  /** Whether morsel-driven parallel fan-out is enabled. */
  public static boolean isMorselEnabled() {
    return morselEnabled;
  }

  /**
   * Get the effective vectorized executor (thread-local override if set,
   * else the process-wide static). Used by BlockPipelineStrategy too.
   */
  public static VectorizedExecutor getVectorizedExecutor() {
    final VectorizedExecutor local = threadLocalExecutor.get();
    return local != null ? local : vectorizedExecutor;
  }

  /**
   * Check AST annotations and create the appropriate VectorizedGroupByExpr.
   * Shared by both Sequential and Block pipeline strategies.
   * <p>
   * The {@code countWrapped} flag indicates whether this PipeExpr is the
   * argument of a {@code count()} function call. Filter-count patterns
   * are only intercepted when wrapped in {@code count()}, because the
   * vectorized executor returns a scalar count — not the filtered items.
   */
  public static Expr tryVectorizedExpr(AST node) throws QueryException {
    return tryVectorizedExpr(node, false);
  }

  public static Expr tryVectorizedExpr(AST node, boolean countWrapped) throws QueryException {
    return tryVectorizedExpr(node, countWrapped, null);
  }

  /**
   * Supplies the generic (always-correct) compilation of the SAME query fragment, for call sites
   * able to compile both sides so a {@link SourceRef.Kind#VARIABLE} source can be decided per
   * evaluation (see {@link RuntimeSourceGatedExpr}). May run the compiler, hence may throw.
   */
  @FunctionalInterface
  public interface GenericExprSupplier {
    Expr get() throws QueryException;
  }

  public static Expr tryVectorizedExpr(AST node, boolean countWrapped, GenericExprSupplier generic)
      throws QueryException {
    // Thread-local override takes precedence — the SirixCompileChain
    // installs a per-compile executor here without touching the static.
    VectorizedExecutor executor = threadLocalExecutor.get();
    if (executor == null) {
      executor = vectorizedExecutor;
    }
    if (executor == null)
      return null;

    // Source-document identity (from the loop variable's IN clause). A resource-bound
    // executor answers a scan from ITS columns, so serving a scan over a document it is
    // not bound to would return the wrong resource's data. Ask before substituting any
    // vectorized expression; a decline falls through to the generic (correct) pipeline.
    // The annotation is present on every walker-annotated scan; when absent (legacy
    // callers / hand-built ASTs) the executor's default accept-all applies unchanged.
    final VectorizedExecutor boundExecutor = executor;
    return gateBySource(node, boundExecutor, () -> buildVectorizedExpr(node, countWrapped, boundExecutor), generic);
  }

  /** Lazily builds a vectorized expression once the source gate admits it (may yield {@code null}). */
  @FunctionalInterface
  public interface VectorizedExprSupplier {
    Expr get();
  }

  /**
   * SINGLE AUTHORITY for the source-identity gate. Every site that substitutes a vectorized
   * expression for a scan MUST route through here — a site with its own (or no) gate is how a
   * resource-bound executor ends up serving a scan over a document it is not bound to
   * (wrong results). Semantics, by the annotated {@link SourceRef}:
   * <ul>
   * <li>no annotation — legacy/hand-built ASTs: the executor's default applies, admit;</li>
   * <li>{@link SourceRef.Kind#VARIABLE} — unverifiable at compile time: when the caller can
   * supply the generic compilation, substitute a {@link RuntimeSourceGatedExpr} deciding per
   * evaluation against the actual binding; otherwise fail closed ({@code null});</li>
   * <li>anything else — ask {@link VectorizedExecutor#acceptsSource(SourceRef)}; a decline
   * returns {@code null} and the caller falls through to its generic compilation.</li>
   * </ul>
   */
  public static Expr gateBySource(AST pipe, VectorizedExecutor executor, VectorizedExprSupplier vectorized,
      GenericExprSupplier generic) throws QueryException {
    final SourceRef sourceRef = (SourceRef) pipe.getProperty(VectorizedScanAnnotation.SOURCE_REF);
    if (sourceRef == null) {
      return vectorized.get();
    }
    if (sourceRef.kind() == SourceRef.Kind.VARIABLE) {
      if (generic == null) {
        return null;
      }
      final Expr vec = vectorized.get();
      if (vec == null) {
        return null;
      }
      return new RuntimeSourceGatedExpr(executor, sourceRef, vec, generic.get());
    }
    if (!executor.acceptsSource(sourceRef)) {
      return null;
    }
    return vectorized.get();
  }

  /** The pattern matcher proper: builds the vectorized expression for an ACCEPTED source. */
  private static Expr buildVectorizedExpr(AST node, boolean countWrapped, VectorizedExecutor executor) {

    // Source-path prefix (from the loop variable's IN clause). Threaded through
    // every vectorized Expr so the executor can combine it with per-predicate
    // field names to fully-qualify query paths and filter matches to the
    // correct tree depth.
    final String[] sourcePath = (String[]) node.getProperty(VectorizedScanAnnotation.SOURCE_PATH_PREFIX);

    // Pattern 0 (highest priority when count-wrapped): count-distinct answered
    // from an HLL cardinality sketch. The walker sets VECTORIZED_COUNT_DISTINCT
    // in addition to VECTORIZED_GROUPBY on the same PipeExpr because the shape
    // `for $u let $d := $u.F group by $d return $d` is *structurally* a
    // group-by; the count-distinct specialization only applies when the outer
    // expression is count(...). Checking this before the group-by branch lets
    // the HLL path win when count-wrapped; uncount-wrapped calls fall through
    // to the materializing group-by below.
    if (countWrapped && Boolean.TRUE.equals(node.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT_DISTINCT))) {
      String field = (String) node.getProperty(VectorizedScanAnnotation.COUNT_DISTINCT_FIELD);
      if (field != null) {
        return VectorizedGroupByExpr.countDistinct(executor, sourcePath, field);
      }
    }

    // Pattern 1: Group-by (with optional filter). PREDICATE_TREE carries the
    // full WHERE clause; the executor receives it and evaluates any
    // AND/OR/NOT combination of NumCmp/StrEq/BoolRef leaves in one scan.
    if (Boolean.TRUE.equals(node.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY))) {
      String groupField = (String) node.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD);
      if (groupField == null)
        return null;
      PredicateNode predicate = (PredicateNode) node.getProperty(VectorizedScanAnnotation.PREDICATE_TREE);
      if (predicate != null) {
        return VectorizedGroupByExpr.predicateGroupByCount(executor, sourcePath, predicate, groupField);
      }
      return VectorizedGroupByExpr.groupBy(executor, sourcePath, groupField);
    }

    // Pattern 1b: generalized group-by (multi-key and/or query-renamed output
    // fields). Capability-gated at translate time, same rationale as the sorted
    // scan below. The canonical single-key Pattern 1 is checked first so
    // executors keep their specialized projection fast paths for that shape.
    if (executor.supportsMultiKeyGroupBy() && Boolean.TRUE.equals(node.getProperty(
                                                                                   VectorizedScanAnnotation.VECTORIZED_GROUPBY_MULTI))) {
      String[] groupFields = (String[]) node.getProperty(VectorizedScanAnnotation.GROUPBY_FIELDS);
      String[] outNames = (String[]) node.getProperty(VectorizedScanAnnotation.GROUPBY_OUT_NAMES);
      String countName = (String) node.getProperty(VectorizedScanAnnotation.GROUPBY_COUNT_NAME);
      if (groupFields != null && outNames != null && countName != null && groupFields.length == outNames.length
          && groupFields.length >= 1) {
        PredicateNode predicate = (PredicateNode) node.getProperty(VectorizedScanAnnotation.PREDICATE_TREE);
        return VectorizedGroupByExpr.groupByMulti(executor, sourcePath, groupFields, outNames, countName, predicate);
      }
    }

    // Pattern 2: Filtered count — only when wrapped in count(). Requires
    // PREDICATE_TREE; without it the query wasn't representable by the walker
    // and we fall back to the generic pipeline.
    if (countWrapped && Boolean.TRUE.equals(node.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT))) {
      PredicateNode predicate = (PredicateNode) node.getProperty(VectorizedScanAnnotation.PREDICATE_TREE);
      if (predicate != null) {
        return VectorizedGroupByExpr.predicateCount(executor, sourcePath, predicate);
      }
    }

    // Pattern 3: Sorted scan. Capability-gated at translate time: the substitution
    // is irreversible (no Volcano pipeline left at evaluate time), so dispatching to
    // an executor whose executeSortedScan answers null would turn a valid query into
    // a runtime error instead of a fallback.
    if (executor.supportsSortedScan() && Boolean.TRUE.equals(node.getProperty(
                                                                              VectorizedScanAnnotation.VECTORIZED_ORDERBY))) {
      String orderField = (String) node.getProperty(VectorizedScanAnnotation.ORDER_FIELD);
      String direction = (String) node.getProperty(VectorizedScanAnnotation.ORDER_DIRECTION);
      if (orderField != null) {
        return VectorizedGroupByExpr.sorted(executor, sourcePath, orderField, direction);
      }
    }

    // Pattern 4: Pure aggregate (sum/avg/min/max/count over flat scan).
    // Generic predicate-tree filtered aggregate takes priority when
    // PREDICATE_TREE is set — fuses filter + aggregate in one scan.
    if (Boolean.TRUE.equals(node.getProperty(VectorizedScanAnnotation.VECTORIZED_AGGREGATE))) {
      String func = (String) node.getProperty(VectorizedScanAnnotation.AGGREGATE_FUNC);
      String field = (String) node.getProperty(VectorizedScanAnnotation.AGGREGATE_FIELD);
      if (func != null) {
        PredicateNode predicate = (PredicateNode) node.getProperty(VectorizedScanAnnotation.PREDICATE_TREE);
        if (predicate != null) {
          return VectorizedGroupByExpr.predicateAggregate(executor, sourcePath, predicate, func, field);
        }
        return VectorizedGroupByExpr.aggregate(executor, sourcePath, func, field);
      }
    }

    return null;
  }

  @Override
  public Expr compilePipeExpr(AST node, Compiler compiler) throws QueryException {
    // Vectorized substitution. For a VARIABLE source (an external variable, unverifiable at
    // compile time) the generic pipeline is compiled as well and the choice is made per
    // evaluation against the actual binding — see RuntimeSourceGatedExpr.
    Expr vectorized = tryVectorizedExpr(node, false, () -> compileGenericPipeExpr(node, compiler));
    if (vectorized != null)
      return vectorized;
    return compileGenericPipeExpr(node, compiler);
  }

  /** The generic (Volcano) pipeline compilation — the always-correct path/fallback. */
  protected Expr compileGenericPipeExpr(AST node, Compiler compiler) throws QueryException {
    final int initialBindSize = compiler.table.bound().length;
    // AST-level pre-scan: decide whether morsel wrapping is safe BEFORE compilation
    // (avoids traversing a not-yet-built Operator graph whose upstream API varies).
    final boolean morselSafe = morselEnabled && isMorselParallelizable(node);

    Operator root = anyOp(null, node.getChild(0), compiler);

    // for simpler scoping, the return expression is
    // at the right-most leaf
    AST returnExpr = node.getChild(0);
    while (returnExpr.getType() != XQ.End) {
      returnExpr = returnExpr.getLastChild();
    }
    Expr expr = compiler.anyExpr(returnExpr.getChild(0));

    // clear operator bindings
    final int unbind = compiler.table.bound().length - initialBindSize;
    for (int i = 0; i < unbind; i++) {
      compiler.table.unbind();
    }

    if (morselSafe) {
      root = new MorselPipeline(root);
    }

    return new PipeExpr(root, expr);
  }

  /**
   * AST-level safety check for morsel wrapping. Returns true when the pipeline
   * starts with a data-driven scan (ForBind) and contains no pipeline breakers
   * (GroupBy, OrderBy, Join) — those require global state and must not cross
   * morsel boundaries.
   *
   * @param pipeExpr the PipeExpr AST node
   * @return {@code true} iff the pipeline is safe to wrap in a {@link MorselPipeline}
   */
  private static boolean isMorselParallelizable(AST pipeExpr) {
    if (pipeExpr == null || pipeExpr.getChildCount() == 0) {
      return false;
    }
    final AST start = pipeExpr.getChild(0);
    // Walk the pipeline chain: Start -> (ForBind|LetBind|Selection|Count)* -> End
    // Require the first operator under Start to be a ForBind (data-driven scan).
    AST cursor = start;
    boolean seenForBind = false;
    while (cursor != null) {
      final int t = cursor.getType();
      if (t == XQ.End) {
        break;
      }
      if (t == XQ.GroupBy || t == XQ.OrderBy || t == XQ.Join) {
        return false;
      }
      if (t == XQ.ForBind) {
        seenForBind = true;
      }
      if (cursor.getChildCount() == 0) {
        break;
      }
      cursor = cursor.getLastChild();
    }
    return seenForBind;
  }

  protected Operator anyOp(Operator in, AST node, Compiler compiler) throws QueryException {
    return _anyOp(in, node, compiler);
  }

  protected Operator _anyOp(Operator in, AST node, Compiler compiler) throws QueryException {
    switch (node.getType()) {
      case XQ.Start -> {
        if (node.getChildCount() == 0) {
          return new Start();
        } else {
          return anyOp(new Start(), node.getLastChild(), compiler);
        }
      }
      case XQ.End -> {
        return in;
      }
      case XQ.ForBind -> {
        return forBind(in, node, compiler);
      }
      case XQ.LetBind -> {
        return letBind(in, node, compiler);
      }
      case XQ.Selection -> {
        return select(in, node, compiler);
      }
      case XQ.OrderBy -> {
        return orderBy(in, node, compiler);
      }
      case XQ.GroupBy -> {
        return groupBy(in, node, compiler);
      }
      case XQ.Count -> {
        return count(in, node, compiler);
      }
      case XQ.Join -> {
        return join(in, node, compiler);
      }
      default -> throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                                          "Unexpected AST operator node '%s' of type: %s",
                                          node,
                                          node.getType());
    }
  }

  @SuppressWarnings("unchecked")
  protected Operator groupBy(Operator in, AST node, Compiler compiler) throws QueryException {
    int pos = 0;
    while (node.getChild(pos).getType() == XQ.GroupBySpec) {
      pos++;
    }
    int grpSpecCnt = pos;
    // collect additional aggregate bindings
    List<Compiler.AggregateBinding> bnds = new ArrayList<>();
    while (node.getChild(pos).getType() == XQ.AggregateSpec) {
      AST aggSpec = node.getChild(pos);
      QNm var = (QNm) aggSpec.getChild(0).getValue();
      for (int j = 1; j < aggSpec.getChildCount(); j++) {
        AST aggBinding = aggSpec.getChild(j);
        AST typedVarBnd = aggBinding.getChild(0);
        Aggregate agg = compiler.aggregate(aggBinding.getChild(1));
        QNm aggVar = (QNm) typedVarBnd.getChild(0).getValue();
        SequenceType aggType = SequenceType.ITEM_SEQUENCE;
        if (typedVarBnd.getChildCount() == 2) {
          aggType = compiler.sequenceType(typedVarBnd.getChild(1));
        }
        bnds.add(new Compiler.AggregateBinding(var, aggVar, aggType, agg));
      }
      pos++;
    }
    Aggregate dftAgg = compiler.aggregate(node.getChild(pos).getChild(0));
    Aggregate[] addAggs = new Aggregate[bnds.size()];
    for (int i = 0; i < bnds.size(); i++) {
      Compiler.AggregateBinding bnd = bnds.get(i);
      addAggs[i] = bnd.agg;
    }
    boolean sequential = node.checkProperty("sequential");
    SpillableGroupBy groupBy = new SpillableGroupBy(in, dftAgg, addAggs, grpSpecCnt, sequential);
    // resolve positions grouping variables
    for (int i = 0; i < grpSpecCnt; i++) {
      QNm grpVarName = (QNm) node.getChild(i).getChild(0).getValue();
      compiler.table.resolve(grpVarName, groupBy.group(i));
    }
    // resolve positions for additional aggregates
    for (int i = 0; i < bnds.size(); i++) {
      Compiler.AggregateBinding bnd = bnds.get(i);
      compiler.table.resolve(bnd.srcVar, groupBy.aggregate(i));
    }
    // bind additional aggregates
    for (Compiler.AggregateBinding bnd : bnds) {
      compiler.table.bind(bnd.aggVar, bnd.aggVarType);
      // fake binding
      compiler.table.resolve(bnd.aggVar);
    }
    addChecks(groupBy, (List<QNm>) node.getProperty("check"), compiler);
    return anyOp(groupBy, node.getLastChild(), compiler);
  }

  @SuppressWarnings("unchecked")
  protected Operator join(Operator in, AST node, Compiler compiler) throws QueryException {
    // get join type
    Cmp cmp = (Cmp) node.getProperty("cmp");
    boolean isGcmp = node.checkProperty("GCmp");

    // compile left (outer) join branch (skip initial start)
    Operator leftIn = anyOp(in, node.getChild(0).getChild(0), compiler);
    AST tmp = node.getChild(0);
    while (tmp.getType() != XQ.End) {
      tmp = tmp.getLastChild();
    }
    Expr leftExpr = compiler.anyExpr(tmp.getChild(0));

    // compile right (inner) join branch
    Operator rightIn = anyOp(new Start(), node.getChild(1), compiler);
    tmp = node.getChild(1);
    while (tmp.getType() != XQ.End) {
      tmp = tmp.getLastChild();
    }
    Expr rightExpr = compiler.anyExpr(tmp.getChild(0));

    boolean leftJoin = node.checkProperty("leftJoin");
    boolean skipSort = node.checkProperty("skipSort");
    TableJoin join = new TableJoin(cmp, isGcmp, leftJoin, skipSort, leftIn, leftExpr, rightIn, rightExpr);

    QNm prop = (QNm) node.getProperty("group");
    if (prop != null) {
      compiler.table.resolve(prop, join.group());
    }
    addChecks(join, (List<QNm>) node.getProperty("check"), compiler);

    Operator op = join;
    AST post = node.getChild(2).getChild(0);
    if ((post.getType() != XQ.End)) {
      op = anyOp(join, post, compiler);
    }

    return anyOp(op, node.getLastChild(), compiler);
  }

  protected Operator nljoin(Operator in, AST node, Compiler compiler) throws QueryException {
    // compile left (outer) join branch (skip initial start)
    Operator leftIn = anyOp(in, node.getChild(0).getChild(0), compiler);

    // get join type
    Cmp cmp = (Cmp) node.getProperty("cmp");
    boolean isGcmp = node.checkProperty("GCmp");

    AST tmp = node.getChild(0);
    while (tmp.getType() != XQ.End) {
      tmp = tmp.getLastChild();
    }
    Expr leftExpr = compiler.anyExpr(tmp.getChild(0));

    // compile right (inner) join branch
    Operator rightIn = anyOp(new Start(), node.getChild(1), compiler);
    tmp = node.getChild(1);
    while (tmp.getType() != XQ.End) {
      tmp = tmp.getLastChild();
    }
    Expr rightExpr = compiler.anyExpr(tmp.getChild(0));

    boolean leftJoin = node.checkProperty("leftJoin");
    boolean skipSort = node.checkProperty("skipSort");
    Operator join = new NLJoin(leftIn, rightIn, leftExpr, rightExpr, cmp, isGcmp, leftJoin);

    return anyOp(join, node.getLastChild(), compiler);
  }

  @SuppressWarnings("unchecked")
  protected Operator forBind(Operator in, AST node, Compiler compiler) throws QueryException {
    int pos = 0;
    AST runVarDecl = node.getChild(pos++);
    QNm runVarName = (QNm) runVarDecl.getChild(0).getValue();
    SequenceType runVarType = SequenceType.ITEM_SEQUENCE;
    if (runVarDecl.getChildCount() == 2) {
      runVarType = compiler.sequenceType(runVarDecl.getChild(1));
    }
    AST posBindingOrSourceExpr = node.getChild(pos++);
    // 'allowing empty' marker child (see Compiler#forClause) or the lifted property form.
    boolean allowingEmpty = node.checkProperty("allowingEmpty");
    if (posBindingOrSourceExpr.getType() == XQ.AllowingEmpty) {
      allowingEmpty = true;
      posBindingOrSourceExpr = node.getChild(pos++);
    }
    QNm posVarName = null;
    if (posBindingOrSourceExpr.getType() == XQ.TypedVariableBinding) {
      posVarName = (QNm) posBindingOrSourceExpr.getChild(0).getValue();
      posBindingOrSourceExpr = node.getChild(pos++);
    }
    Expr sourceExpr = compiler.expr(posBindingOrSourceExpr, true);

    Binding posBinding = null;
    compiler.table.bind(runVarName, runVarType);
    // Fake binding of run variable because set-oriented processing requires
    // the variable anyway
    compiler.table.resolve(runVarName);

    if (posVarName != null) {
      posBinding = compiler.table.bind(posVarName, SequenceType.INTEGER);
      // Fake binding of pos variable to simplify compilation.
      compiler.table.resolve(posVarName);
      // TODO Optimize and do not bind variable if not necessary
    }
    ForBind forBind = new ForBind(in, sourceExpr, allowingEmpty);
    if (posBinding != null) {
      forBind.bindPosition(posBinding.isReferenced());
    }
    addChecks(forBind, (List<QNm>) node.getProperty("check"), compiler);
    return anyOp(forBind, node.getLastChild(), compiler);
  }

  @SuppressWarnings("unchecked")
  protected Operator letBind(Operator in, AST node, Compiler compiler) throws QueryException {
    int pos = 0;
    AST letVarDecl = node.getChild(pos++);
    QNm letVarName = (QNm) letVarDecl.getChild(0).getValue();
    SequenceType letVarType = SequenceType.ITEM_SEQUENCE;
    if (letVarDecl.getChildCount() == 2) {
      letVarType = compiler.sequenceType(letVarDecl.getChild(1));
    }
    Expr sourceExpr = compiler.expr(node.getChild(pos++), true);
    compiler.table.bind(letVarName, letVarType);

    // Fake binding of let variable because set-oriented processing requires
    // the variable anyway
    compiler.table.resolve(letVarName);
    LetBind letBind = new LetBind(in, sourceExpr);
    addChecks(letBind, (List<QNm>) node.getProperty("check"), compiler);
    return anyOp(letBind, node.getLastChild(), compiler);
  }

  @SuppressWarnings("unchecked")
  protected Operator count(Operator in, AST node, Compiler compiler) throws QueryException {
    int pos = 0;
    AST posVarDecl = node.getChild(pos++);
    QNm posVarName = (QNm) posVarDecl.getChild(0).getValue();
    SequenceType posVarType = SequenceType.ITEM_SEQUENCE;
    if (posVarDecl.getChildCount() == 2) {
      posVarType = compiler.sequenceType(posVarDecl.getChild(1));
    }
    compiler.table.bind(posVarName, posVarType);

    // Fake binding of let variable because set-oriented processing requires
    // the variable anyway
    compiler.table.resolve(posVarName);
    Count count = new Count(in);
    addChecks(count, (List<QNm>) node.getProperty("check"), compiler);
    return anyOp(count, node.getLastChild(), compiler);
  }

  @SuppressWarnings("unchecked")
  protected Operator select(Operator in, AST node, Compiler compiler) throws QueryException {
    int pos = 0;
    Expr expr = compiler.anyExpr(node.getChild(pos++));
    Select select = new Select(in, expr);
    addChecks(select, (List<QNm>) node.getProperty("check"), compiler);
    return anyOp(select, node.getLastChild(), compiler);
  }

  @SuppressWarnings("unchecked")
  protected Operator orderBy(Operator in, AST node, Compiler compiler) throws QueryException {
    int orderBySpecCount = node.getChildCount() - 1;
    Expr[] orderByExprs = new Expr[orderBySpecCount];
    Ordering.OrderModifier[] orderBySpec = new Ordering.OrderModifier[orderBySpecCount];
    for (int i = 0; i < orderBySpecCount; i++) {
      AST orderBy = node.getChild(i);
      orderByExprs[i] = compiler.expr(orderBy.getChild(0), true);
      orderBySpec[i] = compiler.orderModifier(orderBy);
    }
    OrderBy orderBy = new OrderBy(in, orderByExprs, orderBySpec);
    addChecks(orderBy, (List<QNm>) node.getProperty("check"), compiler);
    return anyOp(orderBy, node.getLastChild(), compiler);
  }

  protected void addChecks(Check op, List<QNm> check, Compiler compiler) throws QueryException {
    if (check != null) {
      for (QNm checkVar : check) {
        compiler.table.resolve(checkVar, op.check());
      }
    }
  }
}
