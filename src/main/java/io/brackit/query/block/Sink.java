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
package io.brackit.query.block;

import io.brackit.query.QueryException;
import io.brackit.query.Tuple;

/**
 * <p>
 * A sink is an output channel for computations, i.e., tuples.
 * </p>
 * <p>
 * Clients must use sinks only according to the protocol:
 * <em>(fork()|partition())* -> begin() -> output()* -> (end() | fail())</em>.
 * </p>
 * <p>
 * Logically, each sink is used single-threaded, i.e., callers
 * must not concurrently access the same "logical" sink. However,
 * logically distinct sinks, may be called concurrently. If a particular
 * implementation does not fork by returning a distinct object,
 * that means that it must take care of concurrent access by itself.
 * </p>
 * <p>
 * A forked sink is considered to be independent of the base sink and the above
 * protocol must be obeyed for all forked sinks, too.
 * </p>
 * <p>
 * Depending on the type of the sink, a fork may either be really a distinct
 * object (i.e. the sink creates a fan-out) or it may be the same (i.e., the
 * sink creates a fan-in). Since all callers must follow the sink protocol, an
 * implementations must ensure to behave in every case correctly.
 * </p>
 *
 * @author Sebastian Baechle
 */
public interface Sink {
  /**
   * Output, i.e., the given tuple buffer.
   */
  void output(Tuple[] buf, int len) throws QueryException;

  /**
   * Create a fork of the current sink.
   */
  Sink fork();

  /**
   * Create a partition fork of the current sink.
   *
   * @param stopAt TODO
   */
  Sink partition(Sink stopAt);

  /**
   * Notify the sink that no further output operations will be called. This
   * does not affect forked sinks.
   */
  void end() throws QueryException;

  /**
   * Notify the sink that
   */
  void begin() throws QueryException;

  /**
   * Notify the sink that an error happened and that no further output
   * operations will be called. This does not affect forked sinks.
   */
  void fail() throws QueryException;

}