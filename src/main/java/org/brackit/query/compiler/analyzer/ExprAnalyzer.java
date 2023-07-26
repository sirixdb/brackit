/**
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 * <p>
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
 * <p>
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
package org.brackit.query.compiler.analyzer;

import org.brackit.query.ErrorCode;
import org.brackit.query.QueryException;
import org.brackit.query.Query;
import org.brackit.query.atomic.AnyURI;
import org.brackit.query.atomic.Bool;
import org.brackit.query.atomic.QNm;
import org.brackit.query.atomic.Str;
import org.brackit.query.compiler.AST;
import org.brackit.query.compiler.Bits;
import org.brackit.query.compiler.XQ;
import org.brackit.query.expr.Variable;
import org.brackit.query.function.UDF;
import org.brackit.query.function.json.JSONFun;
import org.brackit.query.module.Functions;
import org.brackit.query.module.Module;
import org.brackit.query.module.Namespaces;
import org.brackit.query.module.StaticContext;
import org.brackit.query.util.Whitespace;
import org.brackit.query.jdm.Function;
import org.brackit.query.jdm.Kind;
import org.brackit.query.jdm.Signature;
import org.brackit.query.jdm.type.*;

/**
 * @author Sebastian Baechle
 */
public class ExprAnalyzer extends AbstractAnalyzer {
  protected final VarScopes variables;
  protected final Module module;

  public ExprAnalyzer(Module module) {
    this.variables = new VarScopes();
    this.module = module;
    this.sctx = module.getStaticContext();
  }

  public Module getModule() {
    return module;
  }

  void functionBody(AST body) throws QueryException {
    enclosedExpr(body);
  }

  protected boolean queryBody(AST body) throws QueryException {
    if (body.getType() != XQ.QueryBody) {
      return false;
    }
    expr(body.getChild(0));
    return true;
  }

  boolean expr(AST expr) throws QueryException {
    if (expr.getType() != XQ.SequenceExpr) {
      exprSingle(expr);
    } else {
      for (int i = 0; i < expr.getChildCount(); i++) {
        exprSingle(expr.getChild(i));
      }
    }
    return true;
  }

  boolean exprSingle(AST expr) throws QueryException {
    return flowrExpr(expr) || quantifiedExpr(expr) || switchExpr(expr) || typeswitchExpr(expr) || ifExpr(expr)
        || tryCatchExpr(expr) ||
        /* Begin XQuery Update Facility 1.0 */
        insertExpr(expr) || deleteExpr(expr) || renameExpr(expr) || replaceExpr(expr) || transformExpr(expr) ||
        /* End XQuery Update Facility 1.0 */
        /* Begin JSONiq Update Facility */
        insertJsonExpr(expr) || deleteJsonExpr(expr) || renameJsonExpr(expr) || replaceJsonExpr(expr) || appendJsonExpr(
                                                                                                                        expr)
        ||
        /* End JSONiq Update Facility */
        orExpr(expr);
  }

