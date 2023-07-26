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
package org.brackit.query.function.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.brackit.query.QueryContext;
import org.brackit.query.QueryException;
import org.brackit.query.atomic.Atomic;
import org.brackit.query.atomic.QNm;
import org.brackit.query.atomic.Str;
import org.brackit.query.function.AbstractFunction;
import org.brackit.query.module.StaticContext;
import org.brackit.query.sequence.BaseIter;
import org.brackit.query.sequence.LazySequence;
import org.brackit.query.util.io.URIHandler;
import org.brackit.query.jdm.Item;
import org.brackit.query.jdm.Iter;
import org.brackit.query.jdm.Sequence;
import org.brackit.query.jdm.Signature;
import org.brackit.query.jdm.type.AtomicType;
import org.brackit.query.jdm.type.Cardinality;
import org.brackit.query.jdm.type.SequenceType;

/**
 * @author Sebastian Baechle
 */
public class Readline extends AbstractFunction {
  public static final QNm DEFAULT_NAME = new QNm(IOFun.IO_NSURI, IOFun.IO_PREFIX, "readline");

  public Readline() {
    this(DEFAULT_NAME);
  }

  public Readline(QNm name) {
    super(name,
          new Signature(new SequenceType(AtomicType.STR, Cardinality.ZeroOrMany),
                        new SequenceType(AtomicType.STR, Cardinality.One)),
          true);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    final String uri = ((Atomic) args[0]).stringValue();
    return new LazySequence() {
      @Override
      public Iter iterate() {
        return new BaseIter() {
          BufferedReader in;

          @Override
          public Item next() throws QueryException {
            try {
              if (in == null) {
                in = new BufferedReader(new InputStreamReader(URIHandler.getInputStream(uri)));
              }
              String line = in.readLine();
              return (line != null) ? new Str(line) : null;
            } catch (Exception e) {
              throw new QueryException(e, IOFun.IO_LOADFILE_INT_ERROR);
            }
          }

          @Override
          public void close() {
            if (in != null) {
              try {
                in.close();
              } catch (IOException e) {
                // ignore
              }
            }
          }
        };
      }
    };
  }
}