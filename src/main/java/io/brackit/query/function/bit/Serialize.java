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
package io.brackit.query.function.bit;

import java.io.PrintStream;

import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jdm.type.AtomicType;
import io.brackit.query.jdm.type.Cardinality;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.module.StaticContext;
import io.brackit.query.util.io.IOUtils;
import io.brackit.query.util.serialize.StringSerializer;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.compiler.Bits;
import io.brackit.query.function.AbstractFunction;

/**
 * @author Sebastian Baechle
 */
public class Serialize extends AbstractFunction {

  public static final QNm DEFAULT_NAME = new QNm(Bits.BIT_NSURI, Bits.BIT_PREFIX, "serialize");

  public Serialize() {
    this(DEFAULT_NAME);
  }

  /**
   * Constructor.
   *
   * @param name the qname
   */
  public Serialize(QNm name) {
    super(name, new Signature(new SequenceType(AtomicType.STR, Cardinality.One), SequenceType.ITEM_SEQUENCE), true);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    final Sequence sequence = args[0];
    if (sequence == null) {
      return Int32.ZERO;
    }
    final PrintStream buf = IOUtils.createBuffer();
    try (StringSerializer serializer = new StringSerializer(buf)) {
      serializer.serialize(sequence);
    }
    return new Str(buf.toString());
  }
}