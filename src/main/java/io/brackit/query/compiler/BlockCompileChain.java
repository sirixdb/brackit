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

import java.util.Map;

import io.brackit.query.atomic.AnyURI;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.compiler.translator.BlockTranslator;
import io.brackit.query.compiler.translator.Translator;

/**
 * Compile chain that uses block-based (parallel) execution model.
 *
 * @author Sebastian Baechle
 */
public class BlockCompileChain extends CompileChain {

  private final boolean ordered;

  public BlockCompileChain() {
    this(true);
  }

  public BlockCompileChain(boolean ordered) {
    super();
    this.ordered = ordered;
  }

  public BlockCompileChain(AnyURI baseURI, boolean ordered) {
    super(baseURI);
    this.ordered = ordered;
  }

  public BlockCompileChain(ModuleResolver resolver, boolean ordered) {
    super(resolver);
    this.ordered = ordered;
  }

  public BlockCompileChain(ModuleResolver resolver, AnyURI baseURI, boolean ordered) {
    super(resolver, baseURI);
    this.ordered = ordered;
  }

  @Override
  protected Translator getTranslator(Map<QNm, Str> options) {
    BlockTranslator translator = new BlockTranslator(options);
    translator.setOrdered(ordered);
    return translator;
  }
}
