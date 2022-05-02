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
package org.brackit.xquery.block;

import java.util.Arrays;
import java.util.concurrent.Semaphore;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;

/**
 * A {@link SerialSink} creates a fan-in, i.e., all forked sinks are chained so
 * that (concurrent) output to all forks is serialized and the order is
 * preserved.
 * 
 * @author Sebastian Baechle
 * 
 */
public abstract class SerialSink extends ChainedSink {

    final Semaphore sem;
    Tuple[] pending;
    int pLen;
    int held;

    public SerialSink(int permits) {
        this.sem = (permits >= 0) ? new Semaphore(permits) : null;
    }

    protected SerialSink(Semaphore sem) {
        this.sem = sem;
    }

    protected abstract void doOutput(Tuple[] buf, int len)
            throws QueryException;

    @Override
    protected void processPending() throws QueryException {
        doOutput(pending, pLen);
        pending = null;
    }

    @Override
    protected boolean hasPending() {
        return pending != null;
    }

    @Override
    protected void clearPending() {
        pending = null;
    }

    @Override
    protected boolean yield() {
        if (sem != null) {
            if (!sem.tryAcquire(pLen)) {
                held = pLen;
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    protected void unyield() {
        if (sem != null) {
            int h = held;
            held = 0;
            sem.release(h);
        }
    }

    @Override
    protected void setPending(Tuple[] buf, int len) throws QueryException {
        if (pending == null) {
            pending = buf;
            pLen = len;
        } else {
            int newPLen = pLen + len;
            if (newPLen > pending.length) {
                pending = Arrays.copyOfRange(pending, 0, newPLen);
            }
            System.arraycopy(buf, 0, pending, pLen, len);
            pLen = newPLen;
        }
    }
}