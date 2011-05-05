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
package org.brackit.xquery.util.path;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.antlr.runtime.RecognitionException;

/**
 * General purpose representation of a path or a path expression.
 * 
 * A typical use of the generic type is either {@see java.lang.Integer}
 * representing vocIDs or {@see java.lang.String}.
 * 
 * @author Sebastian Baechle
 * 
 */
public class Path<E extends Comparable<E>> {
	public enum Axis {
		PARENT(".."), SELF("."), DESC("//"), CHILD("/"), DESC_ATTRIBUTE("//@"), CHILD_ATTRIBUTE(
				"/@");

		private final String text;

		private Axis(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}
	};

	public static class Step<T> {
		private final Axis axis;

		private final T value;

		private Step(Axis axis, T value) {
			if (axis == null) {
				throw new NullPointerException();
			}

			this.axis = axis;
			this.value = value;
		}

		public Axis getAxis() {
			return axis;
		}

		public T getValue() {
			return value;
		}

		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {
			if (!(obj instanceof Step)) {
				return false;
			}

			Step<?> other = (Step<?>) obj;

			return (other.axis.equals(axis))
					&& ((other.value == value) || (other.value.equals(value)));
		}

		@Override
		public int hashCode() {
			return (value != null) ? axis.hashCode() ^ value.hashCode() : axis
					.hashCode();
		}

		public String toString() {
			String axisString = (axis != null) ? axis.getText() : "";
			String valueString = ((value != null) ? value.toString()
					: ((axis == Axis.PARENT) || (axis == Axis.SELF)) ? "" : "*");
			return axisString + valueString;
		}
	}

	private final ArrayList<Step<E>> path;

	public Path() {
		path = new ArrayList<Step<E>>();
	}

	private Path(ArrayList<Step<E>> path) {
		this.path = path;
	}

	public Path<E> append(Path<E> other) {
		path.addAll(other.steps());
		return this;
	}

	public Path<E> child(E value) {
		path.add(new Step<E>(Axis.CHILD, value));
		return this;
	}

	public Path<E> descendant(E value) {
		path.add(new Step<E>(Axis.DESC, value));
		return this;
	}

	public Path<E> attribute(E value) {
		path.add(new Step<E>(Axis.CHILD_ATTRIBUTE, value));
		return this;
	}

	public Path<E> descendantAttribute(E value) {
		path.add(new Step<E>(Axis.DESC_ATTRIBUTE, value));
		return this;
	}

	public Path<E> child() {
		path.add(new Step<E>(Axis.CHILD, null));
		return this;
	}

	public Path<E> descendant() {
		path.add(new Step<E>(Axis.DESC, null));
		return this;
	}

	public Path<E> attribute() {
		path.add(new Step<E>(Axis.CHILD_ATTRIBUTE, null));
		return this;
	}

	public Path<E> descendantAttribute() {
		path.add(new Step<E>(Axis.DESC_ATTRIBUTE, null));
		return this;
	}

	public Path<E> self() {
		path.add(new Step<E>(Axis.SELF, null));
		return this;
	}

	public Path<E> parent() {
		path.add(new Step<E>(Axis.PARENT, null));
		return this;
	}

	@SuppressWarnings("unchecked")
	public boolean matches(Path<E> other) throws PathException {
		if (isEmpty()) {
			throw new PathException("Empty pattern");
		}

		if (path.isEmpty()) {
			throw new PathException("Illegal path: %s", path);
		}

		final Step<E>[] o = other.path.toArray((Step<E>[]) Array.newInstance(
				path.get(0).getClass(), other.path.size()));
		final Step<E>[] p = path.toArray((Step<E>[]) Array.newInstance(path
				.get(0).getClass(), path.size()));
		int oLen = o.length;
		int pLen = p.length;
		int[] matchTable = new int[pLen];

		if (oLen < pLen) {
			return false;
		}

		int oPos = oLen - 1;
		int pPos = pLen - 1;

		while (pPos >= 0) {
			Axis pAxis = p[pPos].axis;
			boolean pIsAttributeStep = (pAxis == Axis.CHILD_ATTRIBUTE)
					|| (pAxis == Axis.DESC_ATTRIBUTE);
			boolean pIsNodeStep = (pAxis == Axis.CHILD) || (pAxis == Axis.DESC);

			Axis oAxis = o[oPos].axis;
			boolean oIsAttributeChildStep = (oAxis == Axis.CHILD_ATTRIBUTE);
			boolean oIsNodeChildStep = (oAxis == Axis.CHILD);

			if (!pIsNodeStep && !pIsAttributeStep) {
				throw new PathException("Illegal pattern path: %s", this);
			}

			if (!oIsAttributeChildStep && !oIsNodeChildStep) {
				throw new PathException("Illegal path: %s", path);
			}

			// System.out.print(String.format("p: %3s  o: %3s  oPos: %3s  pPos: %3s  ",
			// p[pPos], o[oPos], oPos, pPos));

			if (((p[pPos].value == null) || o[oPos].value
					.equals((p[pPos].value)))
					&& ((pIsNodeStep && oIsNodeChildStep) || (pIsAttributeStep && oIsAttributeChildStep))) {
				// System.out.println("match " + p[pPos]);
				matchTable[pPos] = oPos;
				oPos--;
				pPos--;
			} else if (pPos < pLen - 1) {
				while ((p[pPos + 1].axis != Axis.DESC)
						&& (p[pPos + 1].axis != Axis.DESC_ATTRIBUTE)) {
					// backtracking
					// System.out.println("Backtracking to pPos " + (pPos + 1));
					pPos++;

					if (pPos == pLen - 1) {
						return false;
					}

					oPos = matchTable[pPos];
				}

				// // try to match an ancestor
				// System.out.println("Try to match ancestor");
				oPos--;
			} else {
				// leaf does not match
				return false;
			}

			if (oPos < pPos) {
				// a match is impossible
				return false;
			}
		}

		boolean match = ((oPos == -1) || (p[0].axis == Axis.DESC) || (p[0].axis == Axis.DESC_ATTRIBUTE));

		if (match) {
			// System.out.println("MatchTable: " + Arrays.toString(matchTable));
		}

		return match;
	}

