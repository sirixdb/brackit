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
import io.brackit.query.block.Block;
import io.brackit.query.block.BlockChain;
// Vectorized execution delegated to SequentialPipelineStrategy.tryVectorizedExpr()
import io.brackit.query.block.MorselBlock;
import io.brackit.query.block.Count;
import io.brackit.query.block.ForBind;
import io.brackit.query.block.GroupBy;
import io.brackit.query.block.LetBind;
import io.brackit.query.block.NLJoin;
import io.brackit.query.block.OrderBy;
import io.brackit.query.block.Select;
import io.brackit.query.block.TableJoin;
import io.brackit.query.compiler.AST;
import io.brackit.query.compiler.XQ;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.expr.BlockExpr;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.operator.Check;
import io.brackit.query.util.Cmp;
import io.brackit.query.util.aggregator.Aggregate;
import io.brackit.query.util.sort.Ordering;

/**
 * Block-based (parallel) pipeline strategy for compiling PipeExpr nodes.
 * Extracted from BlockTranslator.
 */
public class BlockPipelineStrategy implements PipelineStrategy {

  private boolean ordered = true;
  private boolean morselParallel = false;

  public BlockPipelineStrategy() {
  }

  public BlockPipelineStrategy(boolean ordered) {
    this.ordered = ordered;
  }

  public void setOrdered(boolean ordered) {
    this.ordered = ordered;
  }

  public void setMorselParallel(boolean morselParallel) {
    this.morselParallel = morselParallel;
  }

  @Override
  public Expr compilePipeExpr(AST node, Compiler compiler) throws QueryException {
    // Vectorized substitution (shared logic); VARIABLE sources decide per evaluation with the
    // generic block pipeline as the runtime fallback — see RuntimeSourceGatedExpr.
    Expr vectorized = SequentialPipelineStrategy.tryVectorizedExpr(node,
                                                                   false,
                                                                   () -> compileGenericPipeExpr(node, compiler));
    if (vectorized != null)
      return vectorized;
    return compileGenericPipeExpr(node, compiler);
  }

  /** The generic block pipeline compilation — the always-correct path/fallback. */
  protected Expr compileGenericPipeExpr(AST node, Compiler compiler) throws QueryException {
    int initialBindSize = compiler.table.bound().length;

    // Collect blocks
    List<Block> blocks = new ArrayList<>();
    collectBlocks(blocks, node.getChild(0), compiler);

    // Find the return expression at the right-most leaf
    AST returnExpr = node.getChild(0);
    while (returnExpr.getType() != XQ.End) {
      returnExpr = returnExpr.getLastChild();
    }
    Expr expr = compiler.anyExpr(returnExpr.getChild(0));

    // Clear operator bindings
    int unbind = compiler.table.bound().length - initialBindSize;
    for (int i = 0; i < unbind; i++) {
      compiler.table.unbind();
    }

    Block chain = blocks.size() == 1 ? blocks.get(0) : new BlockChain(blocks);
    return new BlockExpr(chain, expr, ordered);
  }

