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
package org.brackit.xquery.util.path;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.brackit.xquery.atomic.QNm;

/**
 * General purpose representation of a path or a path expression.
 * <p>
 * A typical use of the generic type is either {@see java.lang.Integer}
 * representing vocIDs or {@see java.lang.String}.
 *
 * @author Sebastian Baechle
 */
public final class Path<E> {
  public enum Axis {
    PARENT(".."), SELF("."), DESC("//"), CHILD("/"), DESC_ATTRIBUTE("//@"), CHILD_ATTRIBUTE("/@"), CHILD_ARRAY("/[]"), DESC_ARRAY(
        "//[]");

    private final String text;

    Axis(String text) {
      this.text = text;
    }

    public String getText() {
      return text;
    }
  }

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

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Step)) {
        return false;
      }

      final Step<?> other = (Step<?>) obj;

      return other.axis.equals(axis) && (other.value == value || other.value.equals(value));
    }

    @Override
    public int hashCode() {
      return (value != null) ? axis.hashCode() ^ value.hashCode() : axis.hashCode();
    }

    @Override
    public String toString() {
      final String axisString = axis.getText();
      String valueString = value != null
          ? value.toString()
          : (axis == Axis.PARENT || axis == Axis.SELF || axis == Axis.CHILD_ARRAY || axis == Axis.DESC_ARRAY)
              ? ""
              : "*";
      return axisString + valueString;
    }
  }

  private final ArrayList<Step<E>> path;

  public Path() {
    path = new ArrayList<>();
  }

  private Path(ArrayList<Step<E>> path) {
    this.path = path;
  }

  public Path<E> append(Path<E> other) {
    path.addAll(other.steps());
    return this;
  }

  public Path<E> child(E value) {
    path.add(new Step<>(Axis.CHILD, value));
    return this;
  }

  public Path<E> descendant(E value) {
    path.add(new Step<>(Axis.DESC, value));
    return this;
  }

  public Path<E> attribute(E value) {
    path.add(new Step<>(Axis.CHILD_ATTRIBUTE, value));
    return this;
  }

  public Path<E> descendantAttribute(E value) {
    path.add(new Step<>(Axis.DESC_ATTRIBUTE, value));
    return this;
  }

  public Path<E> child() {
    path.add(new Step<>(Axis.CHILD, null));
    return this;
  }

  public Path<E> descendant() {
    path.add(new Step<>(Axis.DESC, null));
    return this;
  }

  public Path<E> attribute() {
    path.add(new Step<>(Axis.CHILD_ATTRIBUTE, null));
    return this;
  }

  public Path<E> descendantAttribute() {
    path.add(new Step<>(Axis.DESC_ATTRIBUTE, null));
    return this;
  }

  public Path<E> self() {
    path.add(new Step<>(Axis.SELF, null));
    return this;
  }

  public Path<E> parent() {
    path.add(new Step<>(Axis.PARENT, null));
    return this;
  }

  public Path<E> childArray() {
    path.add(new Step<>(Axis.CHILD_ARRAY, null));
    return this;
  }

  public Path<E> descendantArray() {
    path.add(new Step<>(Axis.DESC_ARRAY, null));
    return this;
  }

  /**
   * The semantics of this method are twofold, as one has to distinguish
   * abstract <i>path patterns</i> from absolute <i>path instances</i>: It
   * returns true if
   * <ol>
   * <li>a path instance matches a path pattern, i.e. path pattern X matches Y
   * if Y is absolute and X matches Y</li>
   * <li>a path pattern is subsumed by another pattern, i.e. X and Y are path
   * patterns and every absolute path matching Y also matches X.</li>
   * </ol>
   * <br/>
   * Examples:
   * <ol>
   * <li>/a/b//c matches /a/b/c<br/>
   * /a/b//d matches /a/b/c/d<br/>
   * /a/b/d does not match /a/b/c/d</li>
   * <li>/a/b/c matches /a/b//c<br/>
   * /a/b//c matches /a/b//c<br/>
   * /a//b/c matches a/b//b/c<br/>
   * /a//b/c does not match /a/b//c (cf. /a/b/d/c)</li>
   * </ol>
   */
  @SuppressWarnings("unchecked")
  public boolean matches(Path<E> other) throws PathException {
    if (isEmpty()) {
      throw new PathException("Empty pattern");
    }

    if (path.isEmpty()) {
      throw new PathException("Illegal path: %s", path);
    }

    final Step<E>[] o = other.path.toArray((Step<E>[]) Array.newInstance(path.get(0).getClass(), other.path.size()));
    final Step<E>[] p = path.toArray((Step<E>[]) Array.newInstance(path.get(0).getClass(), path.size()));
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
      boolean pIsAttributeStep = (pAxis == Axis.CHILD_ATTRIBUTE) || (pAxis == Axis.DESC_ATTRIBUTE);
      boolean pIsArrayStep = (pAxis == Axis.CHILD_ARRAY) || (pAxis == Axis.DESC_ARRAY);
      boolean pIsNodeStep = (pAxis == Axis.CHILD) || (pAxis == Axis.DESC);

      Axis oAxis = o[oPos].axis;
      boolean oIsAttributeStep = (oAxis == Axis.CHILD_ATTRIBUTE) || (oAxis == Axis.DESC_ATTRIBUTE);
      boolean oIsArrayStep = (oAxis == Axis.CHILD_ARRAY) || (oAxis == Axis.DESC_ARRAY);
      boolean oIsNodeStep = (oAxis == Axis.CHILD) || (oAxis == Axis.DESC);

      if (!pIsNodeStep && !pIsAttributeStep && !pIsArrayStep) {
        throw new PathException("Illegal pattern path: %s", this);
      }

      if (!oIsAttributeStep && !oIsArrayStep && !oIsNodeStep) {
        throw new PathException("Illegal path: %s", path);
      }

      // System.out.print(String.format("p: %3s  o: %3s  oPos: %3s  pPos: %3s  ",
      // p[pPos], o[oPos], oPos, pPos));

      if ((p[pPos].value == null || p[pPos].value.equals(o[oPos].value)) && (pAxis == oAxis || (
          pAxis == Axis.DESC && oAxis == Axis.CHILD) || (pAxis == Axis.DESC_ATTRIBUTE && oAxis
          == Axis.CHILD_ATTRIBUTE) || (pAxis == Axis.DESC_ARRAY && oAxis == Axis.CHILD_ARRAY))) {
        // System.out.println("match " + p[pPos]);
        matchTable[pPos] = oPos;
        oPos--;
        pPos--;
      } else if (pPos < pLen - 1) {
        while (p[pPos + 1].axis != Axis.DESC && p[pPos + 1].axis != Axis.DESC_ATTRIBUTE
            && p[pPos + 1].axis != Axis.DESC_ARRAY) {
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

    boolean match =
        oPos == -1 || p[0].axis == Axis.DESC || p[0].axis == Axis.DESC_ATTRIBUTE || p[0].axis == Axis.DESC_ARRAY;

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
      return new Path<>();
    } else {
      return new Path<>(new ArrayList<>(path.subList(0, path.size() - 1)));
    }
  }

  public Path<E> trailing() {
    int index = path.size();
    return (index > 0) ? new Path<>(new ArrayList<>(path.subList(1, index))) : new Path<>();
  }

  public List<Step<E>> steps() {
    return Collections.unmodifiableList(path);
  }

  public List<Path<?>> explode() {
    final Path<?>[] list = new Path[path.size()];

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
    for (final Step<E> section : path) {
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

    for (final Step<E> step : path) {
      Axis a = step.axis;

      if (((a != Axis.CHILD) && (a != Axis.CHILD_ATTRIBUTE)) && (a != Axis.SELF) && (a != Axis.DESC) && (a
          != Axis.DESC_ATTRIBUTE)) {
        return false;
      }
    }

    return true;
  }

  public boolean isRelative() {
    return (path.size() != 0) && (path.get(0).axis != Axis.CHILD);
  }

  public boolean isAttribute() {
    return (path.size() != 0) && ((path.get(path.size() - 1).axis == Axis.DESC_ATTRIBUTE) || (
        path.get(path.size() - 1).axis == Axis.CHILD_ATTRIBUTE));
  }

  public Path<E> normalize() {
    return null;
  }

  public Path<E> copy() {
    return new Path<>((path != null) ? new ArrayList<>(path) : null);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Path<?>)) {
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
    int[] hashes = new int[path.size()];

    int i = 0;
    for (Step<E> section : path) {
      hashes[i++] = section.hashCode();
    }

    return Arrays.hashCode(hashes);
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    Step<E> previous = null;

    for (final Step<E> section : path) {
      if (((section.axis == Axis.SELF) || (section.axis == Axis.PARENT)) && (previous != null) && (
          (previous.axis == Axis.SELF) || (previous.axis == Axis.PARENT))) {
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

  public static Path<QNm> parse(String path) throws PathException {
    return (new PathParser(path)).parse();
  }

  public static void main(String[] args) {
    var path = new Path<String>();

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