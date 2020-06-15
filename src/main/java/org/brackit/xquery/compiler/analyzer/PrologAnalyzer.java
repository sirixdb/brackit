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
package org.brackit.xquery.compiler.analyzer;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.expr.DeclVariable;
import org.brackit.xquery.expr.DefaultCtxItem;
import org.brackit.xquery.function.UDF;
import org.brackit.xquery.module.DecimalFormat;
import org.brackit.xquery.module.Module;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.type.AnyItemType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.ItemType;
import org.brackit.xquery.xdm.type.SequenceType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sebastian Baechle
 */
public class PrologAnalyzer extends AbstractAnalyzer {

  protected final List<ForwardDeclaration> decls;
  protected final List<Import> imports;
  protected final Module module;

  boolean declaredBoundarySpace = false;
  boolean declaredBaseURI = false;
  boolean declaredConstructionMode = false;
  boolean declaredOrderingMode = false;
  boolean declaredEmptyOrder = false;
  boolean declaredCopyNamespaces = false;
  boolean declaredDecimalFormatDefault = false;

  public static class Import {
    final String uri;
    final String[] locs;

    public Import(String uri, String[] locs) {
      super();
      this.uri = uri;
      this.locs = locs;
    }

    public String getURI() {
      return uri;
    }

    public String[] getLocations() {
      return locs;
    }
  }

  public PrologAnalyzer(Module module, AST prolog) throws QueryException {
    this.module = module;
    this.sctx = module.getStaticContext();
    this.decls = new ArrayList<>();
    this.imports = new ArrayList<>();
    prolog(prolog);
  }

  public List<Import> getImports() {
    return imports;
  }

  public List<ForwardDeclaration> getDeclarations() {
    return decls;
  }