  protected void collectBlocks(List<Block> blocks, AST node, Compiler compiler) throws QueryException {
    switch (node.getType()) {
      case XQ.Start -> {
        if (node.getChildCount() > 0) {
          collectBlocks(blocks, node.getLastChild(), compiler);
        }
      }
      case XQ.End -> {
        // End node, nothing to add
      }
      case XQ.ForBind -> {
        blocks.add(forBindBlock(node, compiler));
        if (morselParallel) {
          // Insert morsel parallelism boundary after the scan (ForBind).
          // Dispatches morsel-sized batches to ForkJoinPool workers.
          blocks.add(new MorselBlock());
        }
        collectBlocks(blocks, node.getLastChild(), compiler);
      }
      case XQ.LetBind -> {
        blocks.add(letBindBlock(node, compiler));
        collectBlocks(blocks, node.getLastChild(), compiler);
      }
      case XQ.Selection -> {
        blocks.add(selectBlock(node, compiler));
        collectBlocks(blocks, node.getLastChild(), compiler);
      }
      case XQ.OrderBy -> {
        blocks.add(orderByBlock(node, compiler));
        collectBlocks(blocks, node.getLastChild(), compiler);
      }
      case XQ.GroupBy -> {
        blocks.add(groupByBlock(node, compiler));
        collectBlocks(blocks, node.getLastChild(), compiler);
      }
      case XQ.Count -> {
        blocks.add(countBlock(node, compiler));
        collectBlocks(blocks, node.getLastChild(), compiler);
      }
      case XQ.Join -> {
        blocks.add(joinBlock(node, compiler));
        collectBlocks(blocks, node.getLastChild(), compiler);
      }
      default -> throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                                          "Unexpected AST operator node '%s' of type: %s",
                                          node,
                                          node.getType());
    }
  }

  @SuppressWarnings("unchecked")
  protected Block forBindBlock(AST node, Compiler compiler) throws QueryException {
    int pos = 0;
    AST runVarDecl = node.getChild(pos++);
    QNm runVarName = (QNm) runVarDecl.getChild(0).getValue();
    SequenceType runVarType = SequenceType.ITEM_SEQUENCE;
    if (runVarDecl.getChildCount() == 2) {
      runVarType = compiler.sequenceType(runVarDecl.getChild(1));
    }
    AST posBindingOrSourceExpr = node.getChild(pos++);
    // 'allowing empty' marker child (see Compiler#forClause) or the lifted property form.
    boolean allowingEmptyChild = false;
    if (posBindingOrSourceExpr.getType() == XQ.AllowingEmpty) {
      allowingEmptyChild = true;
      posBindingOrSourceExpr = node.getChild(pos++);
    }
    QNm posVarName = null;
    if (posBindingOrSourceExpr.getType() == XQ.TypedVariableBinding) {
      posVarName = (QNm) posBindingOrSourceExpr.getChild(0).getValue();
      posBindingOrSourceExpr = node.getChild(pos++);
    }
    Expr sourceExpr = compiler.expr(posBindingOrSourceExpr, true);

    compiler.table.bind(runVarName, runVarType);
    compiler.table.resolve(runVarName);

    // Check if allowingEmpty is set
    boolean allowingEmpty = allowingEmptyChild || node.checkProperty("allowingEmpty");
    ForBind forBind = new ForBind(sourceExpr, allowingEmpty);

    if (posVarName != null) {
      Binding posBinding = compiler.table.bind(posVarName, SequenceType.INTEGER);
      compiler.table.resolve(posVarName);
      forBind.bindPosition(posBinding.isReferenced());
    }

    return forBind;
  }

  @SuppressWarnings("unchecked")
  protected Block letBindBlock(AST node, Compiler compiler) throws QueryException {
    int pos = 0;
    AST letVarDecl = node.getChild(pos++);
    QNm letVarName = (QNm) letVarDecl.getChild(0).getValue();
    SequenceType letVarType = SequenceType.ITEM_SEQUENCE;
    if (letVarDecl.getChildCount() == 2) {
      letVarType = compiler.sequenceType(letVarDecl.getChild(1));
    }
    Expr sourceExpr = compiler.expr(node.getChild(pos++), true);
    compiler.table.bind(letVarName, letVarType);
    compiler.table.resolve(letVarName);

    return new LetBind(sourceExpr);
  }

  @SuppressWarnings("unchecked")
  protected Block selectBlock(AST node, Compiler compiler) throws QueryException {
    Expr predExpr = compiler.anyExpr(node.getChild(0));

    // Handle check for dead tuple semantics
    List<QNm> checkVars = (List<QNm>) node.getProperty("check");
    Check check = null;
    if (checkVars != null && !checkVars.isEmpty()) {
      check = new Check();
      for (QNm checkVar : checkVars) {
        compiler.table.resolve(checkVar, check.check());
      }
    }

    return new Select(predExpr, check);
  }

  @SuppressWarnings("unchecked")
  protected Block orderByBlock(AST node, Compiler compiler) throws QueryException {
    int orderBySpecCount = 0;
    for (int i = 0; i < node.getChildCount() - 1; i++) {
      AST child = node.getChild(i);
      if (child.getType() == XQ.OrderBySpec) {
        orderBySpecCount++;
      }
    }

    Expr[] orderByExprs = new Expr[orderBySpecCount];
    Ordering.OrderModifier[] orderBySpec = new Ordering.OrderModifier[orderBySpecCount];

    for (int i = 0; i < orderBySpecCount; i++) {
      AST orderBy = node.getChild(i);
      orderByExprs[i] = compiler.anyExpr(orderBy.getChild(0));
      orderBySpec[i] = compiler.orderModifier(orderBy);
    }

    return new OrderBy(orderByExprs, orderBySpec);
  }

  @SuppressWarnings("unchecked")
  protected Block groupByBlock(AST node, Compiler compiler) throws QueryException {
    int pos = 0;
    while (node.getChild(pos).getType() == XQ.GroupBySpec) {
      pos++;
    }
    int grpSpecCnt = pos;

    // Collect additional aggregate bindings
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
      addAggs[i] = bnds.get(i).agg;
    }

    boolean sequential = node.checkProperty("sequential");
    GroupBy groupBy = new GroupBy(dftAgg, addAggs, grpSpecCnt, sequential);

    // Resolve positions for grouping variables
    for (int i = 0; i < grpSpecCnt; i++) {
      QNm grpVarName = (QNm) node.getChild(i).getChild(0).getValue();
      compiler.table.resolve(grpVarName, groupBy.group(i));
    }

    // Resolve positions for additional aggregates
    for (int i = 0; i < bnds.size(); i++) {
      Compiler.AggregateBinding bnd = bnds.get(i);
      compiler.table.resolve(bnd.srcVar, groupBy.aggregate(i));
    }

    // Bind additional aggregates
    for (Compiler.AggregateBinding bnd : bnds) {
      compiler.table.bind(bnd.aggVar, bnd.aggVarType);
      compiler.table.resolve(bnd.aggVar);
    }

    return groupBy;
  }

  @SuppressWarnings("unchecked")
  protected Block countBlock(AST node, Compiler compiler) throws QueryException {
    int pos = 0;
    AST posVarDecl = node.getChild(pos++);
    QNm posVarName = (QNm) posVarDecl.getChild(0).getValue();
    SequenceType posVarType = SequenceType.ITEM_SEQUENCE;
    if (posVarDecl.getChildCount() == 2) {
      posVarType = compiler.sequenceType(posVarDecl.getChild(1));
    }
    compiler.table.bind(posVarName, posVarType);
    compiler.table.resolve(posVarName);

    // Handle check for group boundary detection
    List<QNm> checkVars = (List<QNm>) node.getProperty("check");
    Check check = null;
    if (checkVars != null && !checkVars.isEmpty()) {
      check = new Check();
      for (QNm checkVar : checkVars) {
        compiler.table.resolve(checkVar, check.check());
      }
    }

    return new Count(check);
  }

  @SuppressWarnings("unchecked")
  protected Block joinBlock(AST node, Compiler compiler) throws QueryException {
    // Get join type
    Cmp cmp = (Cmp) node.getProperty("cmp");
    boolean isGcmp = node.checkProperty("GCmp");

    // Compile left (outer) join branch
    List<Block> leftBlocks = new ArrayList<>();
    collectBlocks(leftBlocks, node.getChild(0).getChild(0), compiler);
    AST tmp = node.getChild(0);
    while (tmp.getType() != XQ.End) {
      tmp = tmp.getLastChild();
    }
    Expr leftExpr = compiler.anyExpr(tmp.getChild(0));

    // Compile right (inner) join branch
    List<Block> rightBlocks = new ArrayList<>();
    collectBlocks(rightBlocks, node.getChild(1), compiler);
    tmp = node.getChild(1);
    while (tmp.getType() != XQ.End) {
      tmp = tmp.getLastChild();
    }
    Expr rightExpr = compiler.anyExpr(tmp.getChild(0));

    boolean leftJoin = node.checkProperty("leftJoin");
    boolean skipSort = node.checkProperty("skipSort");

    Block leftBlock = leftBlocks.isEmpty()
        ? null
        : (leftBlocks.size() == 1 ? leftBlocks.get(0) : new BlockChain(leftBlocks));
    Block rightBlock = rightBlocks.isEmpty()
        ? null
        : (rightBlocks.size() == 1 ? rightBlocks.get(0) : new BlockChain(rightBlocks));

    // Determine if we should use TableJoin (hash join) or NLJoin (nested loop)
    // For now, use TableJoin as the default when both branches exist
    if (leftBlock != null && rightBlock != null) {
      // Compile post-join operations if any
      Block postBlock = null;
      AST post = node.getChild(2).getChild(0);
      if (post.getType() != XQ.End) {
        List<Block> postBlocks = new ArrayList<>();
        collectBlocks(postBlocks, post, compiler);
        if (!postBlocks.isEmpty()) {
          postBlock = postBlocks.size() == 1 ? postBlocks.get(0) : new BlockChain(postBlocks);
        }
      }

      TableJoin join = new TableJoin(cmp,
                                     isGcmp,
                                     leftJoin,
                                     skipSort,
                                     leftBlock,
                                     leftExpr,
                                     rightBlock,
                                     rightExpr,
                                     postBlock);

      QNm prop = (QNm) node.getProperty("group");
      if (prop != null) {
        compiler.table.resolve(prop, join.group());
      }

      return join;
    } else if (leftBlock != null) {
      // Only left branch - use NLJoin with empty right
      return new NLJoin(leftBlock,
                        rightBlock != null ? rightBlock : new BlockChain(new Block[0]),
                        leftExpr,
                        rightExpr,
                        cmp,
                        isGcmp,
                        leftJoin);
    }

    throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR, "Invalid join configuration");
  }
}
