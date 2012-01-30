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
package org.brackit.xquery.module;

import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.QNm;

/**
 * <p>
 * The <b>static context</b> as defined in XQuery 3.0 except the in-scope
 * variables.
 * </p>
 * <p>
 * Variables are not included because they are treated differently in several
 * parts of the compilation chain and it would be cumbersome to maintain and
 * synchronize the different representations during query rewriting.
 * </p>
 * <p>
 * Solely the 
 * </p>
 * 
 * @author Sebastian Baechle
 * 
 */
public interface StaticContext {

	public static final String UNICODE_COLLATION = "http://www.w3.org/2005/xpath-functions/collation/codepoint";

	/**
	 * Returns the mapping of statically known namespaces (mapping of prefixes
	 * to namespace URIs), the default element namespace and the default
	 * function namespace.
	 */
	public Namespaces getNamespaces();

	public Functions getFunctions();

	public Types getTypes();

	public void setBoundarySpaceStrip(boolean strip);

	public boolean isBoundarySpaceStrip();

	public String getDefaultCollation();

	public void setDefaultCollation(String collation);

	public AnyURI getBaseURI();

	public void setBaseURI(AnyURI uri);

	public void setConstructionModeStrip(boolean strip);

	public boolean isConstructionModeStrip();

	public void setOrderingModeOrdered(boolean ordered);

	public boolean isOrderingModeOrdered();

	public void setEmptyOrderGreatest(boolean greatest);

	public boolean isEmptyOrderGreatest();

	public boolean isCopyNSPreserve();

	public void setCopyNSPreserve(boolean copyNSPreserve);

	public boolean isCopyNSInherit();

	public void setCopyNSInherit(boolean copyNSInherit);

	public void setDefaultDecimalFormat(DecimalFormat df);

	public void setDecimalFormat(QNm name, DecimalFormat df);

	public DecimalFormat getDecimalFormat(QNm name);

}
