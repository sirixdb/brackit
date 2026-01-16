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
package io.brackit.query.function;

import io.brackit.query.QueryContext;
import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.Function;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.module.StaticContext;
import io.brackit.query.QueryException;

/**
 * A function that wraps another function with some arguments pre-bound.
 * Used for partial function application, e.g., func(2, ?, 7) creates a
 * partially applied function where the first and third arguments are bound.
 *
 * @author Johannes Lichtenberger
 */
public class PartiallyAppliedFunction extends AbstractFunction {
  private final Function originalFunction;
  private final Sequence[] boundArgs;
  private final int[] placeholderPositions;

  /**
   * Creates a partially applied function.
   *
   * @param originalFunction     the original function being partially applied
   * @param boundArgs            array of bound arguments (null for placeholders)
   * @param placeholderPositions positions of the placeholders in the original argument list
   */
  public PartiallyAppliedFunction(Function originalFunction, Sequence[] boundArgs, int[] placeholderPositions) {
    super(createName(originalFunction),
          createSignature(originalFunction, placeholderPositions),
          false,
          originalFunction.isUpdating());
    this.originalFunction = originalFunction;
    this.boundArgs = boundArgs;
    this.placeholderPositions = placeholderPositions;
  }

  private static QNm createName(Function originalFunction) {
    QNm origName = originalFunction.getName();
    if (origName == null) {
      return null;
    }
    return new QNm(origName.getNamespaceURI(), origName.getPrefix(), origName.getLocalName() + "#partial");
  }

  private static Signature createSignature(Function originalFunction, int[] placeholderPositions) {
    SequenceType[] origParams = originalFunction.getSignature().getParams();
    SequenceType[] newParams = new SequenceType[placeholderPositions.length];
    for (int i = 0; i < placeholderPositions.length; i++) {
      int pos = placeholderPositions[i];
      newParams[i] = pos < origParams.length ? origParams[pos] : SequenceType.ITEM_SEQUENCE;
    }
    return new Signature(originalFunction.getSignature().getResultType(), newParams);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    // The expected number of arguments equals the number of placeholders
    int expectedArgCount = placeholderPositions.length;

    // DynamicFunctionExpr may prepend closure variables to args.
    // We need to skip them and only use the last expectedArgCount arguments.
    int offset = args.length - expectedArgCount;
    if (offset < 0) {
      offset = 0;
    }

    // Combine bound arguments with the new arguments
    Sequence[] fullArgs = new Sequence[boundArgs.length];
    System.arraycopy(boundArgs, 0, fullArgs, 0, boundArgs.length);

    // Fill in the placeholder positions with the provided arguments
    for (int i = 0; i < placeholderPositions.length && (offset + i) < args.length; i++) {
      fullArgs[placeholderPositions[i]] = args[offset + i];
    }

    return originalFunction.execute(sctx, ctx, fullArgs);
  }
}
