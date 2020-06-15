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

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.*;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.Bits;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.expr.*;
import org.brackit.xquery.expr.ArithmeticExpr.ArithmeticOp;
import org.brackit.xquery.expr.NodeCmpExpr.NodeCmp;
import org.brackit.xquery.expr.RecordExpr.Field;
import org.brackit.xquery.expr.RecordExpr.KeyValueField;
import org.brackit.xquery.expr.RecordExpr.RecordField;
import org.brackit.xquery.function.FunctionExpr;
import org.brackit.xquery.function.UDF;
import org.brackit.xquery.function.bit.BitFun;
import org.brackit.xquery.function.json.JSONFun;
import org.brackit.xquery.module.Module;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.operator.*;
import org.brackit.xquery.update.*;
import org.brackit.xquery.update.Insert.InsertType;
import org.brackit.xquery.update.json.*;
import org.brackit.xquery.util.Cmp;
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.util.aggregator.Aggregate;
import org.brackit.xquery.util.sort.Ordering.OrderModifier;
import org.brackit.xquery.xdm.*;
import org.brackit.xquery.xdm.type.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Sebastian Baechle
 */
public class Compiler implements Translator {

  protected static class ClauseBinding {
    final ClauseBinding in;
    final Operator operator;
    final Binding[] bindings;

    ClauseBinding(ClauseBinding in, Operator operator, Binding... bindings) {
      this.in = in;
      this.operator = operator;
      this.bindings = bindings;
    }

    void unbind() {
      if (in != null) {
        in.unbind();
      }
    }
  }

  protected static class AggregateBinding {
    final QNm srcVar;
    final QNm aggVar;
    final SequenceType aggVarType;
    final Aggregate agg;

    public AggregateBinding(QNm srcVar, QNm aggVar, SequenceType aggVarType, Aggregate agg) {
      this.srcVar = srcVar;
      this.aggVar = aggVar;
      this.aggVarType = aggVarType;
      this.agg = agg;
    }
  }

  protected VariableTable table;
  protected StaticContext ctx;
  protected final Map<QNm, Str> options;

  public Compiler(Map<QNm, Str> options) {
    this.options = options;
  }

  @Override
  public Expr expression(Module module, StaticContext ctx, AST expr, boolean allowUpdate) throws QueryException {
    this.table = new VariableTable(module);
    this.ctx = ctx;
    Expr e = expr(expr, !allowUpdate);
    table.resolvePositions();
    return e;
  }

  @Override
  public Expr function(Module module, StaticContext ctx, UDF udf, QNm[] params, AST expr, boolean allowUpdate)
      throws QueryException {
    this.table = new VariableTable(module);
    this.ctx = ctx;
    // bind parameter
    SequenceType[] types = udf.getSignature().getParams();
    for (int i = 0; i < params.length; i++) {
      table.bind(params[i], types[i]);
    }
    // ensure fixed parameter positions
    for (final QNm param : params) {
      table.resolve(param);
    }
    // compile body
    Expr body = expr(expr, !allowUpdate);
    // unbind parameters
    for (int i = 0; i < params.length; i++) {
      table.unbind();
    }
    table.resolvePositions();
    return body;
  }

  protected Expr expr(AST node, boolean disallowUpdatingExpr) throws QueryException {
    Expr expr = anyExpr(node);

    if (disallowUpdatingExpr && expr.isUpdating()) {
      throw new QueryException(ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE, "Illegal nested update expression");
    }

    return expr;
  }

