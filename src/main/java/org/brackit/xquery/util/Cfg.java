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
package org.brackit.xquery.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.brackit.xquery.util.log.Logger;

/**
 * 
 * The JVM -D arguments override corresponding properties configured in
 * config.properties and defaults.properties. All the public getters throw a
 * <code>RuntimeException</code> if the requested property is not present,
 * except for The <code>check</code> method, which returns a <code>null</code>
 * in this case.
 * 
 * @author Sebastian Baechle
 * @author Ou Yi
 * 
 */
public class Cfg {

	public final static String DEFAULTS_FILE = "defaults.properties";

	public final static String CONFIG_FILE = "config.properties";

	protected static Properties defaults = new Properties();

	protected static Properties properties = new Properties();

	static {
		try {
			properties.load(new FileInputStream(Cfg.CONFIG_FILE));
		} catch (IOException e) {
		}
	}

	public static void set(String key, String val) {
		System.setProperty(key, val);
	}

	public static String asString(String key) {
		String val = getValue(key);
		if (val == null) {
			throw new RuntimeException("no property value found for key: "
					+ key);
		}
		return val;
	}

	public static String asString(String key, String dflt) {
		String s = getValue(key);
		return (s != null) ? s : dflt;
	}

	public static String check(String key) {
		return getValue(key);
	}

	private static String getValue(String key) {
		String val = System.getProperty(key);
		if (val != null) {
			return val;
		}
		val = properties.getProperty(key);
		if (val != null) {
			return val;
		}
		return defaults.getProperty(key);
	}

	public static void set(String key, int val) {
		System.setProperty(key, Integer.toString(val));
	}

	public static int asInt(String key) {
		return Integer.parseInt(asString(key));
	}

	public static int asInt(String key, int dflt) {
		String s = getValue(key);
		return (s != null) ? Integer.parseInt(s) : dflt;
	}

	public static void set(String key, long val) {
		System.setProperty(key, Long.toString(val));
	}

	public static long asLong(String key) {
		return Long.parseLong(asString(key));
	}

	public static long asLong(String key, long dflt) {
		String s = getValue(key);
		return (s != null) ? Long.parseLong(s) : dflt;
	}

	public static void set(String key, boolean val) {
		System.setProperty(key, Boolean.toString(val));
	}

	public static boolean asBool(String key) {
		return Boolean.parseBoolean(asString(key));
	}

	public static boolean asBool(String key, boolean dflt) {
		String s = getValue(key);
		return (s != null) ? Boolean.parseBoolean(s) : dflt;
	}

	public static void set(String key, double val) {
		System.setProperty(key, Double.toString(val));
	}

	public static double asDouble(String key) {
		return Double.parseDouble(asString(key));
	}

	public static double asDouble(String key, double dflt) {
		String s = getValue(key);
		return (s != null) ? Double.parseDouble(s) : dflt;
	}
}