	public E head() {
		int size = path.size();
		return (size > 0) ? path.get(0).value : null;
	}

	public E tail() {
		int index = path.size() - 1;
		return (index >= 0) ? path.get(index).value : null;
	}

	public Path<E> leading() {
		if (path.size() == 0) {
			return new Path<E>();
		} else {
			return new Path<E>(new ArrayList<Step<E>>(path.subList(0, path
					.size() - 1)));
		}
	}

	public Path<E> trailing() {
		int index = path.size();
		return (index > 0) ? new Path<E>(new ArrayList<Step<E>>(path.subList(1,
				index))) : new Path<E>();
	}

	public List<Step<E>> steps() {
		return Collections.unmodifiableList(path);
	}

	@SuppressWarnings("unchecked")
	public List<Path<E>> explode() {
		Path<E>[] list = new Path[path.size()];

		Path<E> current = this;

		for (int i = path.size() - 1; i >= 0; i--) {
			list[i] = current;
			current = current.leading();
		}

		return Arrays.asList(list);
	}

	public boolean isEmpty() {
		return path.size() == 0;
	}

	public boolean isAbsolute() {
		for (Step<E> section : path) {
			if ((section.axis != Axis.CHILD) || (section.value == null)) {
				return false;
			}
		}

		return true;
	}

	public boolean isBackward() {
		if (isEmpty()) {
			return true;
		}

		for (Step<E> step : path) {
			Axis a = step.axis;

			if ((a != Axis.PARENT) && (a != Axis.SELF)) {
				return false;
			}
		}

		return true;
	}

	public boolean isForward() {
		if (isEmpty()) {
			return true;
		}

		for (Step<E> step : path) {
			Axis a = step.axis;

			if (((a != Axis.CHILD) && (a != Axis.CHILD_ATTRIBUTE))
					&& (a != Axis.SELF) && (a != Axis.DESC)
					&& (a != Axis.DESC_ATTRIBUTE)) {
				return false;
			}
		}

		return true;
	}

	public boolean isRelative() {
		return (path.size() != 0) && (path.get(0).axis != Axis.CHILD);
	}

	public boolean isAttribute() {
		return (path.size() != 0)
				&& ((path.get(path.size() - 1).axis == Axis.DESC_ATTRIBUTE) || (path
						.get(path.size() - 1).axis == Axis.CHILD_ATTRIBUTE));
	}

	public Path<E> normalize() {
		return null;
	}

	public Path<E> copy() {
		return new Path<E>((path != null) ? new ArrayList<Path.Step<E>>(path)
				: null);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Path)) {
			return false;
		}

		if (obj == this) {
			return true;
		}

		Path<?> other = (Path<?>) obj;

		if (other.path.size() != path.size()) {
			return false;
		}

		for (int i = 0; i < path.size(); i++) {
			if (!path.get(i).equals(other.path.get(i))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		int hashes[] = new int[path.size()];

		int i = 0;
		for (Step<E> section : path) {
			hashes[i++] = section.hashCode();
		}

		return Arrays.hashCode(hashes);
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		Step<E> previous = null;

		for (Step<E> section : path) {

			if (((section.axis == Axis.SELF) || (section.axis == Axis.PARENT))
					&& (previous != null)
					&& ((previous.axis == Axis.SELF) || (previous.axis == Axis.PARENT))) {
				builder.append("/");
			}

			builder.append(section);
			previous = section;
		}

		return builder.toString();
	}

	public int getLength() {
		return path.size();
	}

	public static Path<String> parse(String path) throws PathException {
		try {
			return (new PathParser(path)).parse();
		} catch (RecognitionException e) {
			throw new PathException(String.format(
					"Unexpected token in '%s' at position %s.", path,
					e.charPositionInLine), e.charPositionInLine);
		} catch (Exception e) {
			throw new PathException(String.format("Error parsing path '%s'",
					path), -1);
		}
	}

	public static void main(String[] args) {
		Path<String> path = new Path<String>();
		Path<String> pattern = new Path<String>();

		path.child("bib").descendant().descendant("book").child("title");
		System.out.println("Tail " + path.tail());
		System.out.println("Leading " + path.leading());
		System.out.println("Explode " + path.explode());

		path = new Path<String>();
		path.child("bib");
		System.out.println("Tail " + path.tail());
		System.out.println("Leading " + path.leading());
		System.out.println("trailing " + path.leading());
		System.out.println("Explode " + path.explode());

		path = new Path<String>();
		System.out.println("Tail " + path.tail());
		System.out.println("Leading " + path.leading());
		System.out.println("trailing " + path.leading());
		System.out.println("Explode " + path.explode());

		// pattern.descendant("title");
		//		
		// System.out.println("Path:" + path);
		// System.out.println("Pattern:" + pattern);
		//				
		// System.out.println("Path matches pattern: " + pattern.matches(path));
	}
}
