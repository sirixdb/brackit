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
package org.brackit.xquery.expr;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class ElementExpr extends ConstructedNodeBuilder implements Expr {

	public static class NS {
		private final Str prefix;
		private final AnyURI uri;

		public NS(Str prefix, AnyURI uri) {
			this.prefix = prefix;
			this.uri = uri;
		}

		public Str getPrefix() {
			return prefix;
		}

		public AnyURI getURI() {
			return uri;
		}
	}

	protected final Expr nameExpr;

	protected final NS[] namespaces;
	
	protected final Expr[] contentExprs;

	protected final boolean bind;

	protected final boolean appendOnly;

	protected final QNm name;

	public ElementExpr(Expr nameExpr, NS[] namespaces, Expr[] contentExpr, boolean bind,
			boolean appendOnly) {
		this.nameExpr = nameExpr;
		this.namespaces = namespaces;
		this.contentExprs = contentExpr;
		this.bind = bind;
		this.appendOnly = appendOnly;
		this.name = (QNm) ((nameExpr instanceof QNm) ? nameExpr : null);
	}

	@Override
	public final Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return evaluateToItem(ctx, tuple);
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		// See XQuery 3.7.3.1 Computed Element Constructors
		QNm name = (this.name != null) ? this.name : buildElementName(ctx,
				nameExpr.evaluateToItem(ctx, tuple));

		final Node<?> element;

		if (appendOnly) {
			element = ((Node<?>) tuple.get(tuple.getSize() - 1)).append(
					Kind.ELEMENT, name, null);
		} else {
			element = ctx.getNodeFactory().element(name);
		}
		
		for (NS ns : namespaces) {
			element.getScope().addPrefix(ns.getPrefix(), ns.getURI());
		}

		ContentSink sink = new ContentSink() {
			@Override
			public Node<?> addNode(QueryContext ctx, Node<?> node)
					throws QueryException {
				return element.append(node);
			}

			@Override
			public Node<?> addAttribute(QueryContext ctx, Node<?> attribute)
					throws QueryException {
				return element.setAttribute(attribute);
			}
		};

		final Tuple t = bind ? tuple.concat(element) : tuple;

		for (int i = 0; i < contentExprs.length; i++) {
			Sequence content = contentExprs[i].evaluate(ctx, t);
			buildContentSequence(ctx, sink, content);
		}
		return (appendOnly) ? null : element;
	}

	@Override
	public boolean isUpdating() {
		boolean updating = nameExpr.isUpdating();
		int i = 0;
		while ((!updating) && (i < contentExprs.length)) {
			updating = contentExprs[i++].isUpdating();
		}
		return updating;
	}

	@Override
	public boolean isVacuous() {
		return false;
	}

	public String toString() {
		return "element";
	}
}