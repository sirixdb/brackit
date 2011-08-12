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
package org.brackit.xquery.node;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.node.stream.filter.Filter;
import org.brackit.xquery.node.stream.filter.FilteredStream;
import org.brackit.xquery.xdm.AbstractItem;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public abstract class AbstractNode<E extends Node<E>> extends AbstractItem implements Node<E> {
	protected static final Kind[] mapping = new Kind[] { Kind.ELEMENT,
			Kind.ATTRIBUTE, Kind.TEXT, Kind.DOCUMENT, Kind.COMMENT,
			Kind.PROCESSING_INSTRUCTION };

	@Override
	public Stream<? extends E> getPath() throws DocumentException {
		final Node<? extends E> node = this;
		return new Stream() {
			Node<? extends E> next = node;

			@Override
			public void close() {
			}

			@Override
			public Object next() throws DocumentException {
				if (next == null) {
					return null;
				}
				Node<? extends E> deliver = next;
				next = deliver.getParent();
				return deliver;
			}
		};
	}

	@Override
	public final int cmp(Node<?> other) {
		if (((Object) other) == this) {
			return 0;
		}
		long fragmentIDA = getFragmentID();
		long fragmentIDB = other.getFragmentID();
		if (fragmentIDA == fragmentIDB) {
			return cmpInternal((E) other);
		} else {
			return fragmentIDA < fragmentIDB ? -1 : 1;
		}
	}

	protected abstract int cmpInternal(E other);

	private static class AttributeFilter implements Filter<Node<?>> {
		@Override
		public boolean filter(Node<?> node) throws DocumentException {
			boolean check = node.getKind() == Kind.ATTRIBUTE;
			return check;
		}
	}

	@Override
	public Stream<? extends E> getDescendantOrSelf() throws DocumentException {
		return new FilteredStream<E>(getSubtree(), new AttributeFilter());
	}

	@Override
	public boolean booleanValue() throws QueryException {
		return true;
	}

	@Override
	public Type type() {
		return Type.UNA;
	}

	@Override
	public Atomic atomize() throws QueryException {
		return new Una(getValue());
	}

	@Override
	public final boolean equals(Object obj) {
		return ((obj instanceof Node<?>) && (isSelfOf(((Node<?>) obj))));
	}
}
