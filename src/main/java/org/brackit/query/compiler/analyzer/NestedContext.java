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
package org.brackit.query.compiler.analyzer;

import org.brackit.query.atomic.AnyURI;
import org.brackit.query.atomic.QNm;
import org.brackit.query.module.DecimalFormat;
import org.brackit.query.module.Functions;
import org.brackit.query.module.Namespaces;
import org.brackit.query.module.StaticContext;
import org.brackit.query.module.Types;

/**
 * @author Sebastian Baechle
 */
public class NestedContext implements StaticContext {
  private final StaticContext parent;
  private final Namespaces ns;
  private Boolean orderingMode;

  public NestedContext(StaticContext parent) {
    this.parent = parent;
    this.ns = new Namespaces(parent.getNamespaces());
  }

  @Override
  public Namespaces getNamespaces() {
    return ns;
  }

  @Override
  public boolean isOrderingModeOrdered() {
    return (orderingMode != null) ? orderingMode : parent.isOrderingModeOrdered();
  }

  @Override
  public void setOrderingModeOrdered(boolean ordered) {
    this.orderingMode = ordered;
  }

  public AnyURI getBaseURI() {
    return parent.getBaseURI();
  }

  public DecimalFormat getDecimalFormat(QNm name) {
    return parent.getDecimalFormat(name);
  }

  public String getDefaultCollation() {
    return parent.getDefaultCollation();
  }

  public Functions getFunctions() {
    return parent.getFunctions();
  }

  public Types getTypes() {
    return parent.getTypes();
  }

  public boolean isBoundarySpaceStrip() {
    return parent.isBoundarySpaceStrip();
  }

  public boolean isConstructionModeStrip() {
    return parent.isConstructionModeStrip();
  }

  public boolean isCopyNSInherit() {
    return parent.isCopyNSInherit();
  }

  public boolean isCopyNSPreserve() {
    return parent.isCopyNSPreserve();
  }

  public boolean isEmptyOrderGreatest() {
    return parent.isEmptyOrderGreatest();
  }

  public void setBaseURI(AnyURI uri) {
    parent.setBaseURI(uri);
  }

  public void setBoundarySpaceStrip(boolean strip) {
    parent.setBoundarySpaceStrip(strip);
  }

  public void setConstructionModeStrip(boolean strip) {
    parent.setConstructionModeStrip(strip);
  }

  public void setCopyNSInherit(boolean copyNSInherit) {
    parent.setCopyNSInherit(copyNSInherit);
  }

  public void setCopyNSPreserve(boolean copyNSPreserve) {
    parent.setCopyNSPreserve(copyNSPreserve);
  }

  public void setDecimalFormat(QNm name, DecimalFormat df) {
    parent.setDecimalFormat(name, df);
  }

  public void setDefaultCollation(String collation) {
    parent.setDefaultCollation(collation);
  }

  public void setDefaultDecimalFormat(DecimalFormat df) {
    parent.setDefaultDecimalFormat(df);
  }

  public void setEmptyOrderGreatest(boolean greatest) {
    parent.setEmptyOrderGreatest(greatest);
  }
}
