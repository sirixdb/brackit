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
package org.brackit.xquery.util.log;

import org.brackit.xquery.util.Cfg;

/**
 * Log wrapper with subset of Logger interface from log4j
 * 
 * @author Sebastian Baechle
 * 
 */
public abstract class Logger {

	public enum Level {
		TRACE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4), FATAL(5);
		public final int priority;

		private Level(int prioprity) {
			this.priority = prioprity;
		}
	};

	static {
		String useLog = Cfg.asString(LogFactory.class.getName(),
				UtilLogFactory.class.getName());
		try {
			factory = (LogFactory) Class.forName(useLog).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static LogFactory factory;

	public static Logger getRootLogger() {
		return factory.getRootLogger();
	}
	
	public static Logger getLogger(String name) {
		return factory.getLogger(name);
	}

	public static Logger getLogger(Class<?> cls) {
		return getLogger(cls.getName());
	}

	public abstract void setLevel(Level level);

	public abstract Level getLevel();

	public abstract boolean isTraceEnabled();

	public abstract boolean isDebugEnabled();

	public abstract boolean isInfoEnabled();

	public abstract void log(Level fatal, Object msg, Throwable t);

	public abstract void log(Level fatal, Object msg);
	
	public abstract String getName();

	public void trace(Object msg) {
		log(Level.TRACE, msg);
	}

	public void trace(Object msg, Throwable t) {
		log(Level.TRACE, msg, t);
	}

	public void debug(Object msg) {
		log(Level.DEBUG, msg);
	}

	public void debug(Object msg, Throwable t) {
		log(Level.DEBUG, msg, t);
	}

	public void info(Object msg) {
		log(Level.INFO, msg);
	}

	public void info(Object msg, Throwable t) {
		log(Level.INFO, msg, t);
	}

	public void warn(Object msg) {
		log(Level.WARN, msg);
	}

	public void warn(Object msg, Throwable t) {
		log(Level.WARN, msg, t);
	}

	public void error(Object msg) {
		log(Level.ERROR, msg);
	}

	public void error(Object msg, Throwable t) {
		log(Level.ERROR, msg, t);
	}

	public void fatal(Object msg) {
		log(Level.FATAL, msg);
	}

	public void fatal(Object msg, Throwable t) {
		log(Level.FATAL, msg, t);
	}
}
