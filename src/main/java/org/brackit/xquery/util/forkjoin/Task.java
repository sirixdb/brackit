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
package org.brackit.xquery.util.forkjoin;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public abstract class Task {

    private static final AtomicIntegerFieldUpdater<Task> STATUS_CAS = AtomicIntegerFieldUpdater
            .newUpdater(Task.class, "status");

    private static final AtomicIntegerFieldUpdater<Task> PROCESS_CAS = AtomicIntegerFieldUpdater
            .newUpdater(Task.class, "process");

    private static final int NOTIFY = -1;
    private static final int NEW = 0;
    private static final int SUCCESS = 1;
    private static final int ERROR = 2;

    volatile Thread join;
    volatile int status = NEW;
    volatile int process = 0;
    Throwable throwable;

    public abstract void compute() throws Throwable;

    boolean exec() {
        if (!PROCESS_CAS.compareAndSet(this, 0, 1)) {
            return false;
        }
        try {
            int s = status;
            if (s > 0) {
                throw new RuntimeException("Illegal state: " + s);
            }
            compute();
            setStatus(SUCCESS);
        } catch (Throwable e) {
            e.printStackTrace();
            throwable = e;
            setStatus(ERROR);
        }
        return true;
    }

    private int setStatus(int newStatus) {
        int s;
        while (true) {
            s = status;
            if (s > 0) {
                // job is done
                return s;
            }
            if (STATUS_CAS.compareAndSet(this, s, newStatus)) {
                if (s < 0) {
                    synchronized (this) {
                        notifyAll();
                        if (join != null) {
                            LockSupport.unpark(join);
                        }
                    }
                }
                return s;
            }
        }
    }

    public void fork() {
        ((Worker) Thread.currentThread()).fork(this);
    }

    public void join() {
        Thread me;
        if ((me = Thread.currentThread()) instanceof Worker) {
            Worker w = (Worker) me;
            w.join(this, false);
        } else {
            externalWaitForFinish();
        }
    }

    public void joinSerial() {
        Thread me;
        if ((me = Thread.currentThread()) instanceof Worker) {
            Worker w = (Worker) me;
            w.join(this, true);
        } else {
            externalWaitForFinish();
        }
    }

    public boolean finished() {
        int s = status;
        return (s > 0);
    }

    public Throwable getError() {
        return throwable;
    }

    private void externalWaitForFinish() {
        int s = status;
        if (s <= 0) {
            synchronized (this) {
                while ((s = status) <= 0) {
                    if (s == 0) {
                        setStatus(NOTIFY);
                    } else {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
    }

    public void park(Thread t) {
        int s = status;
        if (s <= 0) {
            return;
        }
        synchronized (this) {
            if ((s = status) <= 0) {
                Thread o = join;
                join = t;
                LockSupport.park();
                if (o != null) {
                    join = o;
                    LockSupport.unpark(o);
                }
            }
        }
    }
}