  protected Expr anyExpr(AST node) throws QueryException {
    return switch (node.getType()) {
      case XQ.FlowrExpr -> flowrExpr(node);
      case XQ.QuantifiedExpr -> quantifiedExpr(node);
      case XQ.EnclosedExpr, XQ.ParenthesizedExpr, XQ.SequenceExpr -> sequenceExpr(node);
      case XQ.Str -> new Str(Whitespace.normalizeXML11(node.getStringValue()));
      case XQ.Null -> new Null();
      case XQ.Int, XQ.Dbl, XQ.Dec, XQ.QNm, XQ.AnyURI, XQ.Bool -> (Atomic) node.getValue();
      case XQ.VariableRef -> variableRefExpr(node);
      case XQ.ArithmeticExpr -> arithmeticExpr(node);
      case XQ.ComparisonExpr -> comparisonExpr(node);
      case XQ.RangeExpr -> rangeExpr(node);
      case XQ.AndExpr -> andExpr(node);
      case XQ.OrExpr -> orExpr(node);
      case XQ.CastExpr -> castExpr(node);
      case XQ.CastableExpr -> castableExpr(node);
      case XQ.TreatExpr -> treatExpr(node);
      case XQ.InstanceofExpr -> instanceOfExpr(node);
      case XQ.TypeSwitch -> typeswitchExpr(node);
      case XQ.IfExpr -> ifExpr(node);
      case XQ.SwitchExpr -> switchExpr(node);
      case XQ.FilterExpr -> filterExpr(node);
      case XQ.DirElementConstructor, XQ.CompElementConstructor -> elementExpr(node);
      case XQ.DirAttributeConstructor, XQ.CompAttributeConstructor -> attributeExpr(node);
      case XQ.DirCommentConstructor, XQ.CompCommentConstructor -> commentExpr(node);
      case XQ.CompTextConstructor -> textExpr(node);
      case XQ.CompDocumentConstructor -> documentExpr(node);
      case XQ.DirPIConstructor, XQ.CompPIConstructor -> piExpr(node);
      case XQ.FunctionCall -> functionCall(node);
      case XQ.PathExpr -> pathExpr(node);
      case XQ.StepExpr -> stepExpr(node);
      case XQ.ContextItemExpr -> table.resolve(Bits.FS_DOT);
      case XQ.InsertExpr -> insertExpr(node);
      case XQ.DeleteExpr -> deleteExpr(node);
      case XQ.ReplaceNodeExpr, XQ.ReplaceValueExpr -> replaceExpr(node);
      case XQ.RenameExpr -> renameExpr(node);
      case XQ.TransformExpr -> transformExpr(node);
      case XQ.OrderedExpr, XQ.UnorderedExpr, XQ.RecordField -> anyExpr(node.getChild(0));
      case XQ.UnionExpr -> unionExpr(node);
      case XQ.ExceptExpr -> exceptExpr(node);
      case XQ.IntersectExpr -> intersectExpr(node);
      case XQ.TryCatchExpr -> tryCatchExpr(node);
      case XQ.ExtensionExpr -> extensionExpr(node);
      case XQ.ValidateExpr -> throw new QueryException(ErrorCode.ERR_SCHEMA_VALIDATION_FEATURE_NOT_SUPPORTED,
                                                       "Schema validation feature is not supported.");
      case XQ.ArrayConstructor -> arrayExpr(node);
      case XQ.ArrayAccess -> arrayAccessExpr(node);
      case XQ.RecordConstructor -> recordExpr(node);
      case XQ.DerefExpr -> derefExpr(node);
      case XQ.RecordProjection -> projectionExpr(node);
      case XQ.InsertJsonExpr -> insertJsonExpr(node);
      case XQ.DeleteJsonExpr -> deleteJsonExpr(node);
      case XQ.ReplaceJsonExpr -> replaceJsonExpr(node);
      case XQ.RenameJsonExpr -> renameJsonExpr(node);
      case XQ.AppendJsonExpr -> appendJsonExpr(node);
      default -> throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                                          "Unexpected AST expr node '%s' of type: %s",
                                          node,
                                          node.getType());
    };
  }

  private Expr deleteJsonExpr(AST node) {
    AST derefOrArrayIndexNode = node.getChild(0);
    Expr targetExpr = expr(derefOrArrayIndexNode.getChild(0), true);
    Expr fieldOrIndex = expr(derefOrArrayIndexNode.getChild(1), true);

    return new DeleteJson(targetExpr, fieldOrIndex);
  }

  private Expr insertJsonExpr(AST node) {
    Expr sourceExpr = expr(node.getChild(0), true);
    Expr targetExpr = expr(node.getChild(1), true);

    final int position;
    if (node.getChildCount() == 3) {
      position = ((Int32) node.getChild(2).getValue()).intValue();
    } else {
      position = -1;
    }

    return new InsertJson(sourceExpr, targetExpr, position);
  }

  private Expr appendJsonExpr(AST node) {
    Expr sourceExpr = expr(node.getChild(0), true);
    Expr targetExpr = expr(node.getChild(1), true);

    return new AppendJsonArrayValue(sourceExpr, targetExpr);
  }

  private Expr replaceJsonExpr(AST node) {
    AST derefOrArrayIndexNode = node.getChild(0);
    Expr targetExpr = expr(derefOrArrayIndexNode.getChild(0), true);
    Expr fieldOrIndex = expr(derefOrArrayIndexNode.getChild(1), true);
    Expr sourceExpr = expr(node.getChild(1), true);

    return new ReplaceJsonValue(sourceExpr, targetExpr, fieldOrIndex);
  }

  private Expr renameJsonExpr(AST node) {
    AST derefNode = node.getChild(0);
    Expr targetExpr = expr(derefNode.getChild(0), true);
    Expr oldFieldExpr = expr(derefNode.getChild(1), true);
    Expr newFieldExpr = expr(node.getChild(1), true);

    return new RenameJsonField(targetExpr, oldFieldExpr, newFieldExpr);
  }

  protected Expr tryCatchExpr(AST node) throws QueryException {
    Expr expr = expr(node.getChild(0), true);

    Binding code =
        table.bind((QNm) node.getChild(1).getChild(0).getValue(), new SequenceType(AtomicType.QNM, Cardinality.One));
    Binding desc = table.bind((QNm) node.getChild(2).getChild(0).getValue(),
                              new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne));
    Binding value = table.bind((QNm) node.getChild(3).getChild(0).getValue(),
                               new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany));
    Binding module = table.bind((QNm) node.getChild(4).getChild(0).getValue(),
                                new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne));
    Binding lineNo = table.bind((QNm) node.getChild(5).getChild(0).getValue(),
                                new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne));
    Binding colNo = table.bind((QNm) node.getChild(6).getChild(0).getValue(),
                               new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne));

    int catchCount = node.getChildCount() - 7;
    TryCatchExpr.ErrorCatch[][] catches = new TryCatchExpr.ErrorCatch[catchCount][];
    Expr[] handler = new Expr[catchCount];
    for (int i = 0; i < catchCount; i++) {
      AST clause = node.getChild(i + 7);
      AST catchErrorList = clause.getChild(0);
      int errorCount = catchErrorList.getChildCount();
      catches[i] = new TryCatchExpr.ErrorCatch[errorCount];
      for (int j = 0; j < errorCount; j++) {
        AST nametest = catchErrorList.getChild(j);
        catches[i][j] = tryCatchNameTest(nametest);
      }
      handler[i] = expr(clause.getChild(1), true);
    }

    for (int i = 0; i < 6; i++) {
      table.unbind();
    }

    return new TryCatchExpr(expr,
                            catches,
                            handler,
                            code.isReferenced(),
                            desc.isReferenced(),
                            value.isReferenced(),
                            module.isReferenced(),
                            lineNo.isReferenced(),
                            colNo.isReferenced());
  }

  protected TryCatchExpr.ErrorCatch tryCatchNameTest(AST child) throws QueryException {
    AST name = child.getChild(0);
    switch (name.getType()) {
      case XQ.Wildcard:
        return new TryCatchExpr.Wildcard();
      case XQ.NSWildcardNameTest:
        return new TryCatchExpr.NSWildcard(name.getStringValue());
      case XQ.NSNameWildcardTest:
        return new TryCatchExpr.NameWildcard(name.getStringValue());
      default:
        QNm qnm = (QNm) name.getValue();
        return new TryCatchExpr.Name(qnm.getLocalName(), qnm.getNamespaceURI());
    }
  }

  protected Expr extensionExpr(AST node) throws QueryException {
    return (node.getChildCount() == 2) ? anyExpr(node.getChild(1)) : new EmptyExpr();
  }

  protected Expr unionExpr(AST node) throws QueryException {
    Expr firstExpr = expr(node.getChild(0), true);
    Expr secondExpr = expr(node.getChild(1), true);
    return new UnionExpr(firstExpr, secondExpr);
  }

  protected Expr exceptExpr(AST node) throws QueryException {
    Expr firstExpr = expr(node.getChild(0), true);
    Expr secondExpr = expr(node.getChild(1), true);
    return new ExceptExpr(firstExpr, secondExpr);
  }

  protected Expr intersectExpr(AST node) throws QueryException {
    Expr firstExpr = expr(node.getChild(0), true);
    Expr secondExpr = expr(node.getChild(1), true);
    return new IntersectExpr(firstExpr, secondExpr);
  }

  protected Expr castExpr(AST node) throws QueryException {
    Expr expr = expr(node.getChild(0), true);
    AST type = node.getChild(1);
    AST aouType = type.getChild(0);
    Type targetType = resolveType((QNm) aouType.getChild(0).getValue(), true);
    boolean allowEmptySequence =
        ((type.getChildCount() == 2) && (type.getChild(1).getType() == XQ.CardinalityZeroOrOne));
    StaticContext sctx = node.getStaticContext();
    return new Cast(sctx, expr, targetType, allowEmptySequence);
  }

  protected Expr castableExpr(AST node) throws QueryException {
    Expr expr = expr(node.getChild(0), true);
    AST type = node.getChild(1);
    AST aouType = type.getChild(0);
    Type targetType = resolveType((QNm) aouType.getChild(0).getValue(), true);
    boolean allowEmptySequence =
        ((type.getChildCount() == 2) && (type.getChild(1).getType() == XQ.CardinalityZeroOrOne));
    StaticContext sctx = node.getStaticContext();
    return new Castable(sctx, expr, targetType, allowEmptySequence);
  }

  protected Expr treatExpr(AST node) throws QueryException {
    Expr expr = expr(node.getChild(0), true);
    SequenceType sequenceType = sequenceType(node.getChild(1));
    return new Treat(expr, sequenceType);
  }

  protected Expr instanceOfExpr(AST node) throws QueryException {
    Expr expr = expr(node.getChild(0), true);
    SequenceType sequenceType = sequenceType(node.getChild(1));
    return new InstanceOf(expr, sequenceType);
  }

  protected Expr typeswitchExpr(AST node) throws QueryException {
    Expr operandExpr = expr(node.getChild(0), false);
    if (operandExpr.isUpdating()) {
      throw (new QueryException(ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE,
                                "Operand expression of typeswitch expression must not be updating."));
    }

    boolean updating = false;
    int vacOrUpdate = 0;
    int cases = node.getChildCount() - 2;
    Expr[] caseExprs = (cases > 0 ? new Expr[cases] : null);
    SequenceType[] caseTypes = (cases > 0 ? new SequenceType[cases] : null);
    boolean[] varRefs = new boolean[cases + 1];

    for (int i = 0; i < cases; i++) {
      AST caseNode = node.getChild(i + 1);
      AST firstChild = caseNode.getChild(0);
      int c = 0;
      QNm varName = null;
      Binding binding = null;

      if (firstChild.getType() == XQ.Variable) {
        c++;
        varName = (QNm) firstChild.getValue();
      }

      caseTypes[i] = sequenceType(caseNode.getChild(c++));

      if (varName != null) {
        binding = table.bind(varName, caseTypes[i]);
      }

      caseExprs[i] = expr(caseNode.getChild(c), false);

      if (varName != null) {
        if (binding.isReferenced()) {
          varRefs[i] = true;
        }
        table.unbind();
      }

      if (caseExprs[i].isVacuous()) {
        vacOrUpdate++;
      }

      if (caseExprs[i].isUpdating()) {
        updating = true;
        vacOrUpdate++;
      }
    }

    AST defaultNode = node.getChild(node.getChildCount() - 1);
    AST firstChild = defaultNode.getChild(0);
    int c = 0;
    QNm varName = null;
    Binding binding = null;

    if (firstChild.getType() == XQ.Variable) {
      c++;
      varName = (QNm) firstChild.getValue();
      binding = table.bind(varName, SequenceType.ITEM_SEQUENCE);
    }

    Expr defaultExpr = expr(defaultNode.getChild(c), false);

    if (varName != null) {
      if (binding.isReferenced()) {
        varRefs[varRefs.length - 1] = true;
      }
      table.unbind();
    }

    if (defaultExpr.isVacuous()) {
      vacOrUpdate++;
    }

    if (defaultExpr.isUpdating()) {
      updating = true;
      vacOrUpdate++;
    }

    if (updating && vacOrUpdate < cases + 1) {
      throw (new QueryException(ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE,
                                "One updating expression in a typeswitch case requires all branches to be updating or vacuous expressions."));
    }

    return new TypeswitchExpr(operandExpr,
                              caseExprs,
                              caseTypes,
                              varRefs,
                              defaultExpr,
                              updating,
                              vacOrUpdate == cases + 1);
  }

  protected Expr filterExpr(AST node) throws QueryException {
    Expr expr = expr(node.getChild(0), true);
    int noOfPredicates = node.getChildCount() - 1;
    Expr[] predicates = new Expr[noOfPredicates];
    boolean[] bindItem = new boolean[noOfPredicates];
    boolean[] bindPos = new boolean[noOfPredicates];
    boolean[] bindSize = new boolean[noOfPredicates];

    for (int i = 0; i < noOfPredicates; i++) {
      Binding itemBinding = table.bind(Bits.FS_DOT, SequenceType.ITEM);
      Binding posBinding = table.bind(Bits.FS_POSITION, SequenceType.INTEGER);
      Binding sizeBinding = table.bind(Bits.FS_LAST, SequenceType.INTEGER);
      predicates[i] = expr(node.getChild(1 + i).getChild(0), true);
      table.unbind();
      table.unbind();
      table.unbind();
      bindItem[i] = itemBinding.isReferenced();
      bindPos[i] = posBinding.isReferenced();
      bindSize[i] = sizeBinding.isReferenced();
    }

    return new FilterExpr(expr, predicates, bindItem, bindPos, bindSize);
  }

  protected Expr insertExpr(AST node) throws QueryException {
    final AST typeNode = node.getChild(0);
    final InsertType insertType = switch (typeNode.getType()) {
      case XQ.InsertInto -> InsertType.INTO;
      case XQ.InsertBefore -> InsertType.BEFORE;
      case XQ.InsertAfter -> InsertType.AFTER;
      case XQ.InsertFirst -> InsertType.FIRST;
      case XQ.InsertLast -> InsertType.LAST;
      default -> throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                                          "Unexpected AST expr node '%s' of type: %s",
                                          typeNode,
                                          typeNode.getType());
    };

    final Expr sourceExpr = expr(node.getChild(1), true);
    final Expr targetExpr = expr(node.getChild(2), true);

    return new Insert(sourceExpr, targetExpr, insertType);
  }

  protected Expr deleteExpr(AST node) throws QueryException {
    Expr targetExpr = expr(node.getChild(0), true);
    return new Delete(targetExpr);
  }

  protected Expr replaceExpr(AST node) throws QueryException {
    boolean replaceNode = (node.getType() == XQ.ReplaceNodeExpr);
    Expr targetExpr = expr(node.getChild(0), true);
    Expr sourceExpr = expr(node.getChild(1), true);
    return (replaceNode) ? new ReplaceNode(sourceExpr, targetExpr) : new ReplaceValue(sourceExpr, targetExpr);
  }

  protected Expr renameExpr(AST node) throws QueryException {
    Expr targetExpr = expr(node.getChild(0), true);
    Expr sourceExpr = expr(node.getChild(1), true);
    return new Rename(node.getStaticContext(), sourceExpr, targetExpr);
  }

  protected Expr transformExpr(AST node) throws QueryException {
    AST current;

    QNm varName;
    int childCount = node.getChildCount();
    Expr[] sourceExprs = new Expr[childCount - 2];
    Binding[] bindings = new Binding[childCount - 2];
    boolean[] referenced = new boolean[childCount - 2];
    int c = 0;

    while ((current = node.getChild(c)).getType() == XQ.CopyVariableBinding) {
      varName = (QNm) current.getChild(0).getValue();
      sourceExprs[c] = expr(current.getChild(1), true);
      bindings[c++] = table.bind(varName, SequenceType.ITEM);
    }

    Expr modifyExpr = expr(current, false);

    if (!modifyExpr.isUpdating() && !modifyExpr.isVacuous()) {
      throw (new QueryException(ErrorCode.ERR_UPDATING_OR_VACUOUS_EXPR_REQUIRED,
                                "Modify clause must not contain an expression that is non-updating and non-vacuous."));
    }

    Expr returnExpr = expr(node.getChild(++c), true);

    for (int i = 0; i < childCount - 2; i++) {
      if (bindings[i].isReferenced()) {
        referenced[i] = true;
      }
      table.unbind();
    }

    return new Transform(sourceExprs, referenced, modifyExpr, returnExpr);
  }

  protected Expr quantifiedExpr(AST node) throws QueryException {
    int pos = 0;
    AST child = node.getChild(pos++);
    boolean someQuantified = (child.getType() == XQ.SomeQuantifier);
    Expr bindingSequenceExpr = quantifiedBindings(new Start(), node, pos);
    Function function;

    if (someQuantified) {
      function = BitFun.SOME_FUNC;
    } else {
      function = BitFun.EVERY_FUNC;
    }

    return new IfExpr(new FunctionExpr(node.getStaticContext(), function, bindingSequenceExpr), Bool.TRUE, Bool.FALSE);
  }

  protected Expr quantifiedBindings(Operator in, AST node, int pos) throws QueryException {
    AST child = node.getChild(pos++);

    if (child.getType() == XQ.QuantifiedBinding) {
      AST varBinding = child.getChild(0);
      QNm runVarName = (QNm) varBinding.getChild(0).getValue();
      SequenceType type = SequenceType.ITEM_SEQUENCE;
      if (varBinding.getChildCount() == 2) {
        type = sequenceType(varBinding.getChild(1));
      }
      Expr sourceExpr = expr(child.getChild(1), true);
      ForBind forBind = new ForBind(in, sourceExpr, false);

      Binding runVarBinding = table.bind(runVarName, type);
      Expr returnExpr = quantifiedBindings(forBind, node, pos);
      table.unbind();
      forBind.bindVariable(runVarBinding.isReferenced());
      forBind.bindPosition(false);
      return returnExpr;
    } else {
      return new PipeExpr(in, expr(child, true));
    }
  }

  protected Expr functionCall(AST node) throws QueryException {
    int childCount = node.getChildCount();
    QNm name = (QNm) node.getValue();

    if (JSONFun.JSON_PREFIX.equals(name.getPrefix())
        && "null".equals(name.getLocalName())) {
      return new Null();
    }

    Function function = ctx.getFunctions().resolve(name, childCount);
    Expr[] args;

    if (childCount > 0) {
      args = new Expr[childCount];
      for (int i = 0; i < childCount; i++) {
        AST arg = node.getChild(i);
        if (arg.getType() == XQ.ArgumentPlaceHolder) {
          throw new QueryException(ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
                                   "Partial function application is not supported yet");
        }
        args[i] = expr(arg, true);
      }
    } else if (function.getSignature().defaultCtxItemType() != null) {
      Expr contextItemRef = table.resolve(Bits.FS_DOT);
      args = new Expr[] { contextItemRef };
    } else {
      args = new Expr[0];
    }

    return new FunctionExpr(node.getStaticContext(), function, args);
  }

  protected Expr documentExpr(AST node) throws QueryException {
    final Binding binding = table.bind(Bits.FS_PARENT, SequenceType.ITEM);
    final Expr contentExpr = expr(node.getChild(0), false);
    table.unbind();
    final boolean bind = binding.isReferenced();
    return new DocumentExpr(contentExpr, bind);
  }

  protected Expr elementExpr(AST node) throws QueryException {
    int pos = 0;
    int nsCnt = 0;
    while (node.getChild(nsCnt).getType() == XQ.NamespaceDeclaration) {
      nsCnt++;
    }
    ElementExpr.NS[] ns = new ElementExpr.NS[nsCnt];
    for (int i = 0; i < nsCnt; i++) {
      AST nsDecl = node.getChild(pos++);
      if (nsDecl.getChildCount() == 2) {
        String prefix = nsDecl.getChild(0).getStringValue();
        String uri = nsDecl.getChild(1).getStringValue();
        ns[i] = new ElementExpr.NS(prefix, uri);
      } else {
        String uri = nsDecl.getChild(0).getStringValue();
        ns[i] = new ElementExpr.NS(null, uri);
      }
    }
    Expr nameExpr = expr(node.getChild(pos++), true);
    boolean appendOnly = appendOnly(node);
    boolean bind = false;
    Expr[] contentExpr;

    if (node.getChildCount() > 0) {
      Binding binding = table.bind(Bits.FS_PARENT, SequenceType.ITEM);
      contentExpr = contentSequence(node.getChild(pos));
      table.unbind();
      bind = binding.isReferenced();
    } else {
      contentExpr = new Expr[0];
    }

    StaticContext sctx = node.getStaticContext();
    return new ElementExpr(sctx, nameExpr, ns, contentExpr, bind, appendOnly);
  }

  protected Expr[] contentSequence(AST node) throws QueryException {
    int childCount = node.getChildCount();

    if (childCount == 0) {
      return new Expr[0];
    }

    int size = 0;
    Expr[] subExprs = new Expr[childCount];
    String merged = null;

    for (int i = 0; i < node.getChildCount(); i++) {
      AST child = node.getChild(i);
      if ((child.getType() == XQ.Str)) {
        merged = (merged == null) ? child.getStringValue() : merged + child.getStringValue();
      } else {
        if ((merged != null) && (!merged.isEmpty())) {
          subExprs[size++] = new Str(merged);
        }
        merged = null;
        subExprs[size++] = expr(child, true);
      }
    }

    if ((merged != null) && (!merged.isEmpty())) {
      subExprs[size++] = new Str(merged);
    }

    return Arrays.copyOf(subExprs, size);
  }

  protected Expr attributeExpr(AST node) throws QueryException {
    Expr nameExpr = expr(node.getChild(0), true);
    Expr[] contentExpr = (node.getChildCount() > 1) ? contentSequence(node.getChild(1)) : new Expr[0];
    StaticContext sctx = node.getStaticContext();
    return new AttributeExpr(sctx, nameExpr, contentExpr, appendOnly(node));
  }

  protected Expr commentExpr(AST node) throws QueryException {
    Expr contentExpr = expr(node.getChild(0), true);
    return new CommentExpr(contentExpr, appendOnly(node));
  }

  protected Expr textExpr(AST node) throws QueryException {
    Expr contentExpr = expr(node.getChild(0), true);
    return new TextExpr(contentExpr, appendOnly(node));
  }

  protected Expr piExpr(AST node) throws QueryException {
    Expr nameExpr = expr(node.getChild(0), true);
    Expr contentExpr = (node.getChildCount() > 1) ? expr(node.getChild(1), true) : new EmptyExpr();
    return new PIExpr(nameExpr, contentExpr, appendOnly(node));
  }

  private boolean appendOnly(AST node) throws QueryException {
    AST parent = node.getParent();
    if (parent == null) {
      return false;
    }
    if (parent.getType() == XQ.ContentSequence) {
      parent = parent.getParent();
    }

    boolean parentIsConstructor =
        (parent.getType() == XQ.CompElementConstructor) || (parent.getType() == XQ.CompDocumentConstructor);

    if (parentIsConstructor) {
      table.resolve(Bits.FS_PARENT);
    }

    return parentIsConstructor;
  }

  protected Expr andExpr(AST node) throws QueryException {
    Expr firstExpr = expr(node.getChild(0), true);
    Expr secondExpr = expr(node.getChild(1), true);
    return new AndExpr(firstExpr, secondExpr);
  }

  protected Expr orExpr(AST node) throws QueryException {
    Expr firstExpr = expr(node.getChild(0), true);
    Expr secondExpr = expr(node.getChild(1), true);
    return new OrExpr(firstExpr, secondExpr);
  }

  protected Expr ifExpr(AST node) throws QueryException {
    Expr condExpr = expr(node.getChild(0), true);
    Expr ifExpr = expr(node.getChild(1), false);
    Expr elseExpr = expr(node.getChild(2), false);

    if (ifExpr.isUpdating()) {
      if ((!elseExpr.isUpdating()) && (!elseExpr.isVacuous())) {
        throw new QueryException(ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE,
                                 "Single updating if branch is not allowed");
      }
    } else if (elseExpr.isUpdating()) {
      if ((!ifExpr.isUpdating()) && (!ifExpr.isVacuous())) {
        throw new QueryException(ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE,
                                 "Single updating else branch is not allowed");
      }
    }

    return new IfExpr(condExpr, ifExpr, elseExpr);
  }

  protected Expr switchExpr(AST node) throws QueryException {
    Expr opExpr = expr(node.getChild(0), true);
    Expr[][] cases = new Expr[node.getChildCount() - 2][];
    for (int i = 1; i < node.getChildCount() - 1; i++) {
      AST caseClause = node.getChild(i);
      Expr[] caseOps = new Expr[caseClause.getChildCount()];
      for (int j = 0; j < caseClause.getChildCount(); j++) {
        caseOps[j] = expr(caseClause.getChild(j), false);
      }
      cases[i - 1] = caseOps;
    }
    Expr dftExpr = expr(node.getChild(node.getChildCount() - 1), false);
    return new SwitchExpr(opExpr, cases, dftExpr);
  }

  protected Expr variableRefExpr(AST node) throws QueryException {
    return table.resolve((QNm) node.getValue());
  }

  protected Expr rangeExpr(AST node) throws QueryException {
    Expr firstArg = expr(node.getChild(0), true);
    Expr secondArg = expr(node.getChild(1), true);
    return new RangeExpr(firstArg, secondArg);
  }

  protected Expr comparisonExpr(AST node) throws QueryException {
    Expr firstArg = expr(node.getChild(1), true);
    Expr secondArg = expr(node.getChild(2), true);
    AST cmpNode = node.getChild(0);
    return switch (cmpNode.getType()) {
      case XQ.ValueCompEQ -> new VCmpExpr(Cmp.eq, firstArg, secondArg);
      case XQ.ValueCompGE -> new VCmpExpr(Cmp.ge, firstArg, secondArg);
      case XQ.ValueCompLE -> new VCmpExpr(Cmp.le, firstArg, secondArg);
      case XQ.ValueCompLT -> new VCmpExpr(Cmp.lt, firstArg, secondArg);
      case XQ.ValueCompGT -> new VCmpExpr(Cmp.gt, firstArg, secondArg);
      case XQ.ValueCompNE -> new VCmpExpr(Cmp.ne, firstArg, secondArg);
      case XQ.GeneralCompEQ -> new GCmpExpr(Cmp.eq, firstArg, secondArg);
      case XQ.GeneralCompGE -> new GCmpExpr(Cmp.ge, firstArg, secondArg);
      case XQ.GeneralCompLE -> new GCmpExpr(Cmp.le, firstArg, secondArg);
      case XQ.GeneralCompLT -> new GCmpExpr(Cmp.lt, firstArg, secondArg);
      case XQ.GeneralCompGT -> new GCmpExpr(Cmp.gt, firstArg, secondArg);
      case XQ.GeneralCompNE -> new GCmpExpr(Cmp.ne, firstArg, secondArg);
      case XQ.NodeCompIs -> new NodeCmpExpr(NodeCmp.Is, firstArg, secondArg);
      case XQ.NodeCompFollows -> new NodeCmpExpr(NodeCmp.Following, firstArg, secondArg);
      case XQ.NodeCompPrecedes -> new NodeCmpExpr(NodeCmp.Preceding, firstArg, secondArg);
      default -> throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                                          "Unexpected comparison: '%s'",
                                          cmpNode);
    };
  }

  protected Expr arithmeticExpr(AST node) throws QueryException {
    final ArithmeticOp op = switch (node.getChild(0).getType()) {
      case XQ.AddOp -> ArithmeticOp.PLUS;
      case XQ.SubtractOp -> ArithmeticOp.MINUS;
      case XQ.MultiplyOp -> ArithmeticOp.MULT;
      case XQ.DivideOp -> ArithmeticOp.DIV;
      case XQ.IDivideOp -> ArithmeticOp.IDIV;
      case XQ.ModulusOp -> ArithmeticOp.MOD;
      default -> null;
    };

    final Expr firstArg = expr(node.getChild(1), true);
    final Expr secondArg = expr(node.getChild(2), true);
    return new ArithmeticExpr(op, firstArg, secondArg);
  }

  /*
   * The compilation of path expressions is a bit tricky. A path of the form
   * E1/E2/../EN must be evaluated with "left-deep semantics", i.e.,
   * ((E1/E2)/..)/EN and each step EI needs to have the current context item
   * (focus, $fs:dot) bound, from the preceding step EI-1.
   */
  protected Expr pathExpr(AST node) throws QueryException {
    Expr e1 = expr(node.getChild(0), true);
    for (int i = 1; i < node.getChildCount(); i++) {
      Binding itemBinding = table.bind(Bits.FS_DOT, SequenceType.NODE);
      Binding posBinding = table.bind(Bits.FS_POSITION, SequenceType.INTEGER);
      Binding sizeBinding = table.bind(Bits.FS_LAST, SequenceType.INTEGER);
      AST step = node.getChild(i);
      Expr e2 = expr(step, true);

      table.unbind();
      table.unbind();
      table.unbind();

      boolean bindItem = itemBinding.isReferenced();
      boolean bindPos = posBinding.isReferenced();
      boolean bindSize = sizeBinding.isReferenced();
      boolean lastStep = (i + 1 == node.getChildCount());
      boolean skipDDO = step.checkProperty("skipDDO");
      boolean checkInput = step.checkProperty("checkInput");
      e1 = new PathStepExpr(e1, e2, bindItem, bindPos, bindSize, lastStep, skipDDO, checkInput);
    }
    return e1;
  }

  protected Expr stepExpr(AST node) throws QueryException {
    AST child = node.getChild(0);
    Accessor axis;

    if (child.getType() == XQ.AxisSpec) {
      axis = axis(child.getChild(0));
      child = node.getChild(1);
    } else {
      axis = Accessor.CHILD;
    }

    Expr in = table.resolve(Bits.FS_DOT);
    NodeType test = nodeTest(child, axis.getAxis());

    int noOfPredicates = Math.max(node.getChildCount() - 2, 0);
    Expr[] filter = new Expr[noOfPredicates];
    boolean[] bindItem = new boolean[noOfPredicates];
    boolean[] bindPos = new boolean[noOfPredicates];
    boolean[] bindSize = new boolean[noOfPredicates];

    for (int i = 0; i < noOfPredicates; i++) {
      Binding itemBinding = table.bind(Bits.FS_DOT, SequenceType.ITEM);
      Binding posBinding = table.bind(Bits.FS_POSITION, SequenceType.INTEGER);
      Binding sizeBinding = table.bind(Bits.FS_LAST, SequenceType.INTEGER);
      filter[i] = expr(node.getChild(2 + i).getChild(0), true);
      table.unbind();
      table.unbind();
      table.unbind();
      bindItem[i] = itemBinding.isReferenced();
      bindPos[i] = posBinding.isReferenced();
      bindSize[i] = sizeBinding.isReferenced();
    }

    return new StepExpr(axis, test, in, filter, bindItem, bindPos, bindSize);
  }

  protected Accessor axis(AST node) throws QueryException {
    return switch (node.getType()) {
      case XQ.CHILD -> Accessor.CHILD;
      case XQ.DESCENDANT -> Accessor.DESCENDANT;
      case XQ.DESCENDANT_OR_SELF -> Accessor.DESCENDANT_OR_SELF;
      case XQ.ATTRIBUTE -> Accessor.ATTRIBUTE;
      case XQ.PARENT -> Accessor.PARENT;
      case XQ.ANCESTOR -> Accessor.ANCESTOR;
      case XQ.ANCESTOR_OR_SELF -> Accessor.ANCESTOR_OR_SELF;
      case XQ.FOLLOWING_SIBLING -> Accessor.FOLLOWING_SIBLING;
      case XQ.FOLLOWING -> Accessor.FOLLOWING;
      case XQ.PRECEDING -> Accessor.PRECEDING;
      case XQ.PRECEDING_SIBLING -> Accessor.PRECEDING_SIBLING;
      case XQ.SELF -> Accessor.SELF;
      case XQ.NEXT -> Accessor.NEXT;
      case XQ.PREVIOUS -> Accessor.PREVIOUS;
      case XQ.FUTURE -> Accessor.FUTURE;
      case XQ.FUTURE_OR_SELF -> Accessor.FUTURE_OR_SELF;
      case XQ.PAST -> Accessor.PAST;
      case XQ.PAST_OR_SELF -> Accessor.PAST_OR_SELF;
      case XQ.FIRST -> Accessor.FIRST;
      case XQ.LAST -> Accessor.LAST;
      case XQ.ALL_TIMES -> Accessor.ALL_TIME;
      default -> throw new QueryException(ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
                                          "Suport for document axis '%s' not implemented yet",
                                          node);
    };
  }

  protected SequenceType sequenceType(AST node) throws QueryException {
    AST child = node.getChild(0);
    if (child.getType() == XQ.EmptySequenceType) {
      return SequenceType.EMPTY_SEQUENCE;
    }

    ItemType itemType = itemType(child);
    Cardinality cardinality = Cardinality.One;

    if (node.getChildCount() == 2) {
      cardinality = switch (node.getChild(1).getType()) {
        case XQ.CardinalityOneOrMany -> Cardinality.OneOrMany;
        case XQ.CardinalityZeroOrMany -> Cardinality.ZeroOrMany;
        case XQ.CardinalityZeroOrOne -> Cardinality.ZeroOrOne;
        default -> Cardinality.One;
      };
    }

    return new SequenceType(itemType, cardinality);
  }

  protected ItemType itemType(AST node) throws QueryException {
    return switch (node.getType()) {
      case XQ.ItemType -> AnyItemType.ANY;
      case XQ.AtomicOrUnionType -> atomicOrUnionType(node);
      case XQ.StructuredItemTest -> new AnyStructuredItemType();
      case XQ.KindTestRecord -> new RecordType();
      case XQ.KindTestArray -> new ArrayType();
      case XQ.KindTestNull -> new NullType();
      case XQ.JsonItemTest -> new AnyJsonItemType();
      default -> kindTest(node);
    };
  }

  protected ItemType atomicOrUnionType(AST node) throws QueryException {
    Type type = resolveType((QNm) node.getChild(0).getValue(), false);
    return new AtomicType(type);
  }

  protected NodeType nodeTest(AST node, Axis axis) throws QueryException {
    if (node.getType() == XQ.NameTest) {
      return nameTest(node, axis);
    } else {
      return kindTest(node);
    }
  }

  protected NodeType kindTest(AST node) throws QueryException {
    switch (node.getType()) {
      case XQ.KindTestAnyKind:
        return AnyNodeType.ANY_NODE;
      case XQ.KindTestText:
        return new TextType();
      case XQ.KindTestElement:
        return elementTest(node);
      case XQ.KindTestAttribute:
        return attributeTest(node);
      case XQ.KindTestComment:
        return new CommentType();
      case XQ.KindTestDocument:
        return documentTest(node);
      case XQ.KindTestPi:
        if (node.getChildCount() == 0) {
          return new PIType();
        } else {
          return new PIType(node.getChild(0).getStringValue());
        }
      case XQ.KindTestSchemaElement:
        return schemaElementTest(node);
      case XQ.KindTestSchemaAttribute:
        return schemaAttributeTest(node);
      default:
        throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR, "KindTest translation not implemented yet.");
    }
  }

  protected AttributeType schemaAttributeTest(AST child) throws QueryException {
    QNm qname = (QNm) child.getChild(0).getValue();
    return new AttributeType(qname, ctx.getTypes().resolveSchemaType(qname));
  }

  protected ElementType schemaElementTest(AST child) throws QueryException {
    QNm qname = (QNm) child.getChild(0).getValue();
    return new ElementType(qname, ctx.getTypes().resolveSchemaType(qname));
  }

  protected DocumentType documentTest(AST child) throws QueryException {
    if (child.getChildCount() == 0)
      return new DocumentType();
    else if (child.getChild(0).getType() == XQ.KindTestElement)
      return new DocumentType(elementTest(child.getChild(0)));
    else
      return new DocumentType(schemaElementTest(child.getChild(0)));
  }

  protected AttributeType attributeTest(AST child) throws QueryException {
    if (child.getChildCount() == 0)
      return new AttributeType();
    else if (child.getChildCount() == 1)
      return new AttributeType(qNameOrWildcard(child.getChild(0)));
    else
      return new AttributeType(qNameOrWildcard(child.getChild(0)),
                               resolveType((QNm) child.getChild(1).getValue(), false));
  }

  protected ElementType elementTest(AST child) throws QueryException {
    if (child.getChildCount() == 0)
      return new ElementType();
    else if (child.getChildCount() == 1)
      return new ElementType(qNameOrWildcard(child.getChild(0)));
    else
      return new ElementType(qNameOrWildcard(child.getChild(0)),
                             resolveType((QNm) child.getChild(1).getValue(), false));
  }

  protected QNm qNameOrWildcard(AST name) throws QueryException {
    return (name.getType() == XQ.Wildcard) ? null : (QNm) name.getValue();
  }

  protected NodeType nameTest(AST child, Axis axis) throws QueryException {
    AST name = child.getChild(0);
    switch (name.getType()) {
      case XQ.Wildcard:
        if (axis != Axis.ATTRIBUTE) {
          return new ElementType(null);
        } else {
          return new AttributeType(null);
        }
      case XQ.NSWildcardNameTest:
        return new NSWildcardNameTest((axis == Axis.ATTRIBUTE) ? Kind.ATTRIBUTE : Kind.ELEMENT, name.getStringValue());
      case XQ.NSNameWildcardTest:
        return new NSNameWildcardTest((axis == Axis.ATTRIBUTE) ? Kind.ATTRIBUTE : Kind.ELEMENT,
                                      ctx.getNamespaces().resolve(name.getStringValue()));
      default:
        if (axis != Axis.ATTRIBUTE) {
          return new ElementType((QNm) name.getValue());
        } else {
          return new AttributeType((QNm) name.getValue());
        }
    }
  }

  protected Type resolveType(QNm qname, boolean atomic) throws QueryException {
    if (atomic) {
      return ctx.getTypes().resolveAtomicType(qname);
    } else {
      return ctx.getTypes().resolveType(qname);
    }
  }

  protected Expr sequenceExpr(AST node) throws QueryException {
    boolean allVacouousOrUpdating = false;
    Expr[] subExpr = new Expr[node.getChildCount()];
    for (int i = 0; i < node.getChildCount(); i++) {
      subExpr[i] = expr(node.getChild(i), false);

      if (subExpr[i].isUpdating()) {
        if ((!allVacouousOrUpdating) && (i > 0)) {
          // check if all preceding expressions are vacuous
          for (int j = 0; j < i; j++) {
            if (!subExpr[j].isVacuous()) {
              throw new QueryException(ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE,
                                       "Illegal nested updating expression.");
            }
          }
        }
        allVacouousOrUpdating = true;
      } else if (allVacouousOrUpdating) {
        if (!subExpr[i].isVacuous()) {
          throw new QueryException(ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE, "Illegal nested updating expression.");
        }
      }
    }
    return new SequenceExpr(subExpr);
  }

  protected Expr flowrExpr(AST node) throws QueryException {
    final int childCount = node.getChildCount();
    final ClauseBinding cb = flowrClause(new ClauseBinding(null, new Start()), node, 0, childCount - 2);
    final Expr returnExpr = expr(node.getChild(childCount - 1).getChild(0), false);
    cb.unbind();
    final Expr pipeExpr = new PipeExpr(cb.operator, returnExpr);
    return pipeExpr;
  }

  private ClauseBinding flowrClause(ClauseBinding in, AST node, int pos, int maxPos) throws QueryException {
    ClauseBinding cb = in;

    while (pos <= maxPos) {
      final AST clause = node.getChild(pos++);
      cb = switch (clause.getType()) {
        case XQ.ForClause -> forClause(clause, cb);
        case XQ.LetClause -> letClause(clause, cb);
        case XQ.WhereClause -> whereClause(clause, cb);
        case XQ.OrderByClause -> orderByClause(clause, cb);
        case XQ.CountClause -> countClause(clause, cb);
        case XQ.GroupByClause -> groupByClause(clause, cb);
        default -> throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                                            "Unknown flowr clause type: %s",
                                            clause);
      };
    }

    return cb;
  }

  protected ClauseBinding countClause(AST node, ClauseBinding in) throws QueryException {
    AST countVarDecl = node.getChild(0);
    QNm posVarName = (QNm) countVarDecl.getChild(0).getValue();
    SequenceType posVarType = SequenceType.ITEM_SEQUENCE;
    if (countVarDecl.getChildCount() == 2) {
      posVarType = sequenceType(countVarDecl.getChild(1));
    }
    final Binding binding = table.bind(posVarName, posVarType);
    final Count count = new Count(in.operator);

    return new ClauseBinding(in, count, binding) {
      @Override
      public void unbind() {
        super.unbind();
        count.bind(binding.isReferenced());
        table.unbind();
      }
    };
  }

  protected ClauseBinding orderByClause(AST node, ClauseBinding in) throws QueryException {
    int orderBySpecCount = node.getChildCount();
    Expr[] orderByExprs = new Expr[orderBySpecCount];
    OrderModifier[] orderBySpec = new OrderModifier[orderBySpecCount];
    for (int i = 0; i < orderBySpecCount; i++) {
      AST orderBy = node.getChild(i);
      orderByExprs[i] = expr(orderBy.getChild(0), true);
      orderBySpec[i] = orderModifier(orderBy);
    }
    OrderBy orderBy = new OrderBy(in.operator, orderByExprs, orderBySpec);
    return new ClauseBinding(in, orderBy);
  }

  protected OrderModifier orderModifier(AST orderBy) {
    boolean asc = true;
    boolean emptyLeast = true;
    String collation = null;
    for (int i = 1; i < orderBy.getChildCount(); i++) {
      AST modifier = orderBy.getChild(i);
      if (modifier.getType() == XQ.OrderByKind) {
        AST direction = modifier.getChild(0);
        asc = (direction.getType() == XQ.ASCENDING);
      } else if (modifier.getType() == XQ.OrderByEmptyMode) {
        AST empty = modifier.getChild(0);
        emptyLeast = (empty.getType() == XQ.LEAST);
      } else if (modifier.getType() == XQ.Collation) {
        collation = modifier.getChild(0).getStringValue();
      }
    }
    return new OrderModifier(asc, emptyLeast, collation);
  }

  protected ClauseBinding groupByClause(AST node, ClauseBinding in) throws QueryException {
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
    GroupBy groupBy = new GroupBy(in.operator, dftAgg, addAggs, grpSpecCnt, sequential);
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
    for (final AggregateBinding bnd : bnds) {
      table.bind(bnd.aggVar, bnd.aggVarType);
      // fake binding
      table.resolve(bnd.aggVar);
    }
    return new ClauseBinding(in, groupBy);
  }

  protected Aggregate aggregate(AST node) throws QueryException {
    return switch (node.getType()) {
      case XQ.SequenceAgg -> Aggregate.SEQUENCE;
      case XQ.CountAgg -> Aggregate.COUNT;
      case XQ.SumAgg -> Aggregate.SUM;
      case XQ.AvgAgg -> Aggregate.AVG;
      case XQ.MinAgg -> Aggregate.MIN;
      case XQ.MaxAgg -> Aggregate.MAX;
      case XQ.SingleAgg -> Aggregate.SINGLE;
      default -> throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR, "Unknown aggregate type: %s", node);
    };
  }

  protected ClauseBinding whereClause(AST node, ClauseBinding in) throws QueryException {
    Expr expr = anyExpr(node.getChild(0));
    Select select = new Select(in.operator, expr);
    return new ClauseBinding(in, select);
  }

  protected ClauseBinding letClause(AST node, ClauseBinding in) throws QueryException {
    int letClausePos = 0;
    AST letClause = node;
    AST letVarDecl = letClause.getChild(letClausePos++);
    QNm letVarName = (QNm) letVarDecl.getChild(0).getValue();
    SequenceType letVarType = SequenceType.ITEM_SEQUENCE;
    if (letVarDecl.getChildCount() == 2) {
      letVarType = sequenceType(letVarDecl.getChild(1));
    }
    Expr sourceExpr = expr(letClause.getChild(letClausePos++), true);
    final Binding binding = table.bind(letVarName, letVarType);
    final LetBind letBind = new LetBind(in.operator, sourceExpr);

    return new ClauseBinding(in, letBind, binding) {
      @Override
      public void unbind() {
        super.unbind();
        letBind.bind(binding.isReferenced());
        table.unbind();
      }
    };
  }

  protected ClauseBinding forClause(AST node, ClauseBinding in) throws QueryException {
    QNm posVarName = null;
    int forClausePos = 0;
    final AST forClause = node;
    final AST runVarDecl = forClause.getChild(forClausePos++);
    final QNm runVarName = (QNm) runVarDecl.getChild(0).getValue();

    SequenceType runVarType = SequenceType.ITEM_SEQUENCE;

    if (runVarDecl.getChildCount() == 2) {
      runVarType = sequenceType(runVarDecl.getChild(1));
    }

    AST posBindingOrSourceExpr = forClause.getChild(forClausePos++);

    if (posBindingOrSourceExpr.getType() == XQ.TypedVariableBinding) {
      posVarName = (QNm) posBindingOrSourceExpr.getChild(0).getValue();
      posBindingOrSourceExpr = forClause.getChild(forClausePos);
    }
    final Expr sourceExpr = expr(posBindingOrSourceExpr, true);

    final Binding runVarBinding = table.bind(runVarName, runVarType);
    final Binding posBinding = (posVarName != null) ? table.bind(posVarName, SequenceType.INTEGER) : null;
    final ForBind forBind = new ForBind(in.operator, sourceExpr, false);

    return new ClauseBinding(in, forBind, runVarBinding, posBinding) {
      @Override
      public void unbind() {
        super.unbind();
        if (posBinding != null) {
          forBind.bindPosition(posBinding.isReferenced());
          table.unbind();
        }
        forBind.bindVariable(runVarBinding.isReferenced());
        table.unbind();
      }
    };
  }

  // BEGIN Custom array syntax extension
  protected Expr arrayAccessExpr(AST node) throws QueryException {
    Expr expr = expr(node.getChild(0), true);
    Expr index = expr(node.getChild(1), true);
    return new ArrayAccessExpr(expr, index);
  }

  protected Expr arrayExpr(AST node) throws QueryException {
    int cnt = node.getChildCount();
    boolean[] flatten = new boolean[cnt];
    Expr[] expr = new Expr[cnt];
    for (int i = 0; i < cnt; i++) {
      AST field = node.getChild(i);
      flatten[i] = (field.getType() == XQ.FlattenedField);
      expr[i] = expr(field.getChild(0), true);
    }
    return new ArrayExpr(expr, flatten);
  }

  // END Custom array syntax extension

  // BEGIN Custom record syntax extension
  protected Expr recordExpr(AST node) throws QueryException {
    int cnt = node.getChildCount();
    Field[] fields = new Field[cnt];
    for (int i = 0; i < cnt; i++) {
      AST field = node.getChild(i);
      if (field.getType() == XQ.KeyValueField) {
        fields[i] = new KeyValueField(expr(field.getChild(0), true), expr(field.getChild(1), true));
      } else {
        fields[i] = new RecordField(expr(field, true));
      }
    }
    return new RecordExpr(fields);
  }

  protected Expr derefExpr(AST node) throws QueryException {
    Expr record = expr(node.getChild(0), true);
    Expr[] fields = new Expr[node.getChildCount() - 1];
    for (int i = 1; i < node.getChildCount(); i++) {
      fields[i - 1] = expr(node.getChild(i), true);
    }
    return new DerefExpr(record, fields);
  }

  protected Expr projectionExpr(AST node) throws QueryException {
    final Expr record = expr(node.getChild(0), true);
    final Expr[] fields = new Expr[node.getChildCount() - 1];
    for (int i = 1; i < node.getChildCount(); i++) {
      fields[i - 1] = expr(node.getChild(i), true);
    }
    return new ProjectionExpr(record, fields);
  }
  // END Custom record syntax extension
}