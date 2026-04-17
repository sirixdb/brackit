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
import io.brackit.query.compiler.optimizer.VectorizedExecutor;
import io.brackit.query.compiler.optimizer.VectorizedScanAnnotation;
import io.brackit.query.expr.PipeExpr;
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
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.type.SequenceType;

/**
 * Sequential (top-down) pipeline strategy for compiling PipeExpr nodes.
 * Extracted from TopDownTranslator.
 */
public class SequentialPipelineStrategy implements PipelineStrategy {

  /** Pluggable vectorized executor — set by bjq or SirixDB at startup. */
  private static volatile VectorizedExecutor vectorizedExecutor;

  /** Register a vectorized executor for automatic optimization. */
  public static void setVectorizedExecutor(VectorizedExecutor executor) {
    vectorizedExecutor = executor;
  }

  /** Get the registered vectorized executor (used by BlockPipelineStrategy too). */
  public static VectorizedExecutor getVectorizedExecutor() {
    return vectorizedExecutor;
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
  public static Expr tryVectorizedExpr(AST node) {
    return tryVectorizedExpr(node, false);
  }

  public static Expr tryVectorizedExpr(AST node, boolean countWrapped) {
    VectorizedExecutor executor = vectorizedExecutor;
    if (executor == null)
      return null;

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
        return VectorizedGroupByExpr.countDistinct(executor, field);
      }
    }

    // Pattern 1: Group-by (with optional filter)
    if (Boolean.TRUE.equals(node.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY))) {
      String groupField = (String) node.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD);
      if (groupField == null)
        return null;

      String filterField = (String) node.getProperty(VectorizedScanAnnotation.FILTER_FIELD);
      String filterOp = (String) node.getProperty(VectorizedScanAnnotation.FILTER_OP);
      Long filterValue = (Long) node.getProperty(VectorizedScanAnnotation.FILTER_VALUE);

      if (filterField != null && filterOp != null && filterValue != null) {
        return new VectorizedGroupByExpr(executor, groupField, filterField, filterOp, filterValue);
      }
      return new VectorizedGroupByExpr(executor, groupField);
    }

    // Pattern 2: Filtered count — only when wrapped in count() function
    // The executor returns a scalar Int64(count), which replaces the entire
    // count(PipeExpr) expression.
    if (countWrapped && Boolean.TRUE.equals(node.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT))) {
      String filterField = (String) node.getProperty(VectorizedScanAnnotation.FILTER_FIELD);
      String filterOp = (String) node.getProperty(VectorizedScanAnnotation.FILTER_OP);
      Long filterValue = (Long) node.getProperty(VectorizedScanAnnotation.FILTER_VALUE);

      if (filterField != null && filterOp != null && filterValue != null) {
        // Two-predicate count: when the walker extracted FILTER2 alongside FILTER, and
        // both carry numeric (long) values, dispatch to the fused
        // executeFilterCount2 method so the executor can run one SIMD pass with a
        // range mask instead of Brackit post-filtering the first predicate's match set.
        String filter2Field = (String) node.getProperty(VectorizedScanAnnotation.FILTER2_FIELD);
        String filter2Op = (String) node.getProperty(VectorizedScanAnnotation.FILTER2_OP);
        Long filter2Value = (Long) node.getProperty(VectorizedScanAnnotation.FILTER2_VALUE);
        // Only dispatch fused 2-predicate when both filters target the SAME field
        // (i.e. a range predicate like age > 30 AND age < 50). Cross-field ANDs
        // (e.g. age > 40 AND active) aren't fuseable by executeFilterCount2
        // today — those fall through to single-pred and let the generic
        // pipeline handle the second clause.
        if (filter2Field != null && filter2Field.equals(filterField) && filter2Op != null && filter2Value != null) {
          return VectorizedGroupByExpr.filterCount2(executor,
                                                    filterField,
                                                    filterOp,
                                                    filterValue,
                                                    filter2Field,
                                                    filter2Op,
                                                    filter2Value);
        }
        return new VectorizedGroupByExpr(executor, filterField, filterOp, filterValue);
      }
    }

    // Pattern 3: Sorted scan
    if (Boolean.TRUE.equals(node.getProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY))) {
      String orderField = (String) node.getProperty(VectorizedScanAnnotation.ORDER_FIELD);
      String direction = (String) node.getProperty(VectorizedScanAnnotation.ORDER_DIRECTION);
      if (orderField != null) {
        return VectorizedGroupByExpr.sorted(executor, orderField, direction);
      }
    }

    // Pattern 4: Pure aggregate (sum/avg/min/max/count over flat scan, no group-by)
    if (Boolean.TRUE.equals(node.getProperty(VectorizedScanAnnotation.VECTORIZED_AGGREGATE))) {
      String func = (String) node.getProperty(VectorizedScanAnnotation.AGGREGATE_FUNC);
      String field = (String) node.getProperty(VectorizedScanAnnotation.AGGREGATE_FIELD);
      if (func != null) {
        return VectorizedGroupByExpr.aggregate(executor, func, field);
      }
    }

    return null;
  }

  @Override
  public Expr compilePipeExpr(AST node, Compiler compiler) throws QueryException {
    // Check for vectorized scan annotations from the optimizer
    Expr vectorized = tryVectorizedExpr(node);
    if (vectorized != null)
      return vectorized;

    int initialBindSize = compiler.table.bound().length;
    Operator root = anyOp(null, node.getChild(0), compiler);

    // for simpler scoping, the return expression is
    // at the right-most leaf
    AST returnExpr = node.getChild(0);
    while (returnExpr.getType() != XQ.End) {
      returnExpr = returnExpr.getLastChild();
    }
    Expr expr = compiler.anyExpr(returnExpr.getChild(0));

    // clear operator bindings
    int unbind = compiler.table.bound().length - initialBindSize;
    for (int i = 0; i < unbind; i++) {
      compiler.table.unbind();
    }

    return new PipeExpr(root, expr);
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
    ForBind forBind = new ForBind(in, sourceExpr, false);
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
