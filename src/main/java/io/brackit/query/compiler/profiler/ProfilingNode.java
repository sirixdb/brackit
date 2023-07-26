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
package io.brackit.query.compiler.profiler;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import io.brackit.query.util.dot.DotContext;
import io.brackit.query.util.dot.DotNode;

/**
 * @author Sebastian Baechle
 */
public abstract class ProfilingNode {
  private static final AtomicInteger idSource = new AtomicInteger(1);

  protected final int id = idSource.getAndIncrement();

  protected ProfilingNode[] ce;

  protected int cel;

  void prependChild(ProfilingNode c) {
    if (c == this) {
      throw new RuntimeException();
    }
    if (ce == null) {
      ce = new ProfilingNode[] { c };
      cel++;
    } else {
      if (cel == ce.length) {
        ce = Arrays.copyOf(ce, (cel * 3) / 2 + 1);
      }
      System.arraycopy(ce, 0, ce, 1, cel);
      ce[0] = c;
      cel++;
    }
  }

  void addChild(ProfilingNode c) {
    if (c == this) {
      throw new RuntimeException();
    }
    if (ce == null) {
      ce = new ProfilingNode[] { c };
      cel++;
    } else {
      if (cel == ce.length) {
        ce = Arrays.copyOf(ce, (cel * 3) / 2 + 1);
      }
      ce[cel++] = c;
    }
  }

  protected abstract String getName();

  protected abstract void addFields(DotNode node);

  public void toDot(DotContext dotCtx) {
    DotNode node = dotCtx.pushChildNode(getName());
    addFields(node);
    for (int i = 0; i < cel; i++) {
      ce[i].toDot(dotCtx);
    }
    dotCtx.popChildNode();
  }
}
