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
package org.brackit.xquery.compiler.translator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.expr.PipeExpr;
import org.brackit.xquery.operator.Check;
import org.brackit.xquery.operator.Count;
import org.brackit.xquery.operator.ForBind;
import org.brackit.xquery.operator.GroupBy;
import org.brackit.xquery.operator.LetBind;
import org.brackit.xquery.operator.NLJoin;
import org.brackit.xquery.operator.Operator;
import org.brackit.xquery.operator.OrderBy;
import org.brackit.xquery.operator.Print;
import org.brackit.xquery.operator.Select;
import org.brackit.xquery.operator.Start;
import org.brackit.xquery.operator.TableJoin;
import org.brackit.xquery.util.Cmp;
import org.brackit.xquery.util.aggregator.Aggregate;
import org.brackit.xquery.util.sort.Ordering.OrderModifier;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.node.Node;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * Extended compiler for bottom-up compilation of unnested flwor expressions
 *
 * @author Sebastian Baechle
 */
public class TopDownTranslator extends Compiler {

  public TopDownTranslator(Map<QNm, Str> options) {
    super(options);
  }

  @Override
  protected Expr anyExpr(AST node) throws QueryException {
    if (node.getType() == XQ.PipeExpr) {
      // switch to bottom up compilation
      return pipeExpr(node);
    }
    return super.anyExpr(node);
  }

  protected Expr pipeExpr(AST node) throws QueryException {
    int initialBindSize = table.bound().length;
    Operator root = anyOp(null, node.getChild(0));

    // for simpler scoping, the return expression is
    // at the right-most leaf
    AST returnExpr = node.getChild(0);
    while (returnExpr.getType() != XQ.End) {
      returnExpr = returnExpr.getLastChild();
    }
    Expr expr = anyExpr(returnExpr.getChild(0));

    // clear operator bindings
    int unbind = table.bound().length - initialBindSize;
    for (int i = 0; i < unbind; i++) {
      table.unbind();
    }

    return new PipeExpr(root, expr);
  }

  protected Operator anyOp(Operator in, AST node) throws QueryException {
    return _anyOp(in, node);
    // return new Print(_anyOp(in, node));
  }

