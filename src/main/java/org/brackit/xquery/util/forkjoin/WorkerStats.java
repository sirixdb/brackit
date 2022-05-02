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
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.util.forkjoin;

/**
 * @author Sebastian Baechle
 */
public class WorkerStats {
  private final long threadid;
  int joinCnt;
  int execCnt;
  int forkCnt;
  int stealCnt;
  int robbedCnt;
  int adoptCnt;
  int parkCnt;
  int joinParkCnt;
  long execTime;

  public WorkerStats(long threadid) {
    this.threadid = threadid;
  }

  public String toString() {
    return String.format(
        "%s: joinCnt=%s\texecCnt=%s\tforkCnt=%s\tstealCnt=%s\trobbedCnt=%s\tadoptCnt=%s\tparkCnt=%s\tjoinParkCnt=%s\texecTime=%sms",
        threadid,
        joinCnt,
        execCnt,
        forkCnt,
        stealCnt,
        robbedCnt,
        adoptCnt,
        parkCnt,
        joinParkCnt,
        execTime);
  }

  public void reset() {
    joinCnt = 0;
    execCnt = 0;
    forkCnt = 0;
    stealCnt = 0;
    robbedCnt = 0;
    adoptCnt = 0;
    execTime = 0;
    parkCnt = 0;
  }
}
