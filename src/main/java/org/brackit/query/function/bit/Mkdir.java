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
package org.brackit.query.function.bit;

import org.brackit.query.QueryContext;
import org.brackit.query.QueryException;
import org.brackit.query.atomic.Atomic;
import org.brackit.query.atomic.Bool;
import org.brackit.query.atomic.QNm;
import org.brackit.query.compiler.Bits;
import org.brackit.query.function.AbstractFunction;
import org.brackit.query.module.StaticContext;
import org.brackit.query.jdm.Sequence;
import org.brackit.query.jdm.Signature;
import org.brackit.query.jdm.type.AtomicType;
import org.brackit.query.jdm.type.Cardinality;
import org.brackit.query.jdm.type.SequenceType;

/**
 * @author Henrique Valer
 */
public class Mkdir extends AbstractFunction {

  public static final QNm DEFAULT_NAME = new QNm(Bits.BIT_NSURI, Bits.BIT_PREFIX, "mkdir");

  public Mkdir() {
    super(Mkdir.DEFAULT_NAME,
          new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                        new SequenceType(AtomicType.STR, Cardinality.One)),
          true);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    String vDirName = ((Atomic) args[0]).stringValue();
    try {
      ctx.getNodeStore().makeDir(vDirName);
      return Bool.TRUE;
    } catch (Exception e) {
      throw new QueryException(e, BitFun.BIT_MAKEDIRECTORY_INT_ERROR, e.getMessage());
    }
  }
}