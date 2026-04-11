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
package io.brackit.query.compiler.translator;

import java.util.Map;

import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;

/**
 * Translator for block-based (parallel) execution model.
 * Delegates PipeExpr compilation to {@link BlockPipelineStrategy}.
 *
 * @author Sebastian Baechle
 */
public class BlockTranslator extends Compiler {

  private final BlockPipelineStrategy blockStrategy;

  public BlockTranslator(Map<QNm, Str> options) {
    this(options, createStrategy(options));
  }

  private BlockTranslator(Map<QNm, Str> options, BlockPipelineStrategy strategy) {
    super(options, strategy);
    this.blockStrategy = strategy;
  }

  private static BlockPipelineStrategy createStrategy(Map<QNm, Str> options) {
    BlockPipelineStrategy strategy = new BlockPipelineStrategy();
    Str parallelOpt = options.get(new QNm("parallel"));
    if (parallelOpt != null && "true".equalsIgnoreCase(parallelOpt.stringValue())) {
      strategy.setOrdered(false);
    }
    Str morselOpt = options.get(new QNm("morsel"));
    if (morselOpt != null && "true".equalsIgnoreCase(morselOpt.stringValue())) {
      strategy.setMorselParallel(true);
    }
    return strategy;
  }

  public void setOrdered(boolean ordered) {
    blockStrategy.setOrdered(ordered);
  }

  public void setMorselParallel(boolean morselParallel) {
    blockStrategy.setMorselParallel(morselParallel);
  }
}
