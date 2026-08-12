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
package io.brackit.query.jdm;

/**
 * A sequence a storage backend can hand out in disjoint pieces that are safe to iterate
 * concurrently, on different threads.
 *
 * <p>This is the seam morsel-driven parallelism needs, and it exists because the alternative does
 * not work. A morsel source that wraps an ordinary {@link Sequence} has to pull items serially and
 * hand them to workers, which leaves the scan — measured at 58-59 % of pipeline time on a 3.5 M
 * record JSON corpus — on one thread and caps the whole plan near 1.7x by Amdahl's law. Splitting
 * at the source instead puts the scan itself on every worker.
 *
 * <p>Implementations are expected to partition in whatever unit is cheap for them, which is rarely
 * item index. A backend that stores records in pages can hand out page ranges in constant time,
 * whereas resolving the item at position {@code i} may cost a walk of length {@code i} — the whole
 * point is to let the backend choose. Consequently <b>splits carry no ordering guarantee</b>: their
 * union is the original sequence as a bag, not as a list. Callers that need document order must not
 * use this interface.
 *
 * @author The SirixDB authors
 */
public interface SplittableSequence extends Sequence {

  /**
   * How many disjoint pieces this sequence is worth breaking into, given the caller's preferred
   * degree of parallelism.
   *
   * <p>Returning {@code 1} (or less) means "do not split" and callers must fall back to ordinary
   * serial iteration — implementations should say so whenever the sequence is too small for the
   * split to pay for the transactions and threads it costs.
   *
   * @param preferred the caller's desired number of pieces, typically the worker count; > 0
   * @return the number of pieces to actually request, in {@code [1, preferred]}
   */
  int splitCount(int preferred);

  /**
   * The {@code index}-th of {@code total} disjoint pieces.
   *
   * <p>Every item of this sequence appears in exactly one piece. The returned sequence is
   * independent of both this one and its siblings: iterating several of them on different threads
   * at the same time is the intended use, so an implementation that shares a cursor with the
   * original must open its own here.
   *
   * <p>Resources a piece acquires (a read transaction, say) are released when the piece's
   * {@link Iter} is closed, so a caller must close every iterator it opens even on an error path.
   *
   * @param index the piece to return, in {@code [0, total)}
   * @param total the number of pieces, as returned by {@link #splitCount(int)}
   * @return the piece, never {@code null}; empty is legal
   */
  Sequence split(int index, int total);
}
