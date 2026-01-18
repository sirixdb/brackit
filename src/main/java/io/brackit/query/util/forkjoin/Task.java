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
package io.brackit.query.util.forkjoin;

import java.util.concurrent.RecursiveAction;

/**
 * Task abstraction for parallel execution.
 * Extends Java's RecursiveAction for better performance and JVM optimization.
 *
 * @author Sebastian Baechle
 */
public abstract class Task extends RecursiveAction {

  private volatile Throwable throwable;

  /**
   * The computation to be performed by this task.
   * Subclasses must implement this method.
   */
  protected abstract void doCompute() throws Throwable;

  @Override
  protected final void compute() {
    try {
      doCompute();
    } catch (Throwable e) {
      throwable = e;
      completeExceptionally(e);
    }
  }

  /**
   * Serial join - for compatibility with existing code.
   * Blocks until task completes.
   */
  public void joinSerial() {
    join();
  }

  /**
   * Check if task has finished.
   */
  public boolean finished() {
    return isDone();
  }

  /**
   * Get any error that occurred during execution.
   */
  public Throwable getError() {
    if (throwable != null) {
      return throwable;
    }
    if (isCompletedAbnormally()) {
      return getException();
    }
    return null;
  }
}
