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
package org.brackit.xquery.function.fn;

import java.util.Arrays;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.function.Signature;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Stream;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class DeepEqual extends AbstractFunction {
	public DeepEqual(QNm name, Signature signature) {
		super(name, signature, true);
	}

	@Override
	public Sequence execute(QueryContext ctx, Sequence[] args)
			throws QueryException {
		if (args.length == 3) {
			Str collation = (Str) args[2];

			if (!collation.str
					.equals("http://www.w3.org/2005/xpath-functions/collation/codepoint")) {
				throw new QueryException(ErrorCode.ERR_UNSUPPORTED_COLLATION,
						"Unsupported collation: %s", collation);
			}
		}

		Sequence a = args[0];
		Sequence b = args[1];

		if (a == null) {
			if (b == null) {
				return Bool.TRUE;
			}
			Iter it = b.iterate();
			try {
				return (it.next() == null) ? Bool.TRUE : Bool.FALSE;
			} finally {
				it.close();
			}
		} else if (b == null) {
			Iter it = a.iterate();
			try {
				return (it.next() == null) ? Bool.TRUE : Bool.FALSE;
			} finally {
				it.close();
			}
		}

		Iter aIt = a.iterate();
		boolean res;
		Iter bIt = null;
		try {
			bIt = b.iterate();
			Item aItem;
			Item bItem;
			while ((aItem = aIt.next()) != null) {
				bItem = bIt.next();
				if (!deepEquals(ctx, aItem, bItem)) {
					return Bool.FALSE;
				}
			}
			return ((bIt.next() == null) ? Bool.TRUE : Bool.FALSE);
		} finally {
			aIt.close();
			if (bIt != null) {
				bIt.close();
			}
		}
	}

	private boolean deepEquals(QueryContext ctx, Item a, Item b)
			throws QueryException {
		if ((a == null)) {
			return (b == null);
		}
		if (b == null) {
			return false;
		}
		if (a instanceof Atomic) {
			if (!(b instanceof Atomic)) {
				return false;
			}
			return atomicDeepEquals((Atomic) a, (Atomic) b);
		} else {
			if (!(b instanceof Node<?>)) {
				return false;
			}
			try {
				return nodeDeepEquals(ctx, (Node<?>) a, (Node<?>) b);
			} catch (DocumentException e) {
				throw new QueryException(e,
						ErrorCode.BIT_DYN_DOCUMENT_ACCESS_ERROR);
			}
		}
	}

	private boolean nodeDeepEquals(QueryContext ctx, Node<?> a, Node<?> b)
			throws DocumentException, QueryException {
		Kind aKind = a.getKind();
		if (aKind != b.getKind()) {
			return false;
		}

		if (aKind == Kind.ELEMENT) {
			if (!a.getName().equals(b.getName())) {
				return false;
			}

			// TODO For schema support add typing details from
			// XQuery 1.0 and XPath 2.0 Functions: 15.3.1 fn:deep-equal
			if (!attributesDeepEqual(ctx, a, b).bool) {
				return false;
			}

			return childrenDeepEqual(ctx, a, b);
		}
		if (aKind == Kind.ATTRIBUTE) {
			// TODO Type support requires comparison of typed value not string
			// value
			return (a.getName().equals(b.getName()))
					&& (a.getValue().equals(b.getValue()));
		}
		if ((aKind == Kind.TEXT) || (aKind == Kind.COMMENT)) {
			return (a.getValue().equals(b.getValue()));
		}
		if (aKind == Kind.DOCUMENT) {
			return childrenDeepEqual(ctx, a, b);
		}
		if ((aKind == Kind.PROCESSING_INSTRUCTION) || (aKind == Kind.NAMESPACE)) {
			return ((a.getName().equals(b.getName())) && (a.getValue().equals(b
					.getValue())));
		}
		throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
				"Unexpected node kind: '%s'", aKind);
	}

	private boolean childrenDeepEqual(QueryContext ctx, Node<?> a, Node<?> b)
			throws DocumentException, QueryException {
		Node<?> aChild = a.getFirstChild();
		Node<?> bChild = b.getFirstChild();

		while ((aChild != null) && (bChild != null)) {
			while ((aChild.getKind() != Kind.ELEMENT)
					&& (aChild.getKind() != Kind.TEXT)) {
				aChild = aChild.getNextSibling();
				if (aChild == null) {
					break;
				}
			}
			while ((bChild.getKind() != Kind.ELEMENT)
					&& (bChild.getKind() != Kind.TEXT)) {
				bChild = bChild.getNextSibling();
				if (bChild == null) {
					break;
				}
			}
			if ((aChild != null) && (bChild != null)) {
				if (!nodeDeepEquals(ctx, aChild, bChild)) {
					return false;
				}
				aChild = aChild.getNextSibling();
				bChild = bChild.getNextSibling();
			}
		}

		return ((aChild == null) && (bChild == null));
	}

	private Bool attributesDeepEqual(QueryContext ctx, Node<?> a, Node<?> b)
			throws DocumentException {
		Stream<? extends Node<?>> aAttributes = a.getAttributes();
		Node<?>[] allAAttributes = new Node<?>[0];
		int aSize = 0;

		try {
			Node<?> att;
			while ((att = aAttributes.next()) != null) {
				if (aSize == allAAttributes.length) {
					allAAttributes = Arrays.copyOf(allAAttributes,
							(allAAttributes.length * 3) / 2 + 1);
				}
				allAAttributes[aSize++] = att;
			}
		} finally {
			aAttributes.close();
		}

		Stream<? extends Node<?>> bAttributes = b.getAttributes();
		int bSize = 0;

		try {
			Node<?> bAttribute;
			boolean match;
			while ((bAttribute = bAttributes.next()) != null) {
				match = false;
				bSize++;

				for (int i = 0; i < aSize; i++) {
					Node<?> aAttribute = allAAttributes[i];

					// TODO Type support requires comparison of typed value not
					// string value
					if ((aAttribute.getName().equals(bAttribute.getName()))
							&& (aAttribute.getValue().equals(bAttribute
									.getValue()))) {
						match = true;
						break;
					}
				}

				if (!match) {
					return Bool.FALSE;
				}
			}
		} finally {
			bAttributes.close();
		}

		return (aSize == bSize) ? Bool.TRUE : Bool.FALSE;
	}

	private boolean atomicDeepEquals(Atomic a, Atomic b) throws QueryException {
		try {
			return (a.eq(b));
		} catch (QueryException e) {
			if (e.getCode().eq(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE)) {
				return false;
			}
			throw e;
		}
	}

	private Bool oneOrBothNull(Sequence a, Sequence b) throws QueryException {

		return null;
	}
}