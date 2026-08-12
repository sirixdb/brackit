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
package io.brackit.query.jdm.json;

import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.SplittableSequence;

/**
 * An {@link Array} whose <em>members</em> a storage backend can hand out in disjoint pieces that
 * are safe to iterate concurrently.
 *
 * <p>Kept separate from {@link SplittableSequence} because the two split different things, and
 * conflating them would be a lie about sequence semantics. An array item, viewed as a sequence, has
 * exactly one item — itself — so splitting <em>it</em> could only ever yield that one array. What
 * parallel scanning wants is the array unboxed: the {@code $a[]} of JSONiq, whose items are the
 * members. This interface is that, and {@code ArrayAccessExpr} is where the unboxed view picks it
 * up and presents itself as a {@link SplittableSequence}.
 *
 * <p>Splits carry no ordering guarantee; see {@link SplittableSequence} for the reasoning and for
 * the resource contract, both of which apply unchanged.
 *
 * @author The SirixDB authors
 */
public interface SplittableMembers extends Array {

  /**
   * How many disjoint pieces this array's members are worth being broken into.
   *
   * @param preferred the caller's desired number of pieces, typically the worker count; > 0
   * @return the number of pieces to actually request, in {@code [1, preferred]}; {@code 1} means
   *         "not worth splitting" and the caller must iterate serially
   */
  int memberSplitCount(int preferred);

  /**
   * The {@code index}-th of {@code total} disjoint pieces of this array's members.
   *
   * @param index the piece to return, in {@code [0, total)}
   * @param total the number of pieces, as returned by {@link #memberSplitCount(int)}
   * @return the piece, never {@code null}; empty is legal
   */
  Sequence memberSplit(int index, int total);
}
