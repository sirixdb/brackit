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

import org.brackit.xquery.QueryException;
import org.brackit.xquery.node.stream.AtomStream;
import org.brackit.xquery.node.stream.EmptyStream;
import org.brackit.xquery.sequence.type.KindTest;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Stream;

/**
 * Covers the document axes as defined in XQuery 1.0: 3.2.1.1 Axes
 * 
 * <p>
 * Note that we sometimes have to check a node's kind because attributes are
 * often not in in the defined path axes.
 * </p>
 * 
 * @author Sebastian Baechle
 * 
 */
public abstract class Axis {
	public static final Axis PARENT = new Axis() {
		@Override
		public boolean check(Node<?> node, Node<?> other) throws QueryException {
			return node.isParentOf(other);
		}

		@Override
		public Stream<Node<?>> performStep(Node<?> node) throws QueryException {
			Node<?> parent = node.getParent();
			return (parent != null) ? new AtomStream<Node<?>>(parent)
					: new EmptyStream<Node<?>>();
		}
	};
	public static final Axis CHILD = new Axis() {
		@Override
		public boolean check(Node<?> node, Node<?> other) throws QueryException {
			return node.isChildOf(other);
		}

		@Override
		public Stream<? extends Node<?>> performStep(Node<?> node)
				throws QueryException {
			return node.getChildren();
		}
	};
	public static final Axis ANCESTOR = new Axis() {
		@Override
		public boolean check(Node<?> node, Node<?> other) throws QueryException {
			return node.isAncestorOf(other);
		}

		@Override
		public Stream<? extends Node<?>> performStep(Node<?> node)
				throws QueryException {
			Node<?> parent = node.getParent();
			return (parent != null) ? parent.getPath()
					: new EmptyStream<Node<?>>();
		}
	};
	public static final Axis DESCENDANT = new Axis() {
		@Override
		public boolean check(Node<?> node, Node<?> other) throws QueryException {
			return node.isDescendantOf(other);
		}

		@Override
		public Stream<? extends Node<?>> performStep(Node<?> node)
				throws QueryException {
			final Stream<? extends Node<?>> subtree = node
					.getDescendantOrSelf();
			subtree.next(); // consume self
			return subtree;
		}
	};
	public static final Axis ANCESTOR_OR_SELF = new Axis() {
		@Override
		public boolean check(Node<?> node, Node<?> other) throws QueryException {
			return node.isAncestorOrSelfOf(other);
		}

		@Override
		public Stream<? extends Node<?>> performStep(Node<?> node)
				throws QueryException {
			Stream<? extends Node<?>> subtree = node.getPath();
			return subtree;
		}
	};
	public static final Axis DESCENDANT_OR_SELF = new Axis() {
		@Override
		public boolean check(Node<?> node, Node<?> other) throws QueryException {
			return node.isDescendantOrSelfOf(other);
		}

		@Override
		public Stream<? extends Node<?>> performStep(Node<?> node)
				throws QueryException {
			final Stream<? extends Node<?>> subtree = node
					.getDescendantOrSelf();
			return subtree;
		}
	};
	public static final Axis ATTRIBUTE = new Axis() {
		@Override
		public boolean check(Node<?> node, Node<?> other) throws QueryException {
			return node.isAttributeOf(other);
		}

		@Override
		public Stream<? extends Node<?>> performStep(Node<?> node)
				throws QueryException {
			Stream<? extends Node<?>> subtree = node.getAttributes();
			return subtree;
		}

		@Override
		public Stream<? extends Node<?>> performStep(Node<?> node, KindTest test)
				throws QueryException {
			if ((test.getNodeKind() == Kind.ATTRIBUTE)
					&& (test.getQName() != null)) {
				Node<?> att = node.getAttribute(test.getQName().getLocalName());
				return (att != null) ? new AtomStream<Node<?>>(att)
						: new EmptyStream<Node<?>>();
			}
			return super.performStep(node, test);
		}
	};
	public static final Axis SELF = new Axis() {
		@Override
		public boolean check(Node<?> node, Node<?> other) throws QueryException {
			return node.isSelfOf(other);
		}

		@Override
		public Stream<? extends Node<?>> performStep(Node<?> node)
				throws QueryException {
			return new AtomStream<Node<?>>(node);
		}
	};
	public static final Axis FOLLOWING = new Axis() {
		@Override
		public boolean check(Node<?> node, Node<?> other) throws QueryException {
			return node.isFollowingOf(other);
		}

		@Override
		public Stream<? extends Node<?>> performStep(Node<?> node)
				throws QueryException {
			final Node<?> n = node;

			return new Stream<Node<?>>() {
				Stream<? extends Node<?>> s;
				Node<?> anchor = n;
				Node<?> current = n;

				@Override
				public void close() {
					if (s != null) {
						s.close();
						s = null;
					}
					current = null;
					anchor = null;
				}

				@Override
				public Node<?> next() throws DocumentException {
					Node<?> n;
					while (true) {
						if (s != null) {
							if ((n = s.next()) != null) {
								return n;
							}
							s.close();
							s = null;
						}

						current = current.getNextSibling();

						while (current == null) {
							anchor = anchor.getParent();

							if (anchor == null) {
								return null;
							}
							current = anchor.getNextSibling();
						}
						s = current.getDescendantOrSelf();
					}
				}
			};
		}
	};
	public static final Axis FOLLOWING_SIBLING = new Axis() {
		@Override
		public boolean check(Node<?> node, Node<?> other) throws QueryException {
			return node.isFollowingSiblingOf(other);
		}

		@Override
		public Stream<? extends Node<?>> performStep(Node<?> node)
				throws QueryException {
			final Node<?> nextSibling = node.getNextSibling();

			return new Stream<Node<?>>() {
				Node<?> next = nextSibling;

				@Override
				public void close() {
					next = null;
				}

				@Override
				public Node<?> next() throws DocumentException {
					if (next == null) {
						return null;
					}
					Node<?> deliver = next;
					next = next.getNextSibling();
					return deliver;
				}
			};
		}
	};
	public static final Axis PRECEDING = new Axis() {
		@Override
		public boolean check(Node<?> node, Node<?> other) throws QueryException {
			return node.isPrecedingOf(other);
		}

		@Override
		public Stream<? extends Node<?>> performStep(Node<?> node)
				throws QueryException {
			final Node<?> n = node;
			Node<?> p = node.getParent();

			if (p == null) {
				return new EmptyStream<Node<?>>();
			}

			while (true) {
				Node<?> a = p.getParent();
				if (a == null) {
					break;
				}
				p = a;
			}

			final Stream<? extends Node<?>> fragment = p.getDescendantOrSelf();

			return new Stream<Node<?>>() {
				final Node<?> stopAt = n;
				Stream<? extends Node<?>> s = fragment;

				@Override
				public void close() {
					if (s != null) {
						s.close();
						s = null;
					}
				}

				@Override
				public Node<?> next() throws DocumentException {
					Node<?> n;
					while ((s != null) && ((n = s.next()) != null)) {
						if (n.isSelfOf(stopAt)) {
							s.close();
							s = null;
							return null;
						}
						if (n.isAncestorOf(stopAt)) {
							continue;
						}
						return n;
					}
					return null;
				}
			};
		}
	};
	public static final Axis PRECEDING_SIBLING = new Axis() {
		@Override
		public boolean check(Node<?> node, Node<?> other) throws QueryException {
			return node.isPrecedingSiblingOf(other);
		}

		@Override
		public Stream<? extends Node<?>> performStep(Node<?> node)
				throws QueryException {
			final Node<?> n = node;
			final Node<?> p = node.getParent();

			if (p == null) {
				return new EmptyStream<Node<?>>();
			}

			final Node<?> sibling = p.getFirstChild();

			return new Stream<Node<?>>() {
				final Node<?> stopAt = n;
				Node<?> next = sibling;

				@Override
				public void close() {
					next = null;
				}

				@Override
				public Node<?> next() throws DocumentException {
					if ((next == null) || (next.isSelfOf(stopAt))) {
						return null;
					}
					Node<?> deliver = next;
					next = next.getNextSibling();
					return deliver;
				}
			};
		}
	};
	
	protected static class KindFilter implements Stream<Node<?>> {
		private final KindTest test;
		private final Stream<? extends Node<?>> stream;
		
		public KindFilter(KindTest test, Stream<? extends Node<?>> stream) {
			this.test = test;
			this.stream = stream;
		}

		@Override
		public Node<?> next() throws DocumentException {			
			try {
				Node<?> next;
				while (((next = stream.next()) != null) && (!test.matches(next)));
				return next;
			} catch (DocumentException e) {
				throw e;
			} catch (QueryException e) {
				throw new DocumentException(e);
			}
		}

		@Override
		public void close() {
			stream.close();
		}	
	}

	public abstract boolean check(Node<?> node, Node<?> other)
			throws QueryException;

	public abstract Stream<? extends Node<?>> performStep(Node<?> node)
			throws QueryException;
	
	public Stream<? extends Node<?>> performStep(Node<?> node, KindTest test) throws QueryException {
		return new KindFilter(test, performStep(node));
	}
}
