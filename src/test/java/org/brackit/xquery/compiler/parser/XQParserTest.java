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
package org.brackit.xquery.compiler.parser;

import static org.junit.Assert.assertEquals;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.XQueryBaseTest;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 * 
 */
public class XQParserTest extends XQueryBaseTest {

	@Test
	public void ncname() throws Exception {
		new XQParser("declare").parse();
	}

	@Test
	public void qname() throws Exception {
		new XQParser("declare").parse();
	}

	@Test
	public void emptyElement() throws Exception {
		new XQParser("<a/>").parse();
	}

	@Test
	public void emptyElementWithWS() throws Exception {
		new XQParser(" < a /> ").parse();
	}

	@Test
	public void nestedElements() throws Exception {
		new XQParser("<a><b></b></a>").parse();
	}

	@Test
	public void nestedElementsWithWS() throws Exception {
		new XQParser(" < a> <  b> </b  \n></ \n\na>").parse();
	}

	@Test
	public void emptyElementWithAttribute() throws Exception {
		new XQParser("<a b='a'/>").parse();
	}

	@Test
	public void elementWithAttribute() throws Exception {
		new XQParser("<a b='a'></a>").parse();
	}
	
	@Test
	public void elementsWithComplexContent() throws Exception {
		new XQParser("<a>1&amp;<b/>12 {'aha'}soso<c/></a>").parse();
	}
	
	@Test
	public void elementWithComplexContent2() throws Exception {
		new XQParser("string(<elem>{'a'} a {1,2,3} b <![CDATA[ b ]]> c {'a', 'b'}</elem>)").parse();
	}	

	@Test
	public void illegallyNestedElements() throws Exception {
		try {
			new XQParser("<a><b></a>").parse();
		} catch (QueryException e) {
			assertEquals("illegal error code", ErrorCode.ERR_PARSING_ERROR, e
					.getCode());
		}
	}

	@Test
	public void nestedForBindNameOverlap() throws Exception {
		new XQParser("for $a in for $a in 1 return $a return $a").parse();
	}

	@Test
	public void nestedForBindRename() throws Exception {
		new XQParser(
				"for $a in 1 return for $a in 2 return for $a in $a return $a")
				.parse();
	}

	@Test
	public void weirdPart2() throws Exception {
		new XQParser(
				"else- +-++-**-* instance  of element(*)* * * **---++div- div -div")
				.parse();
	}

	@Test
	public void weird() throws Exception {
		new XQParser(readQuery("/parser/", "weird.xq")).parse();
	}
	
	@Test
	public void constructedAttributeWithEmptyContentSequence() throws Exception {
		new XQParser("attribute {'foo'} {}").parse();
	}
	
	@Test
	public void constructedPI() throws Exception {
		new XQParser("processing-instruction XmL {'pi'}").parse();
	}	
	
	@Test
	public void onlyWSbetweenToken() throws Exception {
		new XQParser("for\n$\na in 1 return $a").parse();
	}
	
	@Test
	public void parentAxis() throws Exception {
		new XQParser("a/../b").parse();
	}
	
	@Test
	public void stringLiteralWithEntityReference() throws Exception {
		new XQParser("\"a string &amp;\"").parse();
	}	
}
