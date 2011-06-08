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
package org.brackit.xquery;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.util.Cfg;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * @author Sebastian Baechle
 *
 */
public class LetBindLiftTest extends XQueryBaseTest {
	
	@Test
	public void forLetWhereConditional() throws Exception {
		Cfg.set(XQuery.DEBUG_CFG, true);
		Cfg.set(XQuery.DEBUG_DIR_CFG, "/media/ramdisk/");
		Cfg.set(XQuery.UNNEST_CFG, true);
		Sequence res = new XQuery(
				"for $a in (1,2,3) " +
				"let $b := 5" +
				"let $c := " +
				"	for $d in (2 to 1)" +
				"	let $e := " +
				"		for $f in (4,5,6) " +
				"		return $f " +
				"	where exactly-one($d) " +
				"	return $d " +
				"return 'no'").execute(ctx);
		Str no = new Str("no");
		ResultChecker.dCheck(ctx, new ItemSequence(no, no, no), res);
		print(res);
	}
	
	@Test
	public void orderByBatched() throws Exception {
		Cfg.set(XQuery.DEBUG_CFG, true);
		Cfg.set(XQuery.DEBUG_DIR_CFG, "/media/ramdisk/");
		Cfg.set(XQuery.UNNEST_CFG, false);
		Sequence res = new XQuery(
				"for $a in (1,2,3) " +
				"let $c := " +
				"	for $d in (6,5,4) " +
				"	for $e in (7,8,9) " +
				"	order by $d ascending, $e descending " +
				"	return ($d,$e) " +
				"return ($a, $c)").execute(ctx);
		print(res);
		ResultChecker.dCheck(ctx, intSequence(1, 4, 9, 4, 8, 4, 7, 5, 9, 5, 8,
				5, 7, 6, 9, 6, 8, 6, 7, 2, 4, 9, 4, 8, 4, 7, 5, 9, 5, 8, 5, 7,
				6, 9, 6, 8, 6, 7, 3, 4, 9, 4, 8, 4, 7, 5, 9, 5, 8, 5, 7, 6, 9,
				6, 8, 6, 7), res);
	}
	
	@Test
	public void orderBy2() throws Exception {
		XQuery.DEBUG = true;
		XQuery.JOIN_DETECTION = false;
		XQuery.UNNEST = true;
		XQuery.DEBUG_DIR = "/media/ramdisk/";
		Cfg.set(XQuery.UNNEST_CFG, true);
		Cfg.set(XQuery.JOIN_DETECTION_CFG, false);
		new XQuery(
				"	for $d in (3,2,1) " +
				"	for $e in (4,5,6) " +
				"	order by $d ascending, $e descending " +
				"	return ($d,$e)").serialize(ctx, System.out);	
	}
	
	@Test
	public void simpleLeftJoin() throws Exception {
		Cfg.set(XQuery.DEBUG_CFG, true);
		Cfg.set(XQuery.DEBUG_DIR_CFG, "/media/ramdisk/");
		Cfg.set(XQuery.UNNEST_CFG, true);
		Cfg.set(XQuery.JOIN_DETECTION_CFG, true);
		XQuery.JOIN_DETECTION = true;
		Sequence res = new XQuery(
				"for $a in (1 to 5) " +
				"let $b := " +
				"	for $c in (2 to 4)" +
				"	where $a = $c " +
				"	return $c " +
				"return ($a,$b)").execute(ctx);
		print(res);
		ResultChecker.dCheck(ctx, intSequence(1,2,2,3,3,4,4,5), res);		
	}
	
	@Test
	public void nestedLeftJoin() throws Exception {
		Cfg.set(XQuery.DEBUG_CFG, true);
		Cfg.set(XQuery.DEBUG_DIR_CFG, "/media/ramdisk/");
		Cfg.set(XQuery.UNNEST_CFG, true);
		Cfg.set(XQuery.JOIN_DETECTION_CFG, true);
		XQuery.JOIN_DETECTION = true;
		Sequence res = new XQuery(
				"for $a in (1 to 5) " +
				"let $b := " +
				"	for $c in (1,0,1) " +
				"	let $f := if ($c) then (2,4) else () " +	
				"	let $d := " +
				"		for $e in $f " +
				"		where $e = $a " +
				"		return $e " +
				"	return $d " +
				"return ($a,$b)").execute(ctx);
		print(res);
		ResultChecker.dCheck(ctx, intSequence(1,2,2,2,3,4,4,4,5), res);		
	}
	
