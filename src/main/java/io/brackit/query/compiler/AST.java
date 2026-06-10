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
package io.brackit.query.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.brackit.query.module.StaticContext;
import io.brackit.query.util.dot.DotContext;
import io.brackit.query.util.dot.DotNode;

/**
 * @author Sebastian Baechle
 */
public class AST {
  protected AST parent;
  protected int type;
  protected Object value;
  protected Map<String, Object> properties;
  protected StaticContext sctx;
  protected AST[] children;

  public AST(int type) {
    this(type, XQ.NAMES[type]);
  }

  protected AST(int type, Object value, StaticContext sctx, Map<String, Object> properties) {
    this.type = type;
    this.value = value;
    this.sctx = sctx;
    this.properties = properties;
  }

  public AST(int type, Object value) {
    this.type = type;
    this.value = value;
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public Object getValue() {
    return value;
  }

  public String getStringValue() {
    return value != null ? value.toString() : "";
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public void setProperty(String name, Object value) {
    if (properties == null) {
      properties = new HashMap<>();
    }
    properties.put(name, value);
  }

  public Object getProperty(String name) {
    return properties != null ? properties.get(name) : null;
  }

  public boolean checkProperty(String name) {
    Object p = properties != null ? properties.get(name) : null;
    if (p == null) {
      return false;
    }
    return (Boolean) p;
  }

  public void delProperty(String name) {
    if (properties != null) {
      properties.remove(name);
    }
  }

  public AST getParent() {
    return parent;
  }

  public int getChildCount() {
    return children == null ? 0 : children.length;
  }

  public int getChildIndex() {
    if (parent == null) {
      return -1;
    }
    int i = 0;
    for (AST ast : parent.children) {
      if (ast == this) {
        return i;
      }
      i++;
    }
    throw new IllegalStateException();
  }

  public void addChildren(AST[] children) {
    for (AST child : children) {
      addChild(child);
    }
  }

  public void addChild(AST child) {
    if (child == null) {
      throw new NullPointerException();
    }
    if (child == this) {
      throw new IllegalArgumentException();
    }
    if (children == null) {
      children = new AST[] { child };
    } else {
      children = Arrays.copyOf(children, children.length + 1);
      children[children.length - 1] = child;
    }
    child.parent = this;
  }

  public void insertChild(int position, AST child) {
    if (position < 0 || children == null || position > children.length) {
      throw new IllegalArgumentException(String.format("Illegal child position: %s", position));
    }
    if (child == null) {
      throw new NullPointerException();
    }
    if (child == this) {
      throw new IllegalArgumentException();
    }
    if (children == null) {
      children = new AST[] { child };
    } else if (position == children.length) {
      children = Arrays.copyOf(children, children.length + 1);
      children[children.length - 1] = child;
    } else {
      AST[] tmp = new AST[children.length + 1];
      if (position > 0) {
        System.arraycopy(children, 0, tmp, 0, position);
      }
      System.arraycopy(children, position, tmp, position + 1, children.length - position);
      tmp[position] = child;
      children = tmp;
    }
    child.parent = this;
  }

  public Map<String, Object> getProperties() {
    return Collections.unmodifiableMap(properties);
  }

  public AST getChild(int position) {
    if (position < 0 || children == null || position >= children.length) {
      throw new IllegalArgumentException(String.format("Illegal child position: %s", position));
    }
    return children[position];
  }

  public AST getLastChild() {
    return children != null ? children[children.length - 1] : null;
  }

  public void replaceChild(int position, AST child) {
    if (position < 0 || children == null || position >= children.length) {
      throw new IllegalArgumentException(String.format("Illegal child position: %s", position));
    }
    if (child == null) {
      throw new NullPointerException();
    }
    children[position] = child;
    child.parent = this;
  }

  public void deleteChild(int position) {
    if (position < 0 || children == null || position >= children.length) {
      throw new IllegalArgumentException(String.format("Illegal child position: %s", position));
    }
    if (children.length == 1) {
      children = null;
    } else {
      AST[] tmp = new AST[children.length - 1];
      if (position > 0) {
        System.arraycopy(children, 0, tmp, 0, position);
      }
      int length = children.length - (position + 1);
      System.arraycopy(children, position + 1, tmp, position, length);
      children = tmp;
    }
  }

  public AST copy() {
    return new AST(type, value, sctx, properties == null ? null : new HashMap<String, Object>(properties));
  }

  public AST copyTree() {
    AST copy = copy();
    if (children != null) {
      copy.children = new AST[children.length];
      for (int i = 0; i < children.length; i++) {
        copy.children[i] = children[i].copyTree();
        copy.children[i].parent = copy;
      }
    }
    return copy;
  }

  public String dot() {
    DotContext dt = new DotContext();
    toDot(0, dt);
    return dt.toDotString();
  }

  public void dot(File file) {
    DotContext dt = new DotContext();
    toDot(0, dt);
    dt.write(file);
  }

  private int toDot(int no, DotContext dt) {
    final int myNo = no++;
    String label = getLabel(getStringValue());
    DotNode node = dt.addNode(String.valueOf(myNo));
    node.addRow(label, null);
    if (properties != null) {
      for (Entry<String, Object> prop : properties.entrySet()) {
        Object value = prop.getValue();
        node.addRow(prop.getKey(), value != null ? value.toString() : "");
      }
    }
    if (children != null) {
      for (AST child : children) {
        dt.addEdge(String.valueOf(myNo), String.valueOf(no));
        no = child.toDot(no, dt);
      }
    }
    return no;
  }

  protected String getLabel(String value) {
    return type > 0 && type < XQ.NAMES.length
        ? XQ.NAMES[type].equals(value) ? value : XQ.NAMES[type] + "[" + value + "]"
        : value;
  }

  public AST getFirstChildWithType(int type) {
    if (children == null) {
      return null;
    }
    for (AST child : children) {
      if (child.type == type) {
        return child;
      }
    }
    return null;
  }

  public void setStaticContext(StaticContext sctx) {
    this.sctx = sctx;
  }

  public StaticContext getStaticContext() {
    AST n = this;
    while (n != null) {
      if (n.sctx != null) {
        return n.sctx;
      }
      n = n.parent;
    }
    return null;
  }

  public void display() {
    try {
      File file = File.createTempFile("ast", ".dot");
      file.deleteOnExit();
      dot(file);
      Runtime.getRuntime().exec(new String[] { "/usr/bin/dotty", file.getAbsolutePath() }).waitFor();
      if (!file.delete()) {
        // Best-effort temp-file cleanup (deleteOnExit above is the fallback).
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String toString() {
    String value = getStringValue();
    return getLabel(value);
  }

  /**
   * Converts this AST node and its children to a JSON representation.
   * This is useful for query plan visualization and debugging.
   *
   * @return a JSON string representation of this AST
   */
  public String toJSON() {
    StringBuilder sb = new StringBuilder();
    toJSON(sb);
    return sb.toString();
  }

  private void toJSON(StringBuilder sb) {
    sb.append("{");

    // Type name
    String typeName = (type > 0 && type < XQ.NAMES.length) ? XQ.NAMES[type] : "UNKNOWN";
    sb.append("\"type\":\"").append(escapeJson(typeName)).append("\"");

    // Value (if different from type name and not null)
    if (value != null) {
      String valueStr = value.toString();
      if (!valueStr.equals(typeName)) {
        sb.append(",\"value\":\"").append(escapeJson(valueStr)).append("\"");
      }
    }

    // Properties — serialized deterministically so two compilations of the
    // same query produce byte-identical output:
    //   * keys sorted alphabetically (independent of HashMap bucket order),
    //   * arrays and Collections unwrapped to JSON arrays (fallback is
    //     Object.toString(), which emits identity hashes like
    //     [Ljava.lang.String;@45f421c for array-typed values).
    if (properties != null && !properties.isEmpty()) {
      sb.append(",\"properties\":{");
      final List<String> keys = new ArrayList<>(properties.keySet());
      Collections.sort(keys);
      boolean first = true;
      for (String key : keys) {
        if (!first) {
          sb.append(",");
        }
        first = false;
        sb.append("\"").append(escapeJson(key)).append("\":");
        appendJsonValue(sb, properties.get(key));
      }
      sb.append("}");
    }

    // Children
    if (children != null && children.length > 0) {
      sb.append(",\"children\":[");
      for (int i = 0; i < children.length; i++) {
        if (i > 0) {
          sb.append(",");
        }
        children[i].toJSON(sb);
      }
      sb.append("]");
    }

    sb.append("}");
  }

  /**
   * Deterministic JSON serialization of a property value. Handles
   * primitives, strings, arrays (any component type) and collections;
   * anything else falls back to the element's {@code toString()} rendered
   * as a JSON string.
   */
  private static void appendJsonValue(StringBuilder sb, Object value) {
    if (value == null) {
      sb.append("null");
      return;
    }
    if (value instanceof Boolean || value instanceof Number) {
      sb.append(value);
      return;
    }
    if (value instanceof String s) {
      sb.append("\"").append(escapeJson(s)).append("\"");
      return;
    }
    // Arrays: component-type-specific iteration, no identity hash.
    if (value instanceof Object[] a) {
      sb.append("[");
      for (int i = 0; i < a.length; i++) {
        if (i > 0)
          sb.append(",");
        appendJsonValue(sb, a[i]);
      }
      sb.append("]");
      return;
    }
    if (value instanceof int[] a) {
      sb.append("[");
      for (int i = 0; i < a.length; i++) {
        if (i > 0)
          sb.append(",");
        sb.append(a[i]);
      }
      sb.append("]");
      return;
    }
    if (value instanceof long[] a) {
      sb.append("[");
      for (int i = 0; i < a.length; i++) {
        if (i > 0)
          sb.append(",");
        sb.append(a[i]);
      }
      sb.append("]");
      return;
    }
    if (value instanceof double[] a) {
      sb.append("[");
      for (int i = 0; i < a.length; i++) {
        if (i > 0)
          sb.append(",");
        sb.append(a[i]);
      }
      sb.append("]");
      return;
    }
    if (value instanceof Collection<?> c) {
      sb.append("[");
      boolean first = true;
      for (Object el : c) {
        if (!first)
          sb.append(",");
        first = false;
        appendJsonValue(sb, el);
      }
      sb.append("]");
      return;
    }
    sb.append("\"").append(escapeJson(value.toString())).append("\"");
  }

  private static String escapeJson(String str) {
    if (str == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(str.length());
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      switch (c) {
        case '"':
          sb.append("\\\"");
          break;
        case '\\':
          sb.append("\\\\");
          break;
        case '\b':
          sb.append("\\b");
          break;
        case '\f':
          sb.append("\\f");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        default:
          if (c < ' ') {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
      }
    }
    return sb.toString();
  }
}