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
package io.brackit.query.compiler.parser;

import io.brackit.query.XQueryBaseTest;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

/**
 * @author Sebastian Baechle
 */
public class XQParserTest extends XQueryBaseTest {

  private static final Path PARSER = RESOURCES.resolve("parser");

  @Test
  public void emptyArray() {
    new JsoniqParser("[]").parse();
  }

  @Test
  public void emptyRecord() {
    new JsoniqParser("{}").parse();
  }

  @Test
  public void ncname() {
    new JsoniqParser("declare").parse();
  }

  @Test
  public void qname() {
    new JsoniqParser("declare").parse();
  }

  @Test
  public void emptyElement() {
    new JsoniqParser("<a/>").parse();
  }

  @Test
  public void nestedElements() {
    new JsoniqParser("<a><b></b></a>").parse();
  }

  @Test
  public void emptyElementWithAttribute() {
    new JsoniqParser("<a b='a'/>").parse();
  }

  @Test
  public void elementWithAttribute() {
    new JsoniqParser("<a b='a'></a>").parse();
  }

  @Test
  public void elementsWithComplexContent() {
    new JsoniqParser("<a>1&amp;<b/>12 {'aha'}soso<c/></a>").parse();
  }

  @Test
  public void elementWithComplexContent2() {
    new JsoniqParser("string(<elem>{'a'} a {1,2,3} b <![CDATA[ b ]]> c {'a', 'b'}</elem>)").parse();
  }

  @Test
  public void illegallyNestedElements() {
    try {
      new JsoniqParser("<a><b></a>").parse();
    } catch (QueryException e) {
      assertEquals("illegal error code", ErrorCode.ERR_PARSING_ERROR, e.getCode());
    }
  }

  @Test
  public void nestedForBindNameOverlap() {
    new JsoniqParser("for $a in for $a in 1 return $a return $a").parse();
  }

  @Test
  public void nestedForBindRename() {
    new JsoniqParser("for $a in 1 return for $a in 2 return for $a in $a return $a").parse();
  }

  @Test
  public void weirdPart2() {
    new JsoniqParser("else- +-++-**-* instance  of element(*)* * * **---++div- div -div").parse();
  }

  @Test
  public void weird() throws Exception {
    new JsoniqParser(Files.readString(PARSER.resolve("weird.xq"))).parse();
  }

  @Test
  public void constructedAttributeWithEmptyContentSequence() {
    new JsoniqParser("attribute {'foo'} {}").parse();
  }

  @Test
  public void constructedPI() {
    new JsoniqParser("processing-instruction XmL {'pi'}").parse();
  }

  @Test
  public void onlyWSbetweenToken() {
    new JsoniqParser("for\n$\na in 1 return $a").parse();
  }

  @Test
  public void parentAxis() {
    new JsoniqParser("a/../b").parse();
  }

  @Test
  public void stringLiteralWithEntityReference() {
    new JsoniqParser("\"a string &amp;\"").parse();
  }
}
