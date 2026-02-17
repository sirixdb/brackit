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
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.operator.Operator;
import io.brackit.query.operator.Print;
import io.brackit.query.jdm.DocumentException;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.node.Node;

/**
 * Extended compiler for bottom-up compilation of unnested flwor expressions.
 * Uses {@link SequentialPipelineStrategy} by default.
 *
 * @author Sebastian Baechle
 */
public class TopDownTranslator extends Compiler {

  public TopDownTranslator(Map<QNm, Str> options) {
    super(options, new SequentialPipelineStrategy());
  }

  public TopDownTranslator(Map<QNm, Str> options, PipelineStrategy pipelineStrategy) {
    super(options, pipelineStrategy);
  }

  protected Operator wrapDebugOutput(Operator root) {
    return new Print(root, System.out) {
      @Override
      public String asString(QueryContext ctx, Sequence sequence) throws QueryException {
        if (sequence == null) {
          return "";
        }
        if (sequence instanceof Item) {
          return (sequence instanceof Node<?>) ? nodeAsString((Node<?>) sequence) : sequence.toString();
        }
        StringBuilder s = new StringBuilder("(");
        try (Iter it = sequence.iterate()) {
          for (Item item = it.next(); item != null; item = it.next()) {
            s.append(sequence);
            s.append(", ");
          }
        } finally {
          s.append(")");
        }
        return s.toString();
      }

      private String nodeAsString(Node<?> node) {
        try {
          return switch (node.getKind()) {
            case ELEMENT -> "<" + node.getName() + ">";
            case ATTRIBUTE -> node.getName() + "='" + node.getValue() + "'";
            case DOCUMENT -> "doc(" + node.getCollection().getName() + ")";
            default -> node.getValue().stringValue();
          };
        } catch (DocumentException e) {
          e.printStackTrace();
          return "";
        }
      }
    };
  }
}
