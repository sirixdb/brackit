/*
 * [New BSD License] Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org> All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: * Redistributions of source code must retain the
 * above copyright notice, this list of conditions and the following disclaimer. * Redistributions
 * in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * * Neither the name of the Brackit Project Team nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery;

import org.brackit.xquery.node.d2linked.D2Node;
import org.brackit.xquery.node.d2linked.D2NodeBuilder;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.node.NodeCollection;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

/**
 * Test XMark queries against default context item.
 *
 * @author Sebastian Baechle
 */
public abstract class XMarkTest extends XQueryBaseTest {

  /**
   * XMark directory.
   */
  protected Path xmarkAuction = RESOURCES.resolve("xmark").resolve("auction.xml");

  /**
   * Query directory.
   */
  protected Path queryDir = RESOURCES.resolve("xmark").resolve("queries").resolve("orig");

  /**
   * Result directory.
   */
  protected Path resultDir = RESOURCES.resolve("xmark").resolve("results");

  /**
   * Collection build from XMark auction document.
   */
  protected NodeCollection<?> coll;

  @Test
  public void xmark01() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q01.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q01.out"), buffer.toString());
  }

  @Test
  public void xmark02() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q02.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q02.out"), buffer.toString());
  }

  @Test
  public void xmark03() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q03.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q03.out"), buffer.toString());
  }

  @Test
  public void xmark04() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q04.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q04.out"), buffer.toString());
  }

  @Test
  public void xmark05() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q05.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q05.out"), buffer.toString());
  }

  @Test
  public void xmark06() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q06.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q06.out"), buffer.toString());
  }

  @Test
  public void xmark07() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q07.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q07.out"), buffer.toString());
  }

  @Test
  public void xmark08() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q08.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q08.out"), buffer.toString());
  }

  @Test
  public void xmark09() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q09.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q09.out"), buffer.toString());
  }

  @Test
  public void xmark10() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q10.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q10.out"), buffer.toString());
  }

  @Test
  public void xmark11() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q11.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q11.out"), buffer.toString());
  }

  @Test
  public void xmark12() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q12.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q12.out"), buffer.toString());
  }

  @Test
  public void xmark13() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q13.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q13.out"), buffer.toString());
  }

  @Test
  public void xmark14() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q14.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q14.out"), buffer.toString());
  }

  @Test
  public void xmark15() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q15.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q15.out"), buffer.toString());
  }

  @Test
  public void xmark16() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q16.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q16.out"), buffer.toString());
  }

  @Test
  public void xmark17() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q17.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q17.out"), buffer.toString());
  }

  @Test
  public void xmark18() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q18.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q18.out"), buffer.toString());
  }

  @Test
  public void xmark19() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q19.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q19.out"), buffer.toString());
  }

  @Test
  public void xmark20() throws Exception {
    ctx.setContextItem(coll.getDocument());
    final PrintStream buffer = createBuffer();
    final XQuery query = xquery(readFile(queryDir, "q20.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(resultDir, "q20.out"), buffer.toString());
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    final DocumentParser parser = new DocumentParser(xmarkAuction.toFile());
    parser.setRetainWhitespace(true);
    coll = createDoc(parser);
  }

  /**
   * Create a collection with a single document.
   *
   * @param parser parses the XMark auction file
   * @return build collection
   * @throws DocumentException if anything went wrong
   */
  protected NodeCollection<?> createDoc(final DocumentParser parser) throws DocumentException {
    final D2NodeBuilder builder = new D2NodeBuilder();
    parser.parse(builder);
    final D2Node subtreeRoot = builder.root();
    return subtreeRoot.getCollection();
  }
}