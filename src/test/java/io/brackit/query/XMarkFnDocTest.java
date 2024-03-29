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
package io.brackit.query;

import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

/**
 * Test of slightly modified XMark queries against stored documents.
 *
 * @author Sebastian Baechle
 */
public class XMarkFnDocTest extends XQueryBaseTest {
  /**
   * Query directory.
   */
  private static final Path QUERY_DIR = RESOURCES.resolve("xmark").resolve("queries").resolve("fndoc");

  /**
   * Result directory.
   */
  private static final Path RESULT_DIR = RESOURCES.resolve("xmark").resolve("results");

  @Test
  public void xmark01() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q01.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q01.out"), buffer.toString());
  }

  @Test
  public void xmark02() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q02.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q02.out"), buffer.toString());
  }

  @Test
  public void xmark03() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q03.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q03.out"), buffer.toString());
  }

  @Test
  public void xmark04() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q04.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q04.out"), buffer.toString());
  }

  @Test
  public void xmark05() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q05.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q05.out"), buffer.toString());
  }

  @Test
  public void xmark06() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q06.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q06.out"), buffer.toString());
  }

  @Test
  public void xmark07() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q07.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q07.out"), buffer.toString());
  }

  @Test
  public void xmark08() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q08.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q08.out"), buffer.toString());
  }

  @Test
  public void xmark09() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q09.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q09.out"), buffer.toString());
  }

  @Test
  public void xmark10() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q10.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q10.out"), buffer.toString());
  }

  @Test
  public void xmark11() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q11.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q11.out"), buffer.toString());
  }

  @Test
  public void xmark12() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q12.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q12.out"), buffer.toString());
  }

  @Test
  public void xmark13() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q13.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q13.out"), buffer.toString());
  }

  @Test
  public void xmark14() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q14.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q14.out"), buffer.toString());
  }

  @Test
  public void xmark15() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q15.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q15.out"), buffer.toString());
  }

  @Test
  public void xmark16() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q16.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q16.out"), buffer.toString());
  }

  @Test
  public void xmark17() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q17.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q17.out"), buffer.toString());
  }

  @Test
  public void xmark18() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q18.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q18.out"), buffer.toString());
  }

  @Test
  public void xmark19() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q19.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q19.out"), buffer.toString());
  }

  @Test
  public void xmark20() throws Exception {
    PrintStream buffer = createBuffer();
    Query query = xquery(readFile(QUERY_DIR, "q20.xq"));
    query.serialize(ctx, buffer);
    assertEquals(readFile(RESULT_DIR, "q20.out"), buffer.toString());
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    storeFile("auction.xml", RESOURCES.resolve("xmark").resolve("auction.xml"));
  }
}
