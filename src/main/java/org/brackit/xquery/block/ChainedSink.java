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

import java.util.concurrent.locks.LockSupport;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.util.forkjoin.Deque;
import org.brackit.xquery.util.forkjoin.Task;
import org.brackit.xquery.util.forkjoin.Worker;

/**
 * A {@link ChainedSink}.
 * 
 * @author Sebastian Baechle
 * 
 */
public abstract class ChainedSink implements Sink {

    private static final boolean SUSPEND = true;

    private static final int NO_TOKEN = 0;
    private static final int WAIT_TOKEN = 1;
    private static final int HAS_TOKEN = 2;
    private static final int HAS_START_TOKEN = 3;
    private static final int FAILED = 4;

    private ChainedSink next;
    private volatile int state;
    private volatile Deque<Task> deposit;
    private volatile Thread blocked;

    public ChainedSink() {
        this.state = HAS_START_TOKEN;
    }

    public final ChainedSink fork() {
        ChainedSink fork = doFork();
        fork.next = next;
        fork.state = NO_TOKEN;
        return (next = fork);
    }

    protected abstract ChainedSink doFork();

    public final ChainedSink partition(Sink stopAt) {
        ChainedSink partition = doPartition(stopAt);
        if (chainPartitions()) {
            partition.next = next;
            partition.state = NO_TOKEN;
            next = partition;
        }
        return partition;
    }

    protected boolean chainPartitions() {
        return false;
    }

    protected abstract ChainedSink doPartition(Sink stopAt);

    protected void processPending() throws QueryException {
    }

    protected boolean hasPending() {
        return false;
    }

    protected void clearPending() {
    }

    protected boolean yield() {
        return false;
    }

    protected void unyield() {
    }

    protected void setPending(Tuple[] buf, int len) throws QueryException {
    }

    protected void doOutput(Tuple[] buf, int len) throws QueryException {
    }

    protected void doBegin() throws QueryException {
    }

    protected void doEnd() throws QueryException {
    }

    protected void doFirstBegin() throws QueryException {
    }

    protected void doFinalEnd() throws QueryException {
    }

    protected void doFail() throws QueryException {
    }

    @Override
    public final void output(Tuple[] buf, int len) throws QueryException {
        int s = state;
        if (s == HAS_TOKEN) {
            if (hasPending()) {
                processPending();
            }
            doOutput(buf, len);
        } else if (s == FAILED) {
            if (hasPending()) {
                clearPending();
            }
            promoteFailure();
            throw new QueryException(ErrorCode.BIT_DYN_ABORTED_ERROR);
        } else {
            setPending(buf, len);
        }
    }

    public final void begin() throws QueryException {
        if (state == HAS_START_TOKEN) {
            doFirstBegin();
            state = HAS_TOKEN;
        }
        doBegin();
    }

    public final void fail() throws QueryException {
        state = FAILED;
        promoteFailure();
        doFail();
    }

    public final void end() throws QueryException {
        int s = state;
        if (s == NO_TOKEN) {
            boolean hasPending = hasPending();
            Worker worker = null;
            Deque<Task> queue = null;

            if (hasPending) {
                // deposit reference to work queue
                // for expected hand-over
                worker = (Worker) Thread.currentThread();
                if (!SUSPEND) {
                    queue = worker.getQueue();
                    deposit = queue;
                } else {
                    blocked = worker;
                }
            }

            // attempt to put predecessor in
            // charge of pending work
            if (compareAndSet(NO_TOKEN, WAIT_TOKEN)) {
                // drop local work queue only if necessary
                // or token was not granted concurrently
                // * NOTE: Sebastian had a ```yield()``` in the right hand side of the &&.
                // * However, for some reason it kept throwing an error about it so I just
                // * replaced the function call with false since that is what '''yield()'''
                // * returns
                if ((hasPending)
                        && ((false) || (!compareAndSet(queue, null)))) {
                    if (!SUSPEND) {
                        // drop local queue
                        worker.dropQueue();
                    } else {
                        LockSupport.park(this);
                    }
                }
                return;
            }
            s = state;
        }
        if (s == HAS_TOKEN) {
            endWithToken();
        } else if (s == FAILED) {
            endWithFailure();
        }
    }

    private void endWithFailure() throws QueryException {
        if (hasPending()) {
            clearPending();
        }
        promoteFailure();
    }

    private void endWithToken() throws QueryException {
        if (hasPending()) {
            processPending();
        }
        promoteToken();
        doEnd();
    }

    private void promoteToken() throws QueryException {
        // process pending work of finished successors
        ChainedSink n = next;
        next = null;
        while ((n != null) && (!n.compareAndSet(NO_TOKEN, HAS_TOKEN))) {
            if (n.state == WAIT_TOKEN) {
                n.state = HAS_TOKEN;
                takeover(n);
                n = n.next;
            } else {
                n.promoteFailure();
                return;
            }
        }
        if (n == null) {
            doFinalEnd();
        }
    }

    private void promoteFailure() throws QueryException {
        ChainedSink n = next;
        // forward promote failure to forked sinks
        while ((n != null) && (!n.compareAndSet(NO_TOKEN, FAILED))) {
            if (n.state == FAILED) {
                break;
            }
            n.clearPending(); // allow gc
            // synchronized (n)
            {
                if (n.deposit != null) {
                    // TODO cleanup tasks
                }
            }
            doFail();
            n = n.next;
        }
    }

    private void takeover(ChainedSink n) throws QueryException {
        if (SUSPEND) {
            LockSupport.unpark(n.blocked);
        }
        if (n.hasPending()) {
            n.processPending();
        }
        Deque<Task> queue = n.deposit;
        if ((queue != null) && (n.compareAndSet(queue, null))) {
            n.unyield();
            ((Worker) Thread.currentThread()).adopt(queue);
        }
        n.doEnd();
    }

    private boolean compareAndSet(int expected, int set) {
        synchronized (this) {
            if (state == expected) {
                state = set;
                return true;
            }
            return false;
        }
    }

    private boolean compareAndSet(Deque<Task> expected, Deque<Task> set) {
        synchronized (this) {
            if (deposit == expected) {
                deposit = set;
                return true;
            }
            return false;
        }
    }
}