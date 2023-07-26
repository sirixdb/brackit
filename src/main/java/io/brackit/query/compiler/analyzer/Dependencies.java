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
package io.brackit.query.compiler.analyzer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.brackit.query.atomic.QNm;

/**
 * @author Sebastian Baechle
 */
class Dependencies<E> {

  Map<E, List<E>> map;
  List<List<E>> cycles;

  Dependencies() {
    map = new HashMap<>();
  }

  List<List<E>> findCycles() {
    ArrayDeque<E> path = new ArrayDeque<>();
    for (E u : map.keySet()) {
      chase(path, u);
    }
    return cycles;
  }

  private boolean chase(ArrayDeque<E> path, E u) {
    if (path.contains(u)) {
      extract(path, u);
      return true;
    }
    path.push(u);
    List<E> deps = map.get(u);
    if (deps != null) {
      List<E> copy = new ArrayList<>(deps);
      for (E dep : copy) {
        if (chase(path, dep)) {
          // this dep caused a cycle
          // remove edge
          deps.remove(dep);
        }
      }
    }
    path.pop();
    return false;
  }

  private void extract(ArrayDeque<E> path, E u) {
    List<E> cycle = new ArrayList<>();
    for (E t : path) {
      cycle.add(t);
      if (t.equals(u)) {
        break;
      }
    }
    if (cycles == null) {
      cycles = new ArrayList<>();
    }
    cycles.add(cycle);
  }

  void dependsOn(E unit, E dependency) {
    List<E> deps = map.get(unit);
    if (deps == null) {
      deps = new LinkedList<>();
      deps.add(dependency);
      map.put(unit, deps);
    } else {
      for (E dep : deps) {
        if (dep == dependency) {
          return;
        }
      }
      deps.add(dependency);
    }
  }

  public static void main(String[] args) {
    QNm a = new QNm("a");
    QNm b = new QNm("b");
    QNm c = new QNm("c");
    QNm d = new QNm("d");
    QNm e = new QNm("e");

    Dependencies<QNm> deps = new Dependencies<>();
    deps.dependsOn(a, b);
    deps.dependsOn(a, d);
    deps.dependsOn(b, c);
    deps.dependsOn(c, d);
    deps.dependsOn(d, b);

    deps.dependsOn(d, a);

    deps.dependsOn(e, d);
    deps.dependsOn(d, e);
    System.out.println(deps.findCycles());
  }
}
