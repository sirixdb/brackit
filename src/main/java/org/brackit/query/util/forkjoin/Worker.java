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
package org.brackit.query.util.forkjoin;

/**
 * @author Sebastian Baechle
 */
public class Worker extends Thread {
  private final Pool pool;
  private volatile Deque<Task> deque;
  private volatile boolean terminate;
  Worker victim;
  final WorkerStats stats;

  protected Worker(Pool pool, int no) {
    super("FJWorker:" + no);
    setDaemon(true);
    this.stats = new WorkerStats(getId());
    this.pool = pool;
    this.deque = new SyncDeque<>();
  }

  void fork(Task task) {
    stats.forkCnt++;
    // System.out.println("FORK " + task);
    deque.push(task);
    pool.signalWork();
  }

  public void adopt(Deque<Task> queue) {
    boolean adopted = false;
    for (Task t = queue.poll(); t != null; t = queue.poll()) {
      deque.add(t);
      stats.adoptCnt++;
      adopted = true;
    }
    if (adopted) {
      pool.signalWork();
    }
  }

  void push(Task task) {
    deque.push(task);
  }

  Task poll() {
    Task task = deque.poll();
    // System.out.println("POLL " + task);
    return task;
  }

  Task pollLast() {
    return deque.pollLast();
  }

  Task steal() {
    return (deque.size() > 1) ? deque.pollLast() : null;
  }

  @Override
  public void run() {
    pool.run(this);
  }

  void join(Task t, boolean serial) {
    pool.join(this, t, serial);
  }

  public String toString() {
    return getName();
  }

  public Deque<Task> getQueue() {
    return deque;
  }

  public void dropQueue() {
    deque = new SyncDeque<>();
  }

  boolean isTerminate() {
    return terminate;
  }

  void setTerminate(boolean terminate) {
    this.terminate = terminate;
  }
}