  // Begin XQuery Update Facility 1.0
  protected boolean insertExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.InsertExpr) {
      return false;
    }
    AST src = expr.getChild(1);
    exprSingle(src);
    AST target = expr.getChild(2);
    exprSingle(target);
    return true;
  }

  protected boolean deleteExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.DeleteExpr) {
      return false;
    }
    AST target = expr.getChild(0);
    exprSingle(target);
    return true;
  }

  protected boolean renameExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.RenameExpr) {
      return false;
    }
    AST target = expr.getChild(0);
    exprSingle(target);
    AST newName = expr.getChild(1);
    exprSingle(newName);
    return true;
  }

  protected boolean replaceExpr(AST expr) throws QueryException {
    if (expr.getType() == XQ.ReplaceValueExpr) {
      AST target = expr.getChild(0);
      exprSingle(target);
      AST replacement = expr.getChild(1);
      exprSingle(replacement);
    } else if (expr.getType() == XQ.ReplaceNodeExpr) {
      AST target = expr.getChild(0);
      exprSingle(target);
      AST replacement = expr.getChild(1);
      exprSingle(replacement);
    } else {
      return false;
    }
    return true;
  }

  protected boolean transformExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.TransformExpr) {
      return false;
    }
    int scopeCount = scopeCount();
    int pos = 0;
    while (pos < expr.getChildCount() - 2) {
      openScope();
      AST binding = expr.getChild(pos++);
      QNm name = (QNm) binding.getChild(0).getValue();
      // expand, bind and update AST
      name = expand(name, DefaultNS.EMPTY);
      name = bind(name);
      binding.getChild(0).setValue(name);
      exprSingle(binding.getChild(1));
      offerScope();
    }
    AST modify = expr.getChild(pos++);
    exprSingle(modify);
    AST ret = expr.getChild(pos);
    exprSingle(ret);
    for (int i = scopeCount(); i > scopeCount; i--) {
      closeScope();
    }
    return true;
  }

  // End XQuery Update Facility 1.0

  // Begin JSONiq Update Facility
  private boolean appendJsonExpr(AST expr) {
    if (expr.getType() != XQ.AppendJsonExpr) {
      return false;
    }
    AST src = expr.getChild(0);
    exprSingle(src);
    AST target = expr.getChild(1);
    exprSingle(target);
    return true;
  }

  private boolean replaceJsonExpr(AST expr) {
    if (expr.getType() != XQ.ReplaceJsonExpr) {
      return false;
    }
    AST target = expr.getChild(0);
    postFixExpr(target);
    AST newExpr = expr.getChild(1);
    exprSingle(newExpr);
    return true;
  }

  private boolean renameJsonExpr(AST expr) {
    if (expr.getType() != XQ.RenameJsonExpr) {
      return false;
    }
    AST target = expr.getChild(0);
    postFixExpr(target);
    AST newExpr = expr.getChild(1);
    exprSingle(newExpr);
    return true;
  }

  private boolean deleteJsonExpr(AST expr) {
    if (expr.getType() != XQ.DeleteJsonExpr) {
      return false;
    }
    AST target = expr.getChild(0);
    exprSingle(target);
    return true;
  }

  private boolean insertJsonExpr(AST expr) {
    if (expr.getType() != XQ.InsertJsonExpr) {
      return false;
    }
    AST src = expr.getChild(0);
    exprSingle(src);
    AST target = expr.getChild(1);
    exprSingle(target);

    if (expr.getChildCount() == 3) {
      AST position = expr.getChild(2);
      exprSingle(position);
    }

    return true;
  }
  // End JSONiq Update Facility

  protected boolean flowrExpr(AST flwor) throws QueryException {
    if (flwor.getType() != XQ.FlowrExpr) {
      return false;
    }
    int scopeCount = scopeCount();
    initialClause(flwor.getChild(0));
    for (int i = 1; i < flwor.getChildCount() - 1; i++) {
      intermediateClause(flwor.getChild(i));
    }
    exprSingle(flwor.getChild(flwor.getChildCount() - 1).getChild(0));
    for (int i = scopeCount(); i > scopeCount; i--) {
      closeScope();
    }
    return true;
  }

  protected boolean initialClause(AST clause) throws QueryException {
    return forClause(clause) || letClause(clause) || windowClause(clause);
  }

  protected boolean forClause(AST clause) throws QueryException {
    if (clause.getType() != XQ.ForClause) {
      return false;
    }
    forBinding(clause);
    return true;
  }

  protected boolean forBinding(AST clause) throws QueryException {
    openScope();
    int pos = 0;
    AST child = clause.getChild(pos++);
    QNm forVar = typedVarBinding(child);
    child = clause.getChild(pos++);
    if (child.getType() == XQ.AllowingEmpty) {
      child = clause.getChild(pos++);
    }
    if (child.getType() == XQ.TypedVariableBinding) {
      try {
        positionalVar(child);
      } catch (QueryException e) {
        if (e.getCode().equals(ErrorCode.ERR_DUPLICATE_VARIABLE_DECL)) {
          throw new QueryException(ErrorCode.ERR_FOR_VAR_AND_POS_VAR_EQUAL,
                                   "Bound variable '%s' and its associated positional variable have the same name",
                                   forVar);
        }
        throw e;
      }
      child = clause.getChild(pos);
    }
    exprSingle(child);
    offerScope();
    return true;
  }

  protected QNm typedVarBinding(AST binding) throws QueryException {
    QNm name = (QNm) binding.getChild(0).getValue();
    // expand, bind and update AST
    QNm expanded = expand(name, DefaultNS.EMPTY);
    name = bind(expanded);
    binding.getChild(0).setValue(name);
    // SequenceType stype;
    // if (binding.getChildCount() >= 2) {
    // stype = typeDeclaration(binding.getChild(1));
    // } else {
    // stype = new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany);
    // }
    return expanded;
  }

  SequenceType typeDeclaration(AST decl) throws QueryException {
    return sequenceType(decl);
  }

  protected QNm positionalVar(AST binding) throws QueryException {
    QNm name = (QNm) binding.getChild(0).getValue();
    // expand, bind and update AST
    QNm expanded = expand(name, DefaultNS.EMPTY);
    name = bind(expanded);
    binding.getChild(0).setValue(name);
    // SequenceType stype = new SequenceType(AtomicType.INR, Cardinality.One);
    return expanded;
  }

  protected boolean letClause(AST clause) throws QueryException {
    if (clause.getType() != XQ.LetClause) {
      return false;
    }
    letBinding(clause);
    return true;
  }

  protected boolean letBinding(AST clause) throws QueryException {
    openScope();
    typedVarBinding(clause.getChild(0));
    exprSingle(clause.getChild(1));
    offerScope();
    return true;
  }

  protected boolean windowClause(AST clause) throws QueryException {
    return tumblingWindowClause(clause) || slidingWindowClause(clause);
  }

  protected boolean tumblingWindowClause(AST clause) throws QueryException {
    if (clause.getType() != XQ.TumblingWindowClause) {
      return false;
    }
    openScope();
    typedVarBinding(clause.getChild(0));
    exprSingle(clause.getChild(1));
    windowStartCondition(clause.getChild(2));
    if (clause.getChildCount() >= 4) {
      windowEndCondition(clause.getChild(3));
    }
    // TODO check all variable names are distinct
    offerScope();
    return true;
  }

  protected boolean windowStartCondition(AST cond) throws QueryException {
    openScope();
    windowVars(cond.getChild(0));
    offerScope();
    exprSingle(cond.getChild(1));
    closeScope();
    return true;
  }

  protected boolean windowEndCondition(AST cond) throws QueryException {
    openScope();
    windowVars(cond.getChild(0));
    offerScope();
    exprSingle(cond.getChild(1));
    closeScope();
    return true;
  }

  protected void windowVars(AST windowVars) throws QueryException {
    for (int i = 0; i < windowVars.getChildCount(); i++) {
      QNm name = (QNm) windowVars.getChild(i).getChild(0).getValue();
      // expand, bind and update AST
      name = expand(name, DefaultNS.EMPTY);
      name = bind(name);
      windowVars.getChild(i).getChild(0).setValue(name);
    }
  }

  protected boolean slidingWindowClause(AST clause) throws QueryException {
    if (clause.getType() != XQ.SlidingWindowClause) {
      return false;
    }
    openScope();
    typedVarBinding(clause.getChild(0));
    exprSingle(clause.getChild(1));
    windowStartCondition(clause.getChild(2));
    windowEndCondition(clause.getChild(3));
    // TODO check all variable names are distinct
    offerScope();
    return true;
  }

  protected boolean intermediateClause(AST clause) throws QueryException {
    return initialClause(clause) || whereClause(clause) || groupByClause(clause) || orderByClause(clause)
        || countClause(clause);
  }

  protected boolean whereClause(AST clause) throws QueryException {
    if (clause.getType() != XQ.WhereClause) {
      return false;
    }
    exprSingle(clause.getChild(0));
    return true;
  }

  protected boolean groupByClause(AST clause) throws QueryException {
    if (clause.getType() != XQ.GroupByClause) {
      return false;
    }
    for (int i = 0; i < clause.getChildCount() - 1; i++) {
      AST groupBySpec = clause.getChild(i);
      QNm name = (QNm) groupBySpec.getChild(0).getValue();
      // expand resolve and update AST
      name = expand(name, DefaultNS.EMPTY);
      name = resolve(name);
      groupBySpec.getChild(0).setValue(name);
      // TODO check for err:XQST0094
      if (groupBySpec.getChildCount() >= 2) {
        String col = groupBySpec.getChild(1).getStringValue();
        if (!col.equals(StaticContext.UNICODE_COLLATION)) {
          throw new QueryException(ErrorCode.ERR_UNKNOWN_COLLATION_IN_FLWOR_CLAUSE,
                                   "Unknown collation in group-by clause: %s",
                                   col);
        }
      }
    }
    return true;
  }

  protected boolean orderByClause(AST clause) throws QueryException {
    if (clause.getType() != XQ.OrderByClause) {
      return false;
    }
    for (int i = 0; i < clause.getChildCount(); i++) {
      AST orderBySpec = clause.getChild(i);
      exprSingle(orderBySpec.getChild(0));
      for (int j = 1; j < orderBySpec.getChildCount(); j++) {
        if (orderBySpec.getChild(j).getType() == XQ.Collation) {
          String col = orderBySpec.getChild(j).getStringValue();
          if (!col.equals(StaticContext.UNICODE_COLLATION)) {
            throw new QueryException(ErrorCode.ERR_UNKNOWN_COLLATION_IN_FLWOR_CLAUSE,
                                     "Unknown collation in order-by clause: %s",
                                     col);
          }
        }
      }
    }
    return true;
  }

  protected boolean countClause(AST clause) throws QueryException {
    if (clause.getType() != XQ.CountClause) {
      return false;
    }
    QNm name = (QNm) clause.getChild(0).getChild(0).getValue();
    // expand, bind and update AST
    name = expand(name, DefaultNS.EMPTY);
    name = bind(name);
    clause.getChild(0).getChild(0).setValue(name);
    return true;
  }

  protected boolean quantifiedExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.QuantifiedExpr) {
      return false;
    }
    int scopeCount = scopeCount();
    // child 0 is quantifier type
    for (int i = 1; i < expr.getChildCount() - 1; i++) {
      openScope();
      typedVarBinding(expr.getChild(i).getChild(0));
      exprSingle(expr.getChild(i).getChild(1));
      offerScope();
    }
    exprSingle(expr.getChild(expr.getChildCount() - 1));
    for (int i = scopeCount(); i > scopeCount; i--) {
      closeScope();
    }
    return true;
  }

  protected boolean switchExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.SwitchExpr) {
      return false;
    }
    expr(expr.getChild(0));
    for (int i = 1; i < expr.getChildCount() - 1; i++) {
      switchClause(expr.getChild(i));
    }
    exprSingle(expr.getChild(expr.getChildCount() - 1));
    return true;
  }

  protected boolean switchClause(AST clause) throws QueryException {
    for (int i = 0; i < clause.getChildCount() - 1; i++) {
      exprSingle(clause.getChild(i));
    }
    exprSingle(clause.getChild(clause.getChildCount() - 1));
    return true;
  }

  protected boolean typeswitchExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.TypeSwitch) {
      return false;
    }
    openScope();
    expr(expr.getChild(0));
    for (int i = 1; i < expr.getChildCount(); i++) {
      // handle default case as case clause
      caseClause(expr.getChild(i));
    }
    closeScope();
    return true;
  }

  protected boolean caseClause(AST clause) throws QueryException {
    openScope();
    int pos = 0;
    AST varOrType = clause.getChild(pos);
    if (varOrType.getType() == XQ.Variable) {
      QNm name = (QNm) varOrType.getValue();
      // expand, bind and update AST
      name = expand(name, DefaultNS.EMPTY);
      name = bind(name);
      varOrType.setValue(name);
      pos++;
    }
    while (pos < clause.getChildCount() - 1) {
      sequenceType(clause.getChild(pos++));
    }
    offerScope();
    exprSingle(clause.getChild(clause.getChildCount() - 1));
    closeScope();
    return true;
  }

  protected boolean ifExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.IfExpr) {
      return false;
    }
    exprSingle(expr.getChild(0));
    exprSingle(expr.getChild(1));
    exprSingle(expr.getChild(2));
    return true;
  }

  protected boolean tryCatchExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.TryCatchExpr) {
      return false;
    }
    expr(expr.getChild(0));
    openScope();
    QNm error = bind(Namespaces.ERR_CODE);
    AST errorBinding = errorInfoBinding(error, "xs:QName", XQ.CardinalityOne);
    expr.insertChild(1, errorBinding);
    QNm desc = bind(Namespaces.ERR_DESCRIPTION);
    AST descBinding = errorInfoBinding(desc, "xs:string", XQ.CardinalityZeroOrOne);
    expr.insertChild(2, descBinding);
    QNm value = bind(Namespaces.ERR_VALUE);
    AST valueBinding = errorInfoBinding(value, "xs:item", XQ.CardinalityZeroOrMany);
    expr.insertChild(3, valueBinding);
    QNm module = bind(Namespaces.ERR_MODULE);
    AST moduleBinding = errorInfoBinding(module, "xs:string", XQ.CardinalityZeroOrOne);
    expr.insertChild(4, moduleBinding);
    QNm lineNo = bind(Namespaces.ERR_LINE_NUMBER);
    AST lineNoBinding = errorInfoBinding(lineNo, "xs:integer", XQ.CardinalityZeroOrOne);
    expr.insertChild(5, lineNoBinding);
    QNm colNo = bind(Namespaces.ERR_COLUMN_NUMBER);
    AST colNoBinding = errorInfoBinding(colNo, "xs:integer", XQ.CardinalityZeroOrOne);
    expr.insertChild(6, colNoBinding);
    offerScope();
    for (int i = 7; i < expr.getChildCount(); i++) {
      tryClause(expr.getChild(i));
    }
    closeScope();
    return true;
  }

  private AST errorInfoBinding(QNm error, String type, int card) {
    AST errorBinding = new AST(XQ.TypedVariableBinding);
    errorBinding.addChild(new AST(XQ.QNm, error));
    AST sType = new AST(XQ.SequenceType);
    AST aType = new AST(XQ.AtomicOrUnionType);
    aType.addChild(new AST(XQ.QNm, type));
    sType.addChild(aType);
    sType.addChild(new AST(card));
    errorBinding.addChild(sType);
    return errorBinding;
  }

  protected boolean tryClause(AST clause) throws QueryException {
    catchErrorList(clause.getChild(0));
    expr(clause.getChild(1));
    return true;
  }

  protected void catchErrorList(AST errorList) throws QueryException {
    for (int i = 0; i < errorList.getChildCount(); i++) {
      nameTest(errorList.getChild(i), false);
    }
  }

  protected void catchVars(AST catchVar) throws QueryException {
    for (int i = 0; i < catchVar.getChildCount(); i++) {
      QNm name = (QNm) catchVar.getChild(i).getValue();
      // expand and update AST
      name = expand(name, DefaultNS.EMPTY);
      catchVar.getChild(i).setValue(name);
    }
  }

  protected boolean orExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.OrExpr) {
      return andExpr(expr);
    }
    orExpr(expr.getChild(0));
    andExpr(expr.getChild(1));
    return true;
  }

  protected boolean andExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.AndExpr) {
      return comparisonExpr(expr);
    }
    andExpr(expr.getChild(0));
    comparisonExpr(expr.getChild(1));
    return true;
  }

  protected boolean comparisonExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.ComparisonExpr) {
      return stringConcatExpr(expr);
    }
    comparisonExpr(expr.getChild(1));
    stringConcatExpr(expr.getChild(2));
    return true;
  }

  protected boolean stringConcatExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.StringConcatExpr) {
      return rangeExpr(expr);
    }
    for (int i = 0; i < expr.getChildCount(); i++) {
      rangeExpr(expr.getChild(i));
    }
    return true;
  }

  protected boolean rangeExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.RangeExpr) {
      return additiveExpr(expr);
    }
    rangeExpr(expr.getChild(0));
    additiveExpr(expr.getChild(1));
    return true;
  }

  protected boolean additiveExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.ArithmeticExpr || expr.getChild(0).getType() != XQ.AddOp && expr.getChild(0).getType()
        != XQ.SubtractOp) {
      return multiplicativeExpr(expr);
    }
    additiveExpr(expr.getChild(1));
    multiplicativeExpr(expr.getChild(2));
    return true;
  }

  protected boolean multiplicativeExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.ArithmeticExpr) {
      return unionExpr(expr);
    }
    multiplicativeExpr(expr.getChild(1));
    unionExpr(expr.getChild(2));
    return true;
  }

  protected boolean unionExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.UnionExpr) {
      return intersectExpr(expr);
    }
    unionExpr(expr.getChild(0));
    intersectExpr(expr.getChild(1));
    return true;
  }

  protected boolean intersectExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.IntersectExpr && expr.getType() != XQ.ExceptExpr) {
      return instanceOfExpr(expr);
    }
    intersectExpr(expr.getChild(0));
    instanceOfExpr(expr.getChild(1));
    return true;
  }

  protected boolean instanceOfExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.InstanceofExpr) {
      return treatExpr(expr);
    }
    treatExpr(expr.getChild(0));
    sequenceType(expr.getChild(1));
    return true;
  }

  protected boolean treatExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.TreatExpr) {
      return castableExpr(expr);
    }
    castableExpr(expr.getChild(0));
    sequenceType(expr.getChild(1));
    return true;
  }

  protected boolean castableExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.CastableExpr) {
      return castExpr(expr);
    }
    castExpr(expr.getChild(0));
    singleType(expr.getChild(1));
    return true;
  }

  protected boolean castExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.CastExpr) {
      return unaryExpr(expr);
    }
    unaryExpr(expr.getChild(0));
    singleType(expr.getChild(1));
    return true;
  }

  protected boolean unaryExpr(AST expr) throws QueryException {
    return valueExpr(expr);
  }

  protected boolean valueExpr(AST expr) throws QueryException {
    return validateExpr(expr) || pathExpr(expr) || extensionExpr(expr);
  }

  protected boolean extensionExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.ExtensionExpr) {
      return false;
    }
    for (int i = 0; i < expr.getChildCount(); i++) {
      AST pragmaOrExpr = expr.getChild(i);
      if (pragmaOrExpr.getType() == XQ.Pragma) {
        QNm name = (QNm) pragmaOrExpr.getChild(0).getValue();
        // expand and update AST
        name = expand(name, DefaultNS.PRAGMA);
        pragmaOrExpr.getChild(0).setValue(name);
      } else {
        expr(pragmaOrExpr);
      }
    }
    return true;
  }

  protected boolean pathExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.PathExpr) {
      return stepExpr(expr);
    }
    for (int i = 0; i < expr.getChildCount(); i++) {
      stepExpr(expr.getChild(i));
      openContextItemScope();
    }
    for (int i = 0; i < expr.getChildCount(); i++) {
      closeContextItemScope();
    }
    return true;
  }

  protected boolean stepExpr(AST expr) throws QueryException {
    return postFixExpr(expr) || axisStep(expr);
  }

  protected boolean axisStep(AST expr) throws QueryException {
    if (expr.getType() != XQ.StepExpr) {
      return false;
    }
    // child 0 is the axis
    nodeTest(expr.getChild(1), expr.getChild(0).getChild(0).getType() == XQ.ATTRIBUTE);
    referContextItem();
    openContextItemScope();
    for (int i = 2; i < expr.getChildCount(); i++) {
      predicate(expr.getChild(i));
    }
    closeContextItemScope();
    return true;
  }

  protected ItemType nodeTest(AST nodeTest, boolean attributeAxis) throws QueryException {
    ItemType test = kindTest(nodeTest);
    test = test != null ? test : nameTest(nodeTest, !attributeAxis);
    return test;
  }

  protected ItemType nameTest(AST test, boolean element) throws QueryException {
    if (test.getType() != XQ.NameTest) {
      return null;
    }
    ItemType type = wildcard(test.getChild(0), element);
    if (type != null) {
      return type;
    }
    if (element) {
      QNm name = (QNm) test.getChild(0).getValue();
      // expand and update AST
      name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
      test.getChild(0).setValue(name);
      return new ElementType(name);
    } else {
      QNm name = (QNm) test.getChild(0).getValue();
      // expand and update AST
      name = expand(name, DefaultNS.EMPTY);
      test.getChild(0).setValue(name);
      return new AttributeType(name);
    }
  }

  protected ItemType wildcard(AST test, boolean element) throws QueryException {
    if (test.getType() == XQ.Wildcard) {
      return new ElementType();
    } else if (test.getType() == XQ.NSWildcardNameTest) {
      Kind kind = element ? Kind.ELEMENT : Kind.ATTRIBUTE;
      return new NSWildcardNameTest(kind, test.getStringValue());
    } else if (test.getType() == XQ.NSNameWildcardTest) {
      Kind kind = element ? Kind.ELEMENT : Kind.ATTRIBUTE;
      return new NSNameWildcardTest(kind, resolvePrefix(test.getStringValue()));
    } else {
      return null;
    }
  }

  protected void predicate(AST predicate) throws QueryException {
    expr(predicate.getChild(0));
  }

  protected boolean validateExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.ValidateExpr) {
      return false;
    }
    if (expr.getChild(0).getType() == XQ.QNm) {
      QNm name = (QNm) expr.getChild(0).getValue();
      // expand and update AST
      name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
      expr.getChild(0).setValue(name);
      sctx.getTypes().resolveType(name);
    }
    expr(expr.getChild(1));
    return true;
  }

  protected boolean postFixExpr(AST expr) throws QueryException {
    // BEGIN Custom object syntax extension
    if (expr.getType() == XQ.DerefExpr) {
      return derefExpr(expr);
    }
    if (expr.getType() == XQ.DerefDescendantExpr) {
      return derefDescendantExpr(expr);
    }
    // END Custom object syntax extension
    // BEGIN Custom array syntax extension
    if (expr.getType() == XQ.ArrayAccess) {
      expr(expr.getChild(0));
      exprSingle(expr.getChild(1));
      return true;
    }
    if (expr.getType() == XQ.ArrayIndexSlice) {
      expr(expr.getChild(0));
      exprSingle(expr.getChild(1));
      if (expr.getChildCount() == 3) {
        exprSingle(expr.getChild(2));
      }
      return true;
    }
    // END Custom array syntax extension
    // BEGIN Custom object syntax extension
    if (expr.getType() == XQ.ObjectProjection) {
      expr(expr.getChild(0));
      for (int i = 1; i < expr.getChildCount(); i++) {
        exprSingle(expr.getChild(i));
      }
      return true;
    }
    // END Custom object syntax extension
    if (expr.getType() == XQ.FilterExpr) {
      expr(expr.getChild(0));
      openContextItemScope();
      for (int i = 1; i < expr.getChildCount(); i++) {
        predicate(expr.getChild(i));
      }
      closeContextItemScope();
    } else if (expr.getType() == XQ.DynamicFunctionCallExpr) {
      expr(expr.getChild(0));
      for (int i = 1; i < expr.getChildCount(); i++) {
        argument(expr.getChild(i));
      }
    } else {
      return primaryExpr(expr);
    }
    return true;
  }

  protected boolean primaryExpr(AST expr) throws QueryException {
    return literal(expr) || varRef(expr) || parenthesizedExpr(expr) || contextItemExpr(expr) || functionCall(expr)
        || orderedExpr(expr) || unorderedExpr(expr) || constructor(expr) || functionItemExpr(expr);
  }

  protected boolean literal(AST expr) throws QueryException {
    return numericLiteral(expr) || expr.getType() == XQ.Str;
  }

  protected boolean varRef(AST expr) throws QueryException {
    if (expr.getType() != XQ.VariableRef) {
      return false;
    }
    QNm name = (QNm) expr.getValue();
    // expand, resolve and update AST
    name = expand(name, DefaultNS.EMPTY);
    name = resolve(name);
    expr.setValue(name);
    return true;
  }

  protected boolean parenthesizedExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.ParenthesizedExpr) {
      return false;
    }
    if (expr.getChildCount() != 0) {
      expr(expr.getChild(0));
    }
    return true;
  }

  protected boolean contextItemExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.ContextItemExpr) {
      return false;
    }
    referContextItem();
    return true;
  }

  protected boolean functionCall(AST expr) throws QueryException {
    if (expr.getType() != XQ.FunctionCall) {
      return false;
    }
    QNm name = (QNm) expr.getValue();
    // expand and update AST
    name = expand(name, DefaultNS.FUNCTION);
    expr.setValue(name);

    if (replaceFunctionCall(expr, name)) {
      return true;
    }

    int noOfParams = expr.getChildCount();
    for (int i = 0; i < noOfParams; i++) {
      argument(expr.getChild(i));
    }
    Function fun = sctx.getFunctions().resolve(name, noOfParams);
    if (fun == null) {
      unknownFunction(name, noOfParams);
    }
    if (noOfParams == 0 && fun.getSignature().defaultCtxItemType() != null) {
      referContextItem();
    }
    return true;
  }

  private void unknownFunction(QNm name, int noOfParams) throws QueryException {
    throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
                             "Unknown function: %s(%s)",
                             name,
                             (noOfParams > 0 ? "?" : "") + ", ?".repeat(Math.max(0, noOfParams - 1)));
  }

  protected boolean replaceFunctionCall(AST expr, QNm name) throws QueryException {
    if (name.getNamespaceURI().equals(Namespaces.DEFAULT_FN_NSURI)) {
      name = new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, name.getLocalName());

      var replaced = internalReplaceFunctionCall(expr, name);

      if (replaced) {
        return true;
      }

      name = new QNm(JSONFun.JSON_NSURI, JSONFun.JSON_NSURI, name.getLocalName());
      replaced = internalReplaceFunctionCall(expr, name);

      if (replaced) {
        return true;
      }

      name = new QNm(Bits.BIT_NSURI, Bits.BIT_PREFIX, name.getLocalName());
      return internalReplaceFunctionCall(expr, name);
    }

    return internalReplaceFunctionCall(expr, name);
  }

  private boolean internalReplaceFunctionCall(AST expr, QNm name) {
    int argc = expr.getChildCount();
    if (name.equals(Functions.FN_POSITION) || name.equals(Functions.FN_LAST)) {
      if (argc != 0) {
        throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
                                 "Illegal number of parameters for function %s() : %s'",
                                 name,
                                 argc);
      }
      // change expr to variable reference
      expr.setType(XQ.VariableRef);
      QNm newName = name.equals(Functions.FN_POSITION) ? Bits.FS_POSITION : Bits.FS_LAST;
      expr.setValue(newName);
      return true;
    }
    if (name.equals(Functions.FN_TRUE) || name.equals(Functions.FN_FALSE)) {
      if (argc != 0) {
        throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
                                 "Illegal number of parameters for function %s() : %s'",
                                 name,
                                 argc);
      }
      // change expr to boolean literal
      expr.setType(XQ.Bool);
      Bool val = name.equals(Functions.FN_TRUE) ? Bool.TRUE : Bool.FALSE;
      expr.setValue(val);
      return true;
    }
    if (name.equals(Functions.FN_DEFAULT_COLLATION)) {
      if (argc != 0) {
        throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
                                 "Illegal number of parameters for function %s() : %s'",
                                 name,
                                 argc);
      }
      // change expr to string literal
      expr.setType(XQ.Str);
      expr.setValue(new Str(sctx.getDefaultCollation()));
      return true;
    }
    if (name.equals(Functions.FN_STATIC_BASE_URI)) {
      if (argc != 0) {
        throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
                                 "Illegal number of parameters for function %s() : %s'",
                                 name,
                                 argc);
      }
      AnyURI baseURI = sctx.getBaseURI();
      if (baseURI != null) {
        // change expr to uri literal
        expr.setType(XQ.AnyURI);
        expr.setValue(baseURI);
      } else {
        // change expr to empty sequence
        expr.setType(XQ.SequenceExpr);
        expr.setValue(XQ.NAMES[XQ.SequenceExpr]);
      }
      return true;
    }
    if (name.equals(JSONFun.JSON_NULL)) {
      if (argc != 0) {
        throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
                                 "Illegal number of parameters for function %s() : %s'",
                                 name,
                                 argc);
      }
      expr.setType(XQ.Null);
      return true;
    }
    return false;
  }

  protected void argument(AST argument) throws QueryException {
    if (argument.getType() != XQ.ArgumentPlaceHolder) {
      exprSingle(argument);
    }
  }

  protected boolean orderedExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.OrderedExpr) {
      return false;
    }
    // TODO change order mode in static context
    expr(expr.getChild(0));
    return true;
  }

  protected boolean unorderedExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.UnorderedExpr) {
      return false;
    }
    // TODO change order mode in static context
    expr(expr.getChild(0));
    return true;
  }

  protected boolean constructor(AST expr) throws QueryException {
    if (directConstructor(expr) || computedConstructor(expr)) {
      return true;
    }
    // BEGIN Custom array syntax extension
    if (arrayConstructor(expr)) {
      return true;
    }
    // END Custom array syntax extension
    // BEGIN Custom object syntax extension
    return objectConstructor(expr);
    // END Custom object syntax extension
  }

  protected boolean directConstructor(AST expr) throws QueryException {
    return dirElemConstructor(expr) || dirCommentConstructor(expr) || dirPIConstructor(expr);
  }

  protected boolean dirElemConstructor(AST expr) throws QueryException {
    if (expr.getType() != XQ.DirElementConstructor) {
      return false;
    }
    // create new static context
    StaticContext psctx = sctx;
    this.sctx = new NestedContext(psctx);
    expr.setStaticContext(sctx);

    QNm name = (QNm) expr.getChild(0).getValue();

    // pre-check content sequence for direct
    // namespace attributes
    AST cseq = expr.getChild(1);
    for (int i = 0; i < cseq.getChildCount(); i++) {
      AST att = cseq.getChild(i);
      if (att.getType() != XQ.DirAttributeConstructor) {
        break;
      }
      QNm attName = (QNm) att.getChild(0).getValue();
      if ("xmlns".equals(attName.getPrefix())) {
        String prefix = attName.getLocalName();
        String uri = extractURIFromDirNSAttContent(att.getChild(1));
        checkDirNSAttBinding(prefix, uri);
        sctx.getNamespaces().declare(prefix, uri);
        // delete from context sequence
        // and prepend prefixed namespace declaration
        // in element constructor
        cseq.deleteChild(i--);
        AST nsDecl = new AST(XQ.NamespaceDeclaration);
        nsDecl.addChild(new AST(XQ.Str, prefix));
        nsDecl.addChild(new AST(XQ.AnyURI, uri));
        expr.insertChild(0, nsDecl);
      } else if ("xmlns".equals(attName.getLocalName())) {
        String uri = extractURIFromDirNSAttContent(att.getChild(1));
        sctx.getNamespaces().setDefaultElementNamespace(uri);
        // delete from context sequence
        // and prepend prefixed namespace declaration
        // in element constructor
        cseq.deleteChild(i--);
        AST nsDecl = new AST(XQ.NamespaceDeclaration);
        nsDecl.addChild(new AST(XQ.AnyURI, uri));
        expr.insertChild(0, nsDecl);
      }
    }

    // expand element name and update AST
    name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
    expr.getChild(0).setValue(name);

    // merge adjacent string literals and
    // strip boundary whitespace if requested
    int merge = 0;
    for (int i = 0; i < cseq.getChildCount(); i++) {
      AST c = cseq.getChild(i);
      if (c.getType() == XQ.Str) {
        if (sctx.isBoundarySpaceStrip() && c.checkProperty("boundaryWS")) {
          cseq.deleteChild(i--);
          merge = 0;
        } else {
          merge++;
        }
        continue;
      }
      if (merge > 1) {
        StringBuilder buf = new StringBuilder();
        int firstStrPos = i - merge;
        AST sc = cseq.getChild(firstStrPos);
        buf.append(sc.getStringValue());
        for (int j = 0; j < merge - 1; j++) {
          buf.append(cseq.getChild(firstStrPos + 1).getStringValue());
          cseq.deleteChild(firstStrPos + 1);
        }
        sc.setValue(buf.toString());
        i -= merge - 1;
      }
      merge = 0;
    }

    // finally process non-direct-namespace-attribute content
    AST p = null;
    for (int i = 0; i < cseq.getChildCount(); i++) {
      AST c = cseq.getChild(i);
      if (c.getType() == XQ.DirAttributeConstructor) {
        dirAttribute(c);
      } else {
        dirElementContent(c);
      }
      p = c;
    }

    // restore static context
    this.sctx = psctx;
    return true;
  }

  protected String extractURIFromDirNSAttContent(AST content) throws QueryException {
    StringBuilder uri = new StringBuilder();
    for (int i = 0; i < content.getChildCount(); i++) {
      AST c = content.getChild(i);
      if (c.getType() == XQ.EnclosedExpr) {
        throw new QueryException(ErrorCode.ERR_ENCLOSED_EXPR_IN_NS_ATTRIBUTE,
                                 "Illegal enclosed expression in direct namespace attribute");
      }
      uri.append(c.getStringValue());
    }
    String eolNormalized = Whitespace.normalizeXML11(uri.toString());
    String wsNormalized = Whitespace.collapse(eolNormalized);
    return wsNormalized;
  }

  protected void checkDirNSAttBinding(String prefix, String uri) throws QueryException {
    if (Namespaces.XML_PREFIX.equals(prefix)) {
      if (Namespaces.XML_NSURI.equals(uri)) {
        throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
                                 "Illegal mapping of the prefix '%s' to the namespace URI '%s'",
                                 Namespaces.XML_PREFIX,
                                 uri);
      }
    } else if (Namespaces.XMLNS_PREFIX.equals(prefix)) {
      throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
                               "Illegal namespace prefix '%s'",
                               Namespaces.XMLNS_PREFIX);
    } else if (Namespaces.XML_NSURI.equals(uri)) {
      throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
                               "Illegal namespace URI '%s'",
                               Namespaces.XMLNS_NSURI);
    }
  }

  protected void dirAttribute(AST dirAtt) throws QueryException {
    QNm name = (QNm) dirAtt.getChild(0).getValue();
    // expand and update AST
    name = expand(name, DefaultNS.EMPTY);
    dirAtt.getChild(0).setValue(name);
    AST content = dirAtt.getChild(1);

    // TODO checks?
    // TODO concat?
    for (int i = 0; i < content.getChildCount(); i++) {
      AST c = content.getChild(i);
      if (c.getType() != XQ.Str) {
        enclosedExpr(c);
      }
    }
  }

  protected boolean dirElementContent(AST content) throws QueryException {
    return directConstructor(content) || content.getType() == XQ.Str || enclosedExpr(content);
  }

  protected boolean dirCommentConstructor(AST expr) throws QueryException {
    if (expr.getType() != XQ.DirCommentConstructor) {
      return false;
    }
    // TODO check comment content?
    return true;
  }

  protected boolean dirPIConstructor(AST expr) throws QueryException {
    if (expr.getType() != XQ.DirPIConstructor) {
      return false;
    }
    // TODO check PI target and content?
    return true;
  }

  protected boolean computedConstructor(AST expr) throws QueryException {
    return compDocConstructor(expr) || compElemConstructor(expr) || compAttrConstructor(expr)
        || compNamespaceConstructor(expr) || compTextConstructor(expr) || compCommentConstructor(expr)
        || compPIConstructor(expr);
  }

  protected boolean compDocConstructor(AST expr) throws QueryException {
    if (expr.getType() != XQ.CompDocumentConstructor) {
      return false;
    }
    if (expr.getChildCount() > 0) {
      expr(expr.getChild(0));
    }
    return true;
  }

  protected boolean compElemConstructor(AST expr) throws QueryException {
    if (expr.getType() != XQ.CompElementConstructor) {
      return false;
    }
    AST nameExpr = expr.getChild(0);
    if (nameExpr.getType() == XQ.QNm) {
      QNm name = (QNm) nameExpr.getValue();
      name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
      nameExpr.setValue(name);
    } else {
      expr(nameExpr);
    }

    AST cseq = expr.getChild(1);
    if (cseq.getChildCount() == 1) {
      expr(cseq.getChild(0));
    }
    return true;
  }

  protected boolean compAttrConstructor(AST expr) throws QueryException {
    if (expr.getType() != XQ.CompAttributeConstructor) {
      return false;
    }
    AST nameExpr = expr.getChild(0);
    if (nameExpr.getType() == XQ.QNm) {
      QNm name = (QNm) nameExpr.getValue();
      name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
      nameExpr.setValue(name);
    } else {
      expr(nameExpr);
    }

    AST cseq = expr.getChild(1);
    if (cseq.getChildCount() == 1) {
      expr(cseq.getChild(0));
    }
    return true;
  }

  protected boolean compNamespaceConstructor(AST expr) throws QueryException {
    if (expr.getType() != XQ.CompNamespaceConstructor) {
      return false;
    }
    AST prefixExpr = expr.getChild(0);
    if (prefixExpr.getType() != XQ.Str) {
      expr(prefixExpr);
    }
    if (expr.getChildCount() == 2) {
      expr(expr.getChild(1));
    }
    return true;
  }

  protected boolean compTextConstructor(AST expr) throws QueryException {
    if (expr.getType() != XQ.CompTextConstructor) {
      return false;
    }
    expr(expr.getChild(0));
    return true;
  }

  protected boolean compCommentConstructor(AST expr) throws QueryException {
    if (expr.getType() != XQ.CompCommentConstructor) {
      return false;
    }
    expr(expr.getChild(0));
    return true;
  }

  protected boolean compPIConstructor(AST expr) throws QueryException {
    if (expr.getType() != XQ.CompPIConstructor) {
      return false;
    }
    AST prefixExpr = expr.getChild(0);
    if (prefixExpr.getType() != XQ.Str) {
      expr(prefixExpr);
    }
    if (expr.getChildCount() == 2) {
      expr(expr.getChild(1));
    }
    return true;
  }

  protected boolean functionItemExpr(AST expr) throws QueryException {
    return literalFunctionItem(expr) || inlineFunctionItem(expr);
  }

  protected boolean literalFunctionItem(AST expr) throws QueryException {
    if (expr.getType() != XQ.LiteralFuncItem) {
      return false;
    }
    QNm name = (QNm) expr.getChild(0).getValue();
    // expand and update AST
    name = expand(name, DefaultNS.FUNCTION);
    expr.getChild(0).setValue(name);
    // TODO lookup and checks
    return true;
  }

  protected boolean inlineFunctionItem(AST expr) throws QueryException {
    if (expr.getType() != XQ.InlineFuncItem) {
      return false;
    }

    int pos = 0;
    AST child;

    // parameters
    int noOfParameters = expr.getChildCount() - 2;
    QNm[] pNames = new QNm[noOfParameters];
    SequenceType[] pTypes = new SequenceType[noOfParameters];
    for (int i = 0; i < noOfParameters; i++) {
      child = expr.getChild(pos++);
      typedVarBinding(child);
      pNames[i] = (QNm) child.getChild(0).getValue();
      for (int j = 0; j < i; j++) {
        if (pNames[i].atomicCmp(pNames[j]) == 0) {
          throw new QueryException(ErrorCode.ERR_DUPLICATE_FUN_PARAMETER,
                                   "Duplicate parameter in declared function: %s",
                                   pNames[j]);
        }
      }
      if (child.getChildCount() == 2) {
        pTypes[i] = sequenceType(child.getChild(1));
      } else {
        pTypes[i] = SequenceType.ITEM_SEQUENCE;
      }
    }

    offerScope();

    // result type
    child = expr.getChild(pos++);
    SequenceType resultType = sequenceType(child);
    functionBody(expr.getChild(pos));

    // register function beforehand to support recursion
    Signature signature = new Signature(resultType, pTypes);
    UDF udf = new UDF(null, signature, false);

    expr.setProperty("udf", udf);
    expr.setProperty("paramNames", pNames);

    return true;
  }

  boolean enclosedExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.EnclosedExpr) {
      return false;
    }
    expr(expr.getChild(0));
    return true;
  }

  // BEGIN Custom array syntax extension
  protected boolean arrayConstructor(AST expr) throws QueryException {
    if (expr.getType() != XQ.ArrayConstructor) {
      return false;
    }
    for (int i = 0; i < expr.getChildCount(); i++) {
      AST field = expr.getChild(i);
      int fType = field.getType();
      if (fType != XQ.SequenceField && fType != XQ.FlattenedField) {
        throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR, "Invalid array field type: %s", fType);
      }
      expr(field.getChild(0));
    }
    return true;
  }

  // END Custom array syntax extension

  // BEGIN Custom object syntax extension
  protected boolean objectConstructor(AST expr) throws QueryException {
    if (expr.getType() != XQ.ObjectConstructor) {
      return false;
    }
    for (int i = 0; i < expr.getChildCount(); i++) {
      AST field = expr.getChild(i);
      int fType = field.getType();
      if (fType == XQ.KeyValueField) {
        expr(field.getChild(0));
        expr(field.getChild(1));
      } else if (fType == XQ.ObjectField) {
        expr(field.getChild(0));
      } else {
        throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR, "Invalid record field type: %s", fType);
      }
    }
    return true;
  }

  protected boolean derefExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.DerefExpr) {
      return false;
    }
    expr(expr.getChild(0));
    for (int i = 1; i < expr.getChildCount(); i++) {
      if (!stepExpr(expr.getChild(i))) {
        return false;
      }
    }
    return true;
  }

  protected boolean derefDescendantExpr(AST expr) throws QueryException {
    if (expr.getType() != XQ.DerefDescendantExpr) {
      return false;
    }
    expr(expr.getChild(0));
    for (int i = 1; i < expr.getChildCount(); i++) {
      if (!stepExpr(expr.getChild(i))) {
        return false;
      }
    }
    return true;
  }

  // END Custom object syntax extension

  protected boolean numericLiteral(AST literal) throws QueryException {
    return integerLiteral(literal) || decimalLiteral(literal) || doubleLiteral(literal);
  }

  protected boolean doubleLiteral(AST literal) throws QueryException {
    return literal.getType() == XQ.Dbl;
  }

  protected boolean decimalLiteral(AST literal) throws QueryException {
    return literal.getType() == XQ.Dec;
  }

  protected boolean integerLiteral(AST literal) throws QueryException {
    return literal.getType() == XQ.Int;
  }

  QNm bind(QNm name) throws QueryException {
    if (Query.DEBUG && log.isDebugEnabled()) {
      log.debug("Declare variable " + name);
    }
    if (variables.check(name)) {
      throw new QueryException(ErrorCode.ERR_DUPLICATE_VARIABLE_DECL, "Variable $%s has already been declared.", name);
    }
    return variables.declare(name);
  }

  protected QNm resolve(QNm name) throws QueryException {
    if (Query.DEBUG && log.isDebugEnabled()) {
      log.debug("Resolve variable " + name);
    }
    QNm resolved = variables.resolve(name);
    if (resolved == null) {
      Variable dVar = module.getVariables().resolve(name);
      if (dVar != null) {
        return name;
      }
    }
    if (resolved == null) {
      throw new QueryException(ErrorCode.ERR_UNDEFINED_REFERENCE, "Variable $%s has not been declared.", name);
    }
    return resolved;
  }

  protected void openScope() throws QueryException {
    if (Query.DEBUG && log.isDebugEnabled()) {
      log.debug("Open scope");
    }
    variables.openScope();
  }

  protected void offerScope() throws QueryException {
    if (Query.DEBUG && log.isDebugEnabled()) {
      log.debug("Offer scope");
    }
    variables.offerScope();
  }

  protected void closeScope() throws QueryException {
    if (Query.DEBUG && log.isDebugEnabled()) {
      log.debug("Close scope");
    }
    variables.closeScope();
  }

  protected int scopeCount() {
    return variables.scopeCount();
  }

  protected void openContextItemScope() throws QueryException {

  }

  protected void closeContextItemScope() throws QueryException {

  }

  protected void referContextItem() throws QueryException {

  }
}