  protected boolean prolog(AST prolog) throws QueryException {
    if (prolog.getType() != XQ.Prolog) {
      return false;
    }
    for (int i = 0; i < prolog.getChildCount(); i++) {
      AST decl = prolog.getChild(i);
      boolean ok = (defaultNamespaceDecl(decl) || setter(decl) || namespaceDecl(decl) || importDecl(decl)
          || contextItemDecl(decl) || annotatedDecl(decl) || optionDecl(decl));
      if (!ok) {
        throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR, "Illegal prolog declaration: %s", decl.getStringValue());
      }
    }
    return true;
  }

  private boolean defaultNamespaceDecl(AST decl) throws QueryException {
    if (decl.getType() == XQ.DefaultElementNamespace) {
      String uri = decl.getChild(0).getStringValue();
      sctx.getNamespaces().setDefaultElementNamespace(uri);
      return true;
    } else if (decl.getType() == XQ.DefaultFunctionNamespace) {
      String uri = decl.getChild(0).getStringValue();
      sctx.getNamespaces().setDefaultFunctionNamespace(uri);
      return true;
    } else {
      return false;
    }
  }

  private boolean setter(AST decl) throws QueryException {
    return (boundarySpaceDecl(decl) || defaultCollationDecl(decl) || baseURIDecl(decl) || constructionDecl(decl)
        || orderingModeDecl(decl) || emptyOrderDecl(decl)
        /* Begin XQuery Update Facility 1.0 */ || revalidationDecl(decl)
        /* Begin XQuery Update Facility 1.0 */ || copyNamespacesDecl(decl) || decimalFormatDecl(decl));
  }

  private boolean boundarySpaceDecl(AST decl) throws QueryException {
    if (decl.getType() != XQ.BoundarySpaceDeclaration) {
      return false;
    }
    if (declaredBoundarySpace) {
      throw new QueryException(ErrorCode.ERR_BOUNDARY_SPACE_ALREADY_DECLARED, "Boundary-space already declared");
    }
    AST mode = decl.getChild(0);
    sctx.setBoundarySpaceStrip(mode.getType() != XQ.BoundarySpaceModePreserve);
    declaredBoundarySpace = true;
    return true;
  }

  private boolean defaultCollationDecl(AST decl) throws QueryException {
    if (decl.getType() != XQ.CollationDeclaration) {
      return false;
    }
    String col = decl.getChild(0).getStringValue();
    if (!col.equals(StaticContext.UNICODE_COLLATION)) {
      throw new QueryException(ErrorCode.ERR_UNSUPPORTED_COLLATION, "Unsupported collation: %s", col);
    }

    sctx.setDefaultCollation(col);
    return true;
  }

  private boolean baseURIDecl(AST decl) throws QueryException {
    if (decl.getType() != XQ.BaseURIDeclaration) {
      return false;
    }
    if (declaredBaseURI) {
      throw new QueryException(ErrorCode.ERR_BASE_URI_ALREADY_DECLARED, "Base URI already declared");
    }
    String uri = decl.getChild(0).getStringValue();
    sctx.setBaseURI(new AnyURI(uri));
    declaredBaseURI = true;
    return true;
  }

  private boolean constructionDecl(AST decl) throws QueryException {
    if (decl.getType() != XQ.ConstructionDeclaration) {
      return false;
    }
    if (declaredConstructionMode) {
      throw new QueryException(ErrorCode.ERR_CONSTRUCTION_ALREADY_DECLARED, "Construction mode already declared");
    }
    AST mode = decl.getChild(0);
    sctx.setConstructionModeStrip(mode.getType() != XQ.ConstructionModePreserve);
    declaredConstructionMode = true;
    return true;
  }

  private boolean orderingModeDecl(AST decl) throws QueryException {
    if (decl.getType() != XQ.OrderingModeDeclaration) {
      return false;
    }
    if (declaredOrderingMode) {
      throw new QueryException(ErrorCode.ERR_ORDERING_MODE_ALREADY_DECLARED, "Ordering mode already declared");
    }
    AST mode = decl.getChild(0);
    sctx.setOrderingModeOrdered(mode.getType() == XQ.OrderingModeOrdered);
    declaredOrderingMode = true;
    return true;
  }

  private boolean emptyOrderDecl(AST decl) throws QueryException {
    if (decl.getType() != XQ.EmptyOrderDeclaration) {
      return false;
    }
    if (declaredEmptyOrder) {
      throw new QueryException(ErrorCode.ERR_EMPTY_ORDER_ALREADY_DECLARED, "Empty order mode already declared");
    }
    AST mode = decl.getChild(0);
    if (mode.getType() == XQ.EmptyOrderModeGreatest) {
      sctx.setEmptyOrderGreatest(true);
    } else {
      sctx.setEmptyOrderGreatest(false);
    }
    declaredEmptyOrder = true;
    return true;
  }

  // Begin XQuery Update Facility 1.0
  private boolean revalidationDecl(AST decl) throws QueryException {
    if (decl.getType() != XQ.RevalidationDeclaration) {
      return false;
    }
    // TODO
    throw new QueryException(ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR);
  }

  // End XQuery Update Facility 1.0

  private boolean copyNamespacesDecl(AST decl) throws QueryException {
    if (decl.getType() != XQ.CopyNamespacesDeclaration) {
      return false;
    }
    if (declaredCopyNamespaces) {
      throw new QueryException(ErrorCode.ERR_COPY_NAMESPACES_ALREADY_DECLARED, "Copy-namespaces already declared");
    }

    AST mode = decl.getChild(0);
    if (preserveMode(mode)) {
      if (decl.getChildCount() == 2) {
        inheritMode(mode);
      }
    } else {
      inheritMode(mode);
    }
    declaredCopyNamespaces = true;
    return true;
  }

  private boolean preserveMode(AST mode) throws QueryException {
    if (mode.getType() == XQ.CopyNamespacesPreserveModePreserve) {
      sctx.setCopyNSPreserve(true);
    } else if (mode.getType() == XQ.CopyNamespacesPreserveModeNoPreserve) {
      sctx.setCopyNSPreserve(false);
    } else {
      return false;
    }
    return true;
  }

  private boolean inheritMode(AST mode) throws QueryException {
    if (mode.getType() == XQ.CopyNamespacesInheritModeInherit) {
      sctx.setCopyNSInherit(true);
    } else if (mode.getType() == XQ.CopyNamespacesInheritModeNoInherit) {
      sctx.setCopyNSInherit(false);
    } else {
      return false;
    }
    return true;
  }

  private boolean decimalFormatDecl(AST decl) throws QueryException {
    if (decl.getType() != XQ.DecimalFormatDeclaration) {
      return false;
    }
    AST format = decl.getChild(0);
    if (format.getType() == XQ.DecimalFormatDefault) {
      if (declaredDecimalFormatDefault) {
        throw new QueryException(ErrorCode.ERR_DECIMAL_FORMAT_ALREADY_DECLARED,
                                 "Default decimal-format already declared");
      }
      declaredDecimalFormatDefault = true;
      DecimalFormat df = new DecimalFormat();
      for (int i = 1; i < decl.getChildCount(); i++) {
        dfProperty(df, decl.getChild(i));
      }
      sctx.setDefaultDecimalFormat(df);
    } else {
      QNm name = (QNm) format.getValue();
      // expand and update AST
      name = expand(name, DefaultNS.EMPTY);
      format.setValue(name);
      if (sctx.getDecimalFormat(name) != null) {
        throw new QueryException(ErrorCode.ERR_DECIMAL_FORMAT_ALREADY_DECLARED,
                                 "Decimal-format already declared: %s",
                                 name);
      }
      DecimalFormat df = new DecimalFormat();
      for (int i = 1; i < decl.getChildCount(); i++) {
        dfProperty(df, decl.getChild(i));
      }
      sctx.setDecimalFormat(name, df);
    }
    return true;
  }

  private boolean dfProperty(DecimalFormat df, AST dfProperty) {
    int type = dfProperty.getType();
    if (type == XQ.DecimalFormatPropertyDecimalSeparator) {
      df.setDecimalSeparator(dfProperty.getStringValue());
    } else if (type == XQ.DecimalFormatPropertyGroupingSeparator) {
      df.setGroupingSeparator(dfProperty.getStringValue());
    } else if (type == XQ.DecimalFormatPropertyInfinity) {
      df.setInfinity(dfProperty.getStringValue());
    } else if (type == XQ.DecimalFormatPropertyMinusSign) {
      df.setMinusSign(dfProperty.getStringValue());
    } else if (type == XQ.DecimalFormatPropertyNaN) {
      df.setNaN(dfProperty.getStringValue());
    } else if (type == XQ.DecimalFormatPropertyPercent) {
      df.setPercent(dfProperty.getStringValue());
    } else if (type == XQ.DecimalFormatPropertyPerMille) {
      df.setPerMille(dfProperty.getStringValue());
    } else if (type == XQ.DecimalFormatPropertyZeroDigit) {
      df.setZeroDigit(dfProperty.getStringValue());
    } else if (type == XQ.DecimalFormatPropertyDigit) {
      df.setDigitSign(dfProperty.getStringValue());
    } else if (type == XQ.DecimalFormatPropertyPatternSeparator) {
      df.setPatternSeparator(dfProperty.getStringValue());
    } else {
      return false;
    }
    return true;
  }

  private boolean namespaceDecl(AST decl) throws QueryException {
    if (decl.getType() != XQ.NamespaceDeclaration) {
      return false;
    }
    String prefix = decl.getChild(0).getStringValue();
    String uri = decl.getChild(1).getStringValue();

    if ((Namespaces.XML_PREFIX.equals(prefix)) || (Namespaces.XMLNS_PREFIX.equals(prefix))) {
      throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
                               "The prefix '%s' must not be used in a namespace declaration",
                               prefix);
    }
    if ((Namespaces.XML_NSURI.equals(uri)) || (Namespaces.XMLNS_NSURI.equals(uri))) {
      throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
                               "The URI '%s' must not be used in a namespace declaration",
                               uri);
    }
    Namespaces ns = sctx.getNamespaces();
    if ((ns.resolve(prefix) != null) && (!ns.isPredefined(prefix))) {
      throw new QueryException(ErrorCode.ERR_MULTIPLE_NS_BINDINGS_FOR_PREFIX,
                               "Namespace prefix '%s' is already bound to '%s",
                               prefix,
                               uri);
    }
    ns.declare(prefix, uri);
    return true;
  }

  private boolean importDecl(AST decl) throws QueryException {
    return (schemaImport(decl) || moduleImport(decl));
  }

  private boolean schemaImport(AST decl) throws QueryException {
    if (decl.getType() != XQ.SchemaImport) {
      return false;
    }
    throw new QueryException(ErrorCode.ERR_SCHEMA_IMPORT_FEATURE_NOT_SUPPORTED, "Schema import is not supported.");
  }

  private boolean moduleImport(AST decl) throws QueryException {
    if (decl.getType() != XQ.ModuleImport) {
      return false;
    }
    AST ns = decl.getChild(0);
    String uri;
    if (ns.getType() == XQ.NamespaceDeclaration) {
      String prefix = ns.getChild(0).getStringValue();
      uri = ns.getChild(1).getStringValue();
      if ((Namespaces.XML_PREFIX.equals(prefix)) || (Namespaces.XMLNS_PREFIX.equals(prefix))) {
        throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
                                 "The prefix '%s' must not be used for a module import",
                                 prefix);
      }
      if (sctx.getNamespaces().resolve(prefix) != null) {
        throw new QueryException(ErrorCode.ERR_MULTIPLE_NS_BINDINGS_FOR_PREFIX,
                                 "Namespace prefix '%s' is already bound to '%s",
                                 prefix,
                                 uri);
      }
      // declare module namespace prefix
      sctx.getNamespaces().declare(prefix, uri);
    } else {
      uri = ns.getStringValue();
    }
    if (uri.isEmpty()) {
      throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL, "Module import with empty target namespace");
    }
    for (Import i : imports) {
      if (i.getURI().equals(uri)) {
        throw new QueryException(ErrorCode.ERR_MULTIPLE_IMPORTS_IN_SAME_NS,
                                 "Multiple imports of module namespace: %s",
                                 uri);
      }
    }
    String[] locs = new String[decl.getChildCount() - 1];
    for (int i = 0; i < locs.length; i++) {
      locs[i] = decl.getChild(i + 1).getStringValue();
    }
    imports.add(new Import(uri, locs));
    return true;
  }

  private boolean contextItemDecl(AST decl) throws QueryException {
    if (decl.getType() != XQ.ContextItemDeclaration) {
      return false;
    }
    DefaultCtxItem var = module.getVariables().getDftCtxItem();
    ItemType type = itemType(decl.getChild(0));
    var.setType(type);
    AST defaultValue = null;
    AST extVarOrDefaultVal = decl.getChild(1);
    if (extVarOrDefaultVal.getType() == XQ.ExternalVariable) {
      if (decl.getChildCount() == 3) {
        defaultValue = decl.getChild(2);
      }
    } else {
      var.setExternal(false);
      defaultValue = extVarOrDefaultVal;
    }
    if (defaultValue != null) {
      if (module.getTargetNS() != null) {
        throw new QueryException(ErrorCode.ERR_CONTEXT_ITEM_VALUE_SPEC_IN_LIBRARY,
                                 "Illegal definition of context item value in library module");
      }
      decls.add(new CtxItemDecl(module, var, decl, defaultValue));
    }
    return true;
  }

  private boolean annotatedDecl(AST decl) throws QueryException {
    return (varDecl(decl) || functionDecl(decl));
  }

  private boolean varDecl(AST decl) throws QueryException {
    if (decl.getType() != XQ.TypedVariableDeclaration) {
      return false;
    }
    boolean declaredPrivateOrPublic = false;
    int pos = 0;
    AST child = decl.getChild(pos++);
    while (child.getType() == XQ.Annotation) {
      String annotation = child.getStringValue();
      if ("%public".equals(annotation) || "%private".equals(annotation)) {
        if (declaredPrivateOrPublic) {
          throw new QueryException(ErrorCode.ERR_VAR_PRIVATE_OR_PUBLIC_ALREADY_DECLARED,
                                   "Variable has already been declared private or public");
        }
        declaredPrivateOrPublic = true;
      }
      // TODO process annotations
      log.warn("Ingoring variable annotation " + annotation);
      child = decl.getChild(pos++);
    }
    QNm name = (QNm) child.getValue();
    // expand and update AST
    name = expand(name, DefaultNS.EMPTY);
    child.setValue(name);
    if (module.getVariables().isDeclared(name)) {
      throw new QueryException(ErrorCode.ERR_DUPLICATE_VARIABLE_DECL, "Variable $%s has already been declared", name);
    }
    String targetNS = module.getTargetNS();
    if ((targetNS != null) && (!targetNS.equals(name.getNamespaceURI()))) {
      throw new QueryException(ErrorCode.ERR_FUN_OR_VAR_NOT_IN_TARGET_NS,
                               "Declared variable $%s is not in library module namespace: %s",
                               name,
                               targetNS);
    }
    child = decl.getChild(pos++);
    SequenceType type;
    boolean external = false;
    if (child.getType() == XQ.SequenceType) {
      type = sequenceType(child);
      child = decl.getChild(pos++);
    } else {
      type = new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany);
    }

    AST defaultValue = null;
    if (child.getType() == XQ.ExternalVariable) {
      external = true;
      if (pos < decl.getChildCount()) {
        defaultValue = decl.getChild(pos);
      }
    } else {
      defaultValue = child;
    }

    DeclVariable var = module.getVariables().declare(name, type, external);

    if (defaultValue != null) {
      // defer default value because it
      // might depend on other variables
      decls.add(new VariableDecl(module, var, decl, defaultValue));
    }
    return true;
  }

  private boolean functionDecl(AST decl) throws QueryException {
    if (decl.getType() != XQ.FunctionDecl) {
      return false;
    }

    boolean declaredPrivateOrPublic = false;
    // Begin XQuery Update 1.0
    boolean updating = false;
    // End XQuery Update 1.0
    int pos = 0;
    AST child = decl.getChild(pos++);
    while (child.getType() == XQ.Annotation) {
      boolean ignored = true;
      String annotation = child.getStringValue();
      if (("public".equals(annotation)) || ("private".equals(annotation))) {
        if (declaredPrivateOrPublic) {
          throw new QueryException(ErrorCode.ERR_FUN_PRIVATE_OR_PUBLIC_ALREADY_DECLARED,
                                   "Function has already been declared private or public");
        }
        declaredPrivateOrPublic = true;
      } else if ("updating".equals(annotation)) {
        updating = true;
        ignored = false;
      }
      // TODO process annotations
      if (ignored) {
        log.warn("Ignoring function annotation " + annotation);
      }
      child = decl.getChild(pos++);
    }

    // function name
    QNm name = (QNm) child.getValue();
    // expand and update AST
    name = expand(name, DefaultNS.FUNCTION);
    child.setValue(name);
    if (name.getNamespaceURI().isEmpty()) {
      throw new QueryException(ErrorCode.ERR_FUNCTION_DECL_NOT_IN_NS, "Function %s is not in a namespace", name);
    }

    String targetNS = module.getTargetNS();
    if ((targetNS != null) && (!targetNS.equals(name.getNamespaceURI()))) {
      throw new QueryException(ErrorCode.ERR_FUN_OR_VAR_NOT_IN_TARGET_NS,
                               "Declared function %s is not in library module namespace: %s",
                               name,
                               targetNS);
    }

    String uri = name.getNamespaceURI();
    if ((uri.equals(Namespaces.XML_NSURI)) || (uri.equals(Namespaces.XS_NSURI)) || (uri.equals(Namespaces.XSI_NSURI))
        || (uri.equals(Namespaces.FN_NSURI)) || (uri.equals(Namespaces.FNMATH_NSURI))) {
      throw new QueryException(ErrorCode.ERR_FUNCTION_DECL_IN_ILLEGAL_NAMESPACE,
                               "Declared function %s is in illegal namespace: %s",
                               name,
                               uri);
    }

    // parameters
    int noOfParameters = (decl.getChildCount() - pos - 2);
    QNm[] pNames = new QNm[noOfParameters];
    SequenceType[] pTypes = new SequenceType[noOfParameters];
    for (int i = 0; i < noOfParameters; i++) {
      child = decl.getChild(pos++);
      pNames[i] = (QNm) child.getChild(0).getValue();
      // expand and update AST
      pNames[i] = expand(pNames[i], DefaultNS.EMPTY);
      child.getChild(0).setValue(pNames[i]);
      for (int j = 0; j < i; j++) {
        if (pNames[i].atomicCmp(pNames[j]) == 0) {
          throw new QueryException(ErrorCode.ERR_DUPLICATE_FUN_PARAMETER,
                                   "Duplicate parameter in declared function %s: %s",
                                   name,
                                   pNames[j]);
        }
      }
      if (child.getChildCount() == 2) {
        pTypes[i] = sequenceType(child.getChild(1));
      } else {
        pTypes[i] = SequenceType.ITEM_SEQUENCE;
      }
    }

    // result type
    child = decl.getChild(pos++);
    SequenceType resultType = sequenceType(child);
    child = decl.getChild(pos);

    // register function beforehand to support recursion
    Signature signature = new Signature(resultType, pTypes);
    UDF udf = new UDF(name, signature, updating);
    sctx.getFunctions().declare(udf);

    // defer function body because functions
    // can depend on declared variables and other
    // declared or imported functions
    final AST body = child;
    decls.add(new FunctionDecl(module, udf, pNames, body));
    return true;
  }

  private boolean optionDecl(AST option) throws QueryException {
    if (option.getType() != XQ.OptionDeclaration) {
      return false;
    }
    QNm name = (QNm) option.getChild(0).getValue();
    // expand and update AST
    name = expand(name, DefaultNS.EMPTY);
    option.getChild(0).setValue(name);
    Str value = (Str) option.getChild(1).getValue();
    module.addOption(name, value);
    return true;
  }

}