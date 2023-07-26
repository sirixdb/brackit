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
package io.brackit.query.compiler.optimizer.walker.topdown;

import static io.brackit.query.compiler.XQ.GeneralCompEQ;
import static io.brackit.query.compiler.XQ.GeneralCompGE;
import static io.brackit.query.compiler.XQ.GeneralCompGT;
import static io.brackit.query.compiler.XQ.GeneralCompLE;
import static io.brackit.query.compiler.XQ.GeneralCompLT;
import static io.brackit.query.compiler.XQ.GeneralCompNE;
import static io.brackit.query.compiler.XQ.ValueCompEQ;
import static io.brackit.query.compiler.XQ.ValueCompGE;
import static io.brackit.query.compiler.XQ.ValueCompGT;
import static io.brackit.query.compiler.XQ.ValueCompLE;
import static io.brackit.query.compiler.XQ.ValueCompLT;
import static io.brackit.query.compiler.XQ.ValueCompNE;

import io.brackit.query.util.Cmp;
import io.brackit.query.compiler.AST;

/**
 * @author Sebastian Baechle
 * @author Johannes Lichtenberger
 */
public final class CmpUtil {
  public static Cmp cmp(AST cmpNode) {
    return switch (cmpNode.getType()) {
      case ValueCompEQ, GeneralCompEQ -> Cmp.eq;
      case ValueCompGE, GeneralCompGE -> Cmp.ge;
      case ValueCompLE, GeneralCompLE -> Cmp.le;
      case ValueCompLT, GeneralCompLT -> Cmp.lt;
      case ValueCompGT, GeneralCompGT -> Cmp.gt;
      case ValueCompNE, GeneralCompNE -> Cmp.ne;
      default -> throw new IllegalArgumentException();
    };
  }

  public static int type(Cmp cmp, boolean isGCmp) {
    return switch (cmp) {
      case eq -> isGCmp ? GeneralCompEQ : ValueCompEQ;
      case ge -> isGCmp ? GeneralCompGE : ValueCompGE;
      case gt -> isGCmp ? GeneralCompGT : ValueCompGT;
      case le -> isGCmp ? GeneralCompLE : ValueCompLE;
      case lt -> isGCmp ? GeneralCompLT : ValueCompLT;
      case ne -> isGCmp ? GeneralCompNE : ValueCompNE;
    };
  }

  public static boolean isGCmp(AST cmp) {
    return switch (cmp.getType()) {
      case GeneralCompEQ, GeneralCompGE, GeneralCompGT, GeneralCompLE, GeneralCompLT -> true;
      default -> false;
    };
  }
}
