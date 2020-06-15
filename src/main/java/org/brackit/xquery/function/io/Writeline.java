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
package org.brackit.xquery.function.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.util.io.URIHandler;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.type.AnyItemType;
import org.brackit.xquery.xdm.type.AtomicType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * @author Sebastian Baechle
 */
public class Writeline extends AbstractFunction {
  public static final QNm DEFAULT_NAME = new QNm(IOFun.IO_NSURI, IOFun.IO_PREFIX, "writeline");

  public Writeline() {
    this(DEFAULT_NAME);
  }

  public Writeline(QNm name) {
    super(name,
          new Signature(new SequenceType(AtomicType.INR, Cardinality.One),
                        new SequenceType(AtomicType.STR, Cardinality.One),
                        new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany)),
          true);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, final Sequence[] args) throws QueryException {
    if (args[1] == null) {
      return Int32.ZERO;
    }
    try {
      IntNumeric count = Int32.ZERO;
      String uri = ((Atomic) args[0]).stringValue();
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(URIHandler.getOutputStream(uri, true)));
      Iter it = args[1].iterate();
      try {
        Item item;
        while ((item = it.next()) != null) {
          out.write(item.toString());
          out.write('\n');
          count = count.inc();
        }
      } finally {
        it.close();
      }
      out.close();

      return count;
    } catch (IOException e) {
      throw new QueryException(e, IOFun.IO_WRITEFILE_INT_ERROR);
    }
  }
}