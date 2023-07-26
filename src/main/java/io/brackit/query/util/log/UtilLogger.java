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
package io.brackit.query.util.log;

import java.util.logging.LogRecord;

/**
 * @author Sebastian Baechle
 */
public class UtilLogger extends Logger {
  private java.util.logging.Logger log;

  private java.util.logging.Level[] map = new java.util.logging.Level[] { java.util.logging.Level.FINER,
      java.util.logging.Level.FINE, java.util.logging.Level.INFO, java.util.logging.Level.WARNING,
      java.util.logging.Level.SEVERE, java.util.logging.Level.SEVERE };

  private static class UtilLogRecord extends LogRecord {

    public UtilLogRecord(java.util.logging.Level level, String msg) {
      super(level, msg);
      inferCaller(UtilLogger.class, this);
    }

    private void inferCaller(Class<?> clazz, LogRecord lrec) {
      StackTraceElement stack[] = (new Throwable()).getStackTrace();
      StackTraceElement frame = null;
      String wcname = clazz.getName();

      int i = 0;
      while (i < stack.length) {
        frame = stack[i];
        String cname = frame.getClassName();
        if (cname.equals(wcname)) {
          break;
        }
        i++;
      }
      if (i < stack.length - 2) {
        frame = stack[i + 2];
        lrec.setSourceClassName(frame.getClassName());
        lrec.setSourceMethodName(frame.getMethodName());
      }
    }
  }

  public UtilLogger(java.util.logging.Logger log) {
    this.log = log;
  }

  @Override
  public String getName() {
    return log.getName();
  }

  @Override
  public void log(Level level, Object msg) {
    UtilLogRecord record = new UtilLogRecord(map[level.priority], (msg != null) ? msg.toString() : null);
    log.log(record);
  }

  @Override
  public void log(Level level, Object msg, Throwable t) {
    UtilLogRecord record = new UtilLogRecord(map[level.priority], (msg != null) ? msg.toString() : null);
    record.setThrown(t);
    log.log(record);
  }

  @Override
  public void setLevel(Level level) {
    log.setLevel(map[level.priority]);
  }

  @Override
  public Level getLevel() {
    java.util.logging.Level level = log.getLevel();
    if (level == java.util.logging.Level.FINER) {
      return Level.TRACE;
    }
    if (level == java.util.logging.Level.FINE) {
      return Level.DEBUG;
    }
    if (level == java.util.logging.Level.INFO) {
      return Level.INFO;
    }
    if (level == java.util.logging.Level.WARNING) {
      return Level.WARN;
    }
    if (level == java.util.logging.Level.SEVERE) {
      return Level.ERROR;
    }
    return Level.ERROR;
  }

  @Override
  public boolean isDebugEnabled() {
    return log.isLoggable(java.util.logging.Level.FINE);
  }

  @Override
  public boolean isInfoEnabled() {
    return log.isLoggable(java.util.logging.Level.INFO);
  }

  @Override
  public boolean isTraceEnabled() {
    return log.isLoggable(java.util.logging.Level.FINER);
  }
}