  protected Operator _anyOp(Operator in, AST node) throws QueryException {
    switch (node.getType()) {
      case XQ.Start:
        if (node.getChildCount() == 0) {
          return new Start();
        } else {
          return anyOp(new Start(), node.getLastChild());
        }
      case XQ.End:
        return in;
      case XQ.ForBind:
        return forBind(in, node);
      case XQ.LetBind:
        return letBind(in, node);
      case XQ.Selection:
        return select(in, node);
      case XQ.OrderBy:
        return orderBy(in, node);
      case XQ.GroupBy:
        return groupBy(in, node);
      case XQ.Count:
        return count(in, node);
      case XQ.Join:
        return join(in, node);
      default:
        throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                                 "Unexpected AST operator node '%s' of type: %s",
                                 node,
                                 node.getType());
    }
  }

  @SuppressWarnings("unchecked")
  protected Operator groupBy(Operator in, AST node) throws QueryException {
    int pos = 0;
    while (node.getChild(pos).getType() == XQ.GroupBySpec) {
      pos++;
    }
    int grpSpecCnt = pos;
    // collect additional aggregate bindings
    List<AggregateBinding> bnds = new ArrayList<>();
    while (node.getChild(pos).getType() == XQ.AggregateSpec) {
      AST aggSpec = node.getChild(pos);
      QNm var = (QNm) aggSpec.getChild(0).getValue();
      for (int j = 1; j < aggSpec.getChildCount(); j++) {
        AST aggBinding = aggSpec.getChild(j);
        AST typedVarBnd = aggBinding.getChild(0);
        Aggregate agg = aggregate(aggBinding.getChild(1));
        QNm aggVar = (QNm) typedVarBnd.getChild(0).getValue();
        SequenceType aggType = SequenceType.ITEM_SEQUENCE;
        if (typedVarBnd.getChildCount() == 2) {
          aggType = sequenceType(typedVarBnd.getChild(1));
        }
        bnds.add(new AggregateBinding(var, aggVar, aggType, agg));
      }
      pos++;
    }
    Aggregate dftAgg = aggregate(node.getChild(pos).getChild(0));
    Aggregate[] addAggs = new Aggregate[bnds.size()];
    for (int i = 0; i < bnds.size(); i++) {
      AggregateBinding bnd = bnds.get(i);
      addAggs[i] = bnd.agg;
    }
    boolean sequential = node.checkProperty("sequential");
    GroupBy groupBy = new GroupBy(in, dftAgg, addAggs, grpSpecCnt, sequential);
    // resolve positions grouping variables
    for (int i = 0; i < grpSpecCnt; i++) {
      QNm grpVarName = (QNm) node.getChild(i).getChild(0).getValue();
      table.resolve(grpVarName, groupBy.group(i));
    }
    // resolve positions for additional aggregates
    for (int i = 0; i < bnds.size(); i++) {
      AggregateBinding bnd = bnds.get(i);
      table.resolve(bnd.srcVar, groupBy.aggregate(i));
    }
    // bind additional aggregates
    for (AggregateBinding bnd : bnds) {
      table.bind(bnd.aggVar, bnd.aggVarType);
      // fake binding
      table.resolve(bnd.aggVar);
    }
    addChecks(groupBy, (List<QNm>) node.getProperty("check"));
    return anyOp(groupBy, node.getLastChild());
  }

  @SuppressWarnings("unchecked")
  protected Operator join(Operator in, AST node) throws QueryException {
    // get join type
    Cmp cmp = (Cmp) node.getProperty("cmp");
    boolean isGcmp = node.checkProperty("GCmp");

    // compile left (outer) join branch (skip initial start)
    Operator leftIn = anyOp(in, node.getChild(0).getChild(0));
    AST tmp = node.getChild(0);
    while (tmp.getType() != XQ.End) {
      tmp = tmp.getLastChild();
    }
    Expr leftExpr = anyExpr(tmp.getChild(0));

    // compile right (inner) join branch
    Operator rightIn = anyOp(new Start(), node.getChild(1));
    tmp = node.getChild(1);
    while (tmp.getType() != XQ.End) {
      tmp = tmp.getLastChild();
    }
    Expr rightExpr = anyExpr(tmp.getChild(0));

    boolean leftJoin = node.checkProperty("leftJoin");
    boolean skipSort = node.checkProperty("skipSort");
    TableJoin join = new TableJoin(cmp, isGcmp, leftJoin, skipSort, leftIn, leftExpr, rightIn, rightExpr);

    QNm prop = (QNm) node.getProperty("group");
    if (prop != null) {
      table.resolve(prop, join.group());
    }
    addChecks(join, (List<QNm>) node.getProperty("check"));

    Operator op = join;
    AST post = node.getChild(2).getChild(0);
    if ((post.getType() != XQ.End)) {
      op = anyOp(join, post);
    }

    return anyOp(op, node.getLastChild());
  }

  protected Operator nljoin(Operator in, AST node) throws QueryException {
    // compile left (outer) join branch (skip initial start)
    Operator leftIn = anyOp(in, node.getChild(0).getChild(0));

    // get join type
    Cmp cmp = (Cmp) node.getProperty("cmp");
    boolean isGcmp = node.checkProperty("GCmp");

    AST tmp = node.getChild(0);
    while (tmp.getType() != XQ.End) {
      tmp = tmp.getLastChild();
    }
    Expr leftExpr = anyExpr(tmp.getChild(0));

    // compile right (inner) join branch
    Operator rightIn = anyOp(new Start(), node.getChild(1));
    tmp = node.getChild(1);
    while (tmp.getType() != XQ.End) {
      tmp = tmp.getLastChild();
    }
    Expr rightExpr = anyExpr(tmp.getChild(0));

    boolean leftJoin = node.checkProperty("leftJoin");
    boolean skipSort = node.checkProperty("skipSort");
    Operator join = new NLJoin(leftIn, rightIn, leftExpr, rightExpr, cmp, isGcmp, leftJoin);

    Operator op = join;
    // if (node.getParent().getParent().getType() == XQ.Join) {
    // return new Print(anyOp(op, node.getLastChild()), System.out);
    // }
    //
    return anyOp(op, node.getLastChild());
  }

  @SuppressWarnings("unchecked")
  protected Operator forBind(Operator in, AST node) throws QueryException {
    int pos = 0;
    AST runVarDecl = node.getChild(pos++);
    QNm runVarName = (QNm) runVarDecl.getChild(0).getValue();
    SequenceType runVarType = SequenceType.ITEM_SEQUENCE;
    if (runVarDecl.getChildCount() == 2) {
      runVarType = sequenceType(runVarDecl.getChild(1));
    }
    AST posBindingOrSourceExpr = node.getChild(pos++);
    QNm posVarName = null;
    if (posBindingOrSourceExpr.getType() == XQ.TypedVariableBinding) {
      posVarName = (QNm) posBindingOrSourceExpr.getChild(0).getValue();
      posBindingOrSourceExpr = node.getChild(pos++);
    }
    Expr sourceExpr = expr(posBindingOrSourceExpr, true);

    Binding posBinding = null;
    table.bind(runVarName, runVarType);
    // Fake binding of run variable because set-oriented processing requires
    // the variable anyway
    table.resolve(runVarName);

    if (posVarName != null) {
      posBinding = table.bind(posVarName, SequenceType.INTEGER);
      // Fake binding of pos variable to simplify compilation.
      table.resolve(posVarName);
      // TODO Optimize and do not bind variable if not necessary
    }
    ForBind forBind = new ForBind(in, sourceExpr, false);
    if (posBinding != null) {
      forBind.bindPosition(posBinding.isReferenced());
    }
    addChecks(forBind, (List<QNm>) node.getProperty("check"));
    return anyOp(forBind, node.getLastChild());
  }

  @SuppressWarnings("unchecked")
  protected Operator letBind(Operator in, AST node) throws QueryException {
    int pos = 0;
    AST letVarDecl = node.getChild(pos++);
    QNm letVarName = (QNm) letVarDecl.getChild(0).getValue();
    SequenceType letVarType = SequenceType.ITEM_SEQUENCE;
    if (letVarDecl.getChildCount() == 2) {
      letVarType = sequenceType(letVarDecl.getChild(1));
    }
    Expr sourceExpr = expr(node.getChild(pos++), true);
    table.bind(letVarName, letVarType);

    // Fake binding of let variable because set-oriented processing requires
    // the variable anyway
    table.resolve(letVarName);
    LetBind letBind = new LetBind(in, sourceExpr);
    addChecks(letBind, (List<QNm>) node.getProperty("check"));
    return anyOp(letBind, node.getLastChild());
  }

  @SuppressWarnings("unchecked")
  protected Operator count(Operator in, AST node) throws QueryException {
    int pos = 0;
    AST posVarDecl = node.getChild(pos++);
    QNm posVarName = (QNm) posVarDecl.getChild(0).getValue();
    SequenceType posVarType = SequenceType.ITEM_SEQUENCE;
    if (posVarDecl.getChildCount() == 2) {
      posVarType = sequenceType(posVarDecl.getChild(1));
    }
    Binding binding = table.bind(posVarName, posVarType);

    // Fake binding of let variable because set-oriented processing requires
    // the variable anyway
    table.resolve(posVarName);
    Count count = new Count(in);
    addChecks(count, (List<QNm>) node.getProperty("check"));
    return anyOp(count, node.getLastChild());
  }

  @SuppressWarnings("unchecked")
  protected Operator select(Operator in, AST node) throws QueryException {
    int pos = 0;
    Expr expr = anyExpr(node.getChild(pos++));
    Select select = new Select(in, expr);
    addChecks(select, (List<QNm>) node.getProperty("check"));
    return anyOp(select, node.getLastChild());
  }

  @SuppressWarnings("unchecked")
  protected Operator orderBy(Operator in, AST node) throws QueryException {
    int orderBySpecCount = node.getChildCount() - 1;
    Expr[] orderByExprs = new Expr[orderBySpecCount];
    OrderModifier[] orderBySpec = new OrderModifier[orderBySpecCount];
    for (int i = 0; i < orderBySpecCount; i++) {
      AST orderBy = node.getChild(i);
      orderByExprs[i] = expr(orderBy.getChild(0), true);
      orderBySpec[i] = orderModifier(orderBy);
    }
    OrderBy orderBy = new OrderBy(in, orderByExprs, orderBySpec);
    addChecks(orderBy, (List<QNm>) node.getProperty("check"));
    return anyOp(orderBy, node.getLastChild());
  }

  protected void addChecks(Check op, List<QNm> check) throws QueryException {
    if (check != null) {
      for (QNm checkVar : check) {
        table.resolve(checkVar, op.check());
      }
    }
  }

  protected Operator wrapDebugOutput(Operator root) {
    return new Print(root, System.out) {
      @Override
      public String asString(QueryContext ctx, Sequence sequence) throws QueryException {
        if (sequence == null) {
          return "";
        }
        if (sequence instanceof Item) {
          return (sequence instanceof Node<?>) ? nodeAsString(ctx, (Node<?>) sequence) : sequence.toString();
        }
        StringBuilder s = new StringBuilder("(");
        Iter it = sequence.iterate();
        try {
          for (Item item = it.next(); item != null; item = it.next()) {
            s.append((sequence instanceof Node<?>) ? nodeAsString(ctx, (Node<?>) sequence) : sequence.toString());
            s.append(", ");
          }
        } finally {
          s.append(")");
          it.close();
        }
        return s.toString();
      }

      private String nodeAsString(QueryContext ctx, Node<?> node) {
        try {
          switch (node.getKind()) {
            case ELEMENT:
              return "<" + node.getName() + ">";
            case ATTRIBUTE:
              return node.getName() + "='" + node.getValue() + "'";
            case DOCUMENT:
              return "doc(" + node.getCollection().getName() + ")";
            default:
              return node.getValue().stringValue();
          }
        } catch (DocumentException e) {
          e.printStackTrace();
          return "";
        }
      }
    };
  }
}
