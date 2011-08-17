/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
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
package org.brackit.xquery.xqts;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.brackit.xquery.util.log.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 
 * @author Max Bechtold
 * 
 */
public class XMLComparator {
	private static Logger log = Logger.getLogger(XMLComparator.class);
	private static DocumentBuilder builder;

	final static String ROOT_NODE = "comp";

	private static Writer writer;

	private static boolean debug = false;
	static {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			builder = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			log.error(e);
			builder = null;
		}

		writer = new Writer(true);
	}

	public static boolean compareAsXML(Document result, String expected) {
		Document exp = null;

		try {
			exp = builder.parse(new InputSource(new StringReader(String.format(
					"<%s>%s</%s>", ROOT_NODE, expected, ROOT_NODE))));
		} catch (SAXException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}

		StringWriter sw = new StringWriter();
		writer.setOutput(sw);
		writer.setCanonical(true);
		writer.write(result);

		String canonicalResult = sw.toString();

		sw = new StringWriter();
		writer.setOutput(sw);
		writer.write(exp);

		String canonicalExpected = sw.toString();

		if (debug) {
			dumpComparison(result, exp, canonicalResult, canonicalExpected);
		}

		boolean equals = canonicalExpected.equals(canonicalResult);

		// if (!equals)
		// {
		// dumpComparison(result, exp, canonicalResult, canonicalExpected);
		// }

		return equals;
	}

	private static void dumpComparison(Document result, Document exp,
			String canonicalResult, String canonicalExpected) {
		StringWriter sw;
		sw = new StringWriter();
		writer.setOutput(sw);
		writer.setCanonical(false);
		writer.write(result);
		System.out.println("Actual document uncanonicalized:");
		System.out.println(sw.toString());

		System.out.println("\nActual document canonicalized:");
		System.out.println(canonicalResult);

		sw = new StringWriter();
		writer.setOutput(sw);
		writer.write(exp);
		System.out.println("\nExpected document uncanonicalized:");
		System.out.println(sw.toString());

		System.out.println("\nExpected document canonicalized:");
		System.out.println(canonicalExpected);
	}

}