	@Test
	public void paper() throws Exception {
		Cfg.set(XQuery.DEBUG_CFG, true);
		Cfg.set(XQuery.DEBUG_DIR_CFG, "/media/ramdisk/");
		Cfg.set(XQuery.UNNEST_CFG, true);
		Cfg.set(XQuery.JOIN_DETECTION_CFG, true);
		XQuery.JOIN_DETECTION = true;
		Sequence res = new XQuery(
				"for $a in (1,2,3) " +
				"let $c:= " +
				"for $b in (2,3,4) " +
				"where $a<$b " +
				"return $b " +
				"return ($a,$c)").execute(ctx);
		print(res);
		ResultChecker.dCheck(ctx, intSequence(1,2,3,4,2,3,4,3,4), res);		
	}
	
	@Test
	public void simple() throws Exception {
		Cfg.set(XQuery.DEBUG_CFG, true);
		Cfg.set(XQuery.DEBUG_DIR_CFG, "/media/ramdisk/");
		Cfg.set(XQuery.UNNEST_CFG, true);
		Cfg.set(XQuery.JOIN_DETECTION_CFG, true);
		XQuery.JOIN_DETECTION = true;
		Sequence res = new XQuery(
				"for $a in (1,2,3) " +
				"let $b:= 2 " +
				"where $a=$b " +
				"return $b ").execute(ctx);
		print(res);
		ResultChecker.dCheck(ctx, intSequence(2), res);		
	}
	
	@Test
	public void nestedLeftJoin2() throws Exception {
		Cfg.set(XQuery.DEBUG_CFG, true);
		Cfg.set(XQuery.DEBUG_DIR_CFG, "/media/ramdisk/");
		Cfg.set(XQuery.UNNEST_CFG, true);
		Cfg.set(XQuery.JOIN_DETECTION_CFG, true);
		XQuery.JOIN_DETECTION = true;
		Sequence res = new XQuery(
				"for $z in 1 " +
				"let $x := " +
				"for $a in (1 to 5) " +
				"let $b := " +
				"	for $c in (2,4) " +
				"	let $f := if ($c) then (2,4) else () " +
				"	order by $c " +	
				"	let $d := " +
				"		for $e in $f " +
				"		let $g := 'ignore' " +		
				"		where $c = $a " +
				"		return $e " +
				"	return $d " +
				"return ($a,$b) " +
				"return $x").execute(ctx);
		print(res);
		ResultChecker.dCheck(ctx, intSequence(1, 2, 2, 4, 3, 4, 2, 4, 5), res);		
	}
	
	private Sequence intSequence(int... v) {
		Int32[] s = new Int32[v.length];
		for (int i = 0; i < v.length; i++) {
			s[i] = new Int32(v[i]);
		}
		return new ItemSequence(s);
	}
	
	@Test
	public void testMe() throws Exception {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(true);
			factory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder builder = factory.newDocumentBuilder();			
			Document document = builder.parse(new InputSource(new StringReader(
					"<a>" +
					"	<b>" +
					"		5" +
					"	</b>" +
					"	<c>" +
					"		6" +
					"	</c>" +
					"</a>")));
			Element root = document.getDocumentElement();
			travers(root);
			fix(root);
			System.out.println("-------------");
			travers(root);
		} catch (Exception e) {
			throw new DocumentException(
					"An error occured while creating DOM input source: %s", e
							.getMessage());
		}		
	}
	
	private void travers(Node node) {
		if (node == null) {
			return;
		}
		System.out.println(node.getNodeType() + " " + node.getNodeName() + " '" + node.getNodeValue() + "'");
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			travers(children.item(i));
		}
	}
	
	private boolean fix(Node node) {
		if (node == null) {
			return false;
		}
		if (node.getNodeType() == Node.TEXT_NODE) {
			String trimmed = node.getTextContent().trim();
			if (trimmed.isEmpty()) {
				node.getParentNode().removeChild(node);
				return true;
			} else {
				node.setNodeValue(trimmed);
			}
		} else if (node.getNodeType() == Node.ELEMENT_NODE) { 
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				if (fix(children.item(i))) {
					i--; // child deleted, step one back
				}
			}
		}
		return false;
	}

	
	@Test
	public void orderBy() throws Exception {
		Cfg.set(XQuery.DEBUG_CFG, true);
		Cfg.set(XQuery.DEBUG_DIR_CFG, "/media/ramdisk/");
		Cfg.set(XQuery.UNNEST_CFG, true);
		new XQuery(
				"for $a in ('A','B','C') " +
				"let $c := " +
				"	for $d in (3,2,1) " +
				"	for $e in (4,5,6) " +
				"	order by $d ascending, $e descending " +
				"	return ($d,$e) " +
				"return ($a, $c)").serialize(ctx, System.out);	
	}
}
