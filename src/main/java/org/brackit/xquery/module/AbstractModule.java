/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.expr.ExtVariable;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class AbstractModule implements Module {
	protected final List<Module> modules = new ArrayList<Module>();

	protected final List<ExtVariable> variables = new ArrayList<ExtVariable>();

	protected final Map<QNm, Str> options = new HashMap<QNm, Str>();

	protected final Namespaces namespaces = new Namespaces();

	protected final Functions functions = new Functions();

	protected final Types types = new Types();

	protected String defaultElementNamespace = null;

	protected boolean boundarySpaceStrip = true;

	protected String defaultCollation = "http://www.w3.org/2005/xpath-functions/collation/codepoint";

	protected AnyURI baseURI = null;

	protected boolean constructionModeStrip = false;

	protected boolean orderingModeOrdered = true;

	protected boolean emptyOrderGreatest = false;

	protected boolean copyNSPreserve = true;

	protected boolean copyNSInherit = true;

	@Override
	public void addModule(Module module) {
		modules.add(module);
	}

	@Override
	public void addVariable(ExtVariable variable) {
		variables.add(variable);
	}

	@Override
	public List<Module> getImportedModules() {
		return Collections.unmodifiableList(modules);
	}

	@Override
	public List<ExtVariable> getVariables() {
		return Collections.unmodifiableList(variables);
	}

	@Override
	public Namespaces getNamespaces() {
		return namespaces;
	}

	@Override
	public Functions getFunctions() {
		return functions;
	}

	public Types getTypes() {
		return types;
	}

	@Override
	public void addOption(QNm name, Str value) {
		options.put(name, value);
	}

	@Override
	public Map<QNm, Str> getOptions() {
		return Collections.unmodifiableMap(options);
	}

	@Override
	public boolean isBoundarySpaceStrip() {
		return boundarySpaceStrip;
	}

	@Override
	public void setBoundarySpaceStrip(boolean boundaryWhitespaceStrip) {
		this.boundarySpaceStrip = boundaryWhitespaceStrip;
	}

	@Override
	public String getDefaultCollation() {
		return defaultCollation;
	}

	@Override
	public void setDefaultCollation(String defaultCollation) {
		this.defaultCollation = defaultCollation;
	}

	@Override
	public AnyURI getBaseURI() {
		return baseURI;
	}

	@Override
	public void setBaseURI(AnyURI baseURI) {
		this.baseURI = baseURI;
	}

	@Override
	public boolean isConstructionModeStrip() {
		return constructionModeStrip;
	}

	@Override
	public void setConstructionModeStrip(boolean constructionModeStrip) {
		this.constructionModeStrip = constructionModeStrip;
	}

	@Override
	public boolean isOrderingModeOrdered() {
		return orderingModeOrdered;
	}

	@Override
	public void setOrderingModeOrdered(boolean orderingModeOrdered) {
		this.orderingModeOrdered = orderingModeOrdered;
	}

	@Override
	public boolean isEmptyOrderGreatest() {
		return emptyOrderGreatest;
	}

	@Override
	public void setEmptyOrderGreatest(boolean emptyOrderGreatest) {
		this.emptyOrderGreatest = emptyOrderGreatest;
	}

	public boolean isCopyNSPreserve() {
		return copyNSPreserve;
	}

	public void setCopyNSPreserve(boolean copyNSPreserve) {
		this.copyNSPreserve = copyNSPreserve;
	}

	public boolean isCopyNSInherit() {
		return copyNSInherit;
	}

	public void setCopyNSInherit(boolean copyNSInherit) {
		this.copyNSInherit = copyNSInherit;
	}
}
