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
package io.brackit.query.expr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.PrintStream;

import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.atomic.Una;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.jdm.node.NodeCollection;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.Query;
import io.brackit.query.sequence.ItemSequence;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 */
public class PathExprTest extends XQueryBaseTest {
  @Test
  public void oneChildStepPathExpr() {
    NodeCollection<?> locator = storeDocument("test.xml", "<a><b/><b/></a>");
    Node<?> documentNode = locator.getDocument();
    Node<?> root = documentNode.getFirstChild();
    ctx.setContextItem(root);
    Sequence result = new Query("b").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(root.getFirstChild(), root.getLastChild()), result);
  }

  @Test
  public void singleSlashStepPathExpr() {
    NodeCollection<?> locator = storeDocument("test.xml", "<a><b/><b/></a>");
    Node<?> documentNode = locator.getDocument();
    Node<?> aNode = documentNode.getFirstChild().getFirstChild();
    ctx.setContextItem(aNode);
    Sequence result = new Query("/").execute(ctx);
    ResultChecker.dCheck(documentNode, result);
  }

  @Test
  public void oneDoubleSlashStepPathExpr() {
    NodeCollection<?> locator = storeDocument("test.xml", "<a><b/></a>");
    Node<?> documentNode = locator.getDocument();
    ctx.setContextItem(documentNode);
    Sequence result = new Query("$$//b").execute(ctx);
    ResultChecker.dCheck(documentNode.getFirstChild().getFirstChild(), result);
  }

  // @Test
  // public void checkSingleDesc() throws Exception {
  // Sequence result = new XQuery("(<a><b/><b/><b/></a>)//b").execute(ctx);
  // ResultChecker.dCheck(new Int32(1), result);
  // }

  @Test
  public void positionAsStep() {
    Sequence result = new Query("<a><b1/><b2/><b3/></a>/last()").execute(ctx);
    ResultChecker.dCheck(new Int32(1), result);
  }

  @Test
  public void positionAndLastInPredicate() {
    Sequence result = new Query("<a><b1/><b2/></a>/*[position() = last()]/name()").execute(ctx);
    ResultChecker.dCheck(new Str("b2"), result);
  }

  @Test
  public void pathExprTest() {
    PrintStream buf = createBuffer();
    new Query("(<a><b><c/><c/></b></a>)//node-name($$)").serialize(ctx, buf);
    assertEquals("a b c c", buf.toString());
  }

  @Test
  public void pathExprTest2() {
    PrintStream buf = createBuffer();
    new Query("(<a><b><c/><c/></b></a>)//position()").serialize(ctx, buf);
    assertEquals("1 2 3 4", buf.toString());
  }

  @Test
  public void pathExprTest3() {
    PrintStream buf = createBuffer();
    new Query("(<a><b><c/><c/></b></a>)//last()").serialize(ctx, buf);
    assertEquals("4 4 4 4", buf.toString());
  }

  @Test
  public void pathExprTest4() {
    PrintStream buf = createBuffer();
    new Query("(<a><b><c/><c/></b></a>)//position()[last()]").serialize(ctx, buf);
    assertEquals("1 2 3 4", buf.toString());
  }

  @Test
  public void pathExprTest5() {
    PrintStream buf = createBuffer();
    new Query("(<a><b><c/><c>aha</c></b></a>)/b/*/position()").serialize(ctx, buf);
    assertEquals("1 2", buf.toString());
  }

  @Test
  public void pathExprTest6() {
    PrintStream buf = createBuffer();
    new Query("(<a><b><c>c1</c><b><c>c2</c></b><c>c3</c></b></a>)//b/c/text()").serialize(ctx, buf);
    assertEquals("c1c2c3", buf.toString());
  }

  @Test
  public void pathExprTest9() {
    PrintStream buf = createBuffer();
    new Query("let $doc := document{<a><c><b>b1</b><b>b2</b></c><d><b>b3</b></d></a>}/* return (($doc/d, $doc/c))//position()").serialize(ctx,
                                                                                                                                          buf);
    assertEquals("1 2 3 4 5 6 7 8", buf.toString());
  }

  @Test
  public void pathExprTest9b() {
    PrintStream buf = createBuffer();
    new Query("let $doc := (<a><c><b>b1</b><b>b2</b></c><d><b>b3</b></d></a>) return (($doc/d, $doc/c))//position()[2]").serialize(ctx,
                                                                                                                                   buf);
    assertEquals("", buf.toString());
  }

  @Test
  public void pathExprTest9c() {
    PrintStream buf = createBuffer();
    new Query("let $doc := (<a><c><b>b1</b><b>b2</b></c><d><b>b3</b></d></a>) return (($doc/d, $doc/c))//*[last() - 1]").serialize(ctx,
                                                                                                                                   buf);
    assertEquals("<b>b1</b>", buf.toString());
  }

  @Test
  public void pathExprTest9d() {
    PrintStream buf = createBuffer();
    new Query("let $doc := (<a><c><b>b1</b><b>b2</b></c><d><b>b3</b></d></a>) return (($doc/d, $doc/c))/../*[last() - 1]").serialize(ctx,
                                                                                                                                     buf);
    assertEquals("<c><b>b1</b><b>b2</b></c>", buf.toString());
  }

  @Test
  public void pathExprTest9e() {
    PrintStream buf = createBuffer();
    new Query("let $doc := (<a><c><b>b1</b><b>b2</b></c><d><b>b3</b></d></a>) return (($doc/d, $doc/c))/..//*[last() - 1]").serialize(ctx,
                                                                                                                                      buf);
    assertEquals("<c><b>b1</b><b>b2</b></c><b>b1</b>", buf.toString());
  }

  @Test
  public void pathExprTest7() {
    PrintStream buf = createBuffer();
    new Query("(<a><b><c>c1</c><b><c>c2</c></b><c>c3</c></b><b><c>c4</c></b></a>)//c[2]").serialize(ctx, buf);
    assertEquals("<c>c3</c>", buf.toString());
  }

  @Test
  public void pathExprTest10() {
    PrintStream buf = createBuffer();
    new Query("let $doc := document{<a><c><b>b1</b><b>b2</b></c><d><b>b3</b></d></a>}/* return (($doc/d, $doc/c, $doc/d))/*").serialize(ctx,
                                                                                                                                        buf);
    assertEquals("<b>b1</b><b>b2</b><b>b3</b>", buf.toString());
  }

  @Test
  public void pathExprTest11() {
    PrintStream buf = createBuffer();
    new Query("let $doc := document{<a><c><b>b1</b><b>b2</b></c><d><b>b3</b></d></a>}/* return (($doc/d, $doc/c, $doc/d))//position()").serialize(ctx,
                                                                                                                                                  buf);
    assertEquals("1 2 3 4 5 6 7 8", buf.toString());
  }

  @Test
  public void pathExprTest12() {
    PrintStream buf = createBuffer();
    new Query("let $doc := document{<a><c><b>b1</b><b>b2</b></c><d><b>b3</b></d></a>}/* return (($doc/d, $doc/c, $doc/d))/b/text()").serialize(ctx,
                                                                                                                                               buf);
    assertEquals("b1b2b3", buf.toString());
  }

  @Test
  public void pathExprTestDebug() {
    PrintStream buf = createBuffer();
    new Query("(<a><b><c>c1</c><b><c>c2</c></b><c>c3</c></b><b><c>c4</c></b></a>)/descendant-or-self::node()/c[2]").serialize(ctx,
                                                                                                                              buf);
    assertEquals("<c>c3</c>", buf.toString());
  }

  @Test
  public void pathExprTest8() {
    PrintStream buf = createBuffer();
    new Query("let $doc := (<a><c><b>b1</b><b>b2</b></c><d><b>b3</b></d></a>) return (($doc/d, $doc/c))//b").serialize(ctx,
                                                                                                                       buf);
    assertEquals("<b>b1</b><b>b2</b><b>b3</b>", buf.toString());
  }

  @Test
  public void elementTest1() {
    Sequence res = new Query("(<a><b/><c/></a>)/element(b)").execute(ctx);
    Node<?> exp = ctx.getNodeFactory().element(new QNm("b"));
    ResultChecker.check(exp, res, false);
  }

  @Test
  public void elementTest2() {
    Sequence res = new Query("(<a><b/><c/></a>)/element(b, xs:untyped)").execute(ctx);
    Node<?> exp = ctx.getNodeFactory().element(new QNm("b"));
    ResultChecker.check(exp, res, false);
  }

  @Test
  public void elementTest3() {
    Sequence res = new Query("(<a><b/><c/></a>)/element(b, xs:double)").execute(ctx);
    ResultChecker.check(null, res, false);
  }

  @Test
  public void elementTest4() {
    Sequence res = new Query("(<a><b/><c/></a>)/element(*, xs:untyped)").execute(ctx);
    Sequence exp = new ItemSequence(ctx.getNodeFactory().element(new QNm("b")),
                                    ctx.getNodeFactory().element(new QNm("c")));
    ResultChecker.check(exp, res, false);
  }

  @Test
  public void attributeTest1() {
    Sequence res = new Query("(<a b='' c=''/>)/attribute(b)").execute(ctx);
    Node<?> exp = ctx.getNodeFactory().attribute(new QNm("b"), new Una(""));
    ResultChecker.check(exp, res, false);
  }

  @Test
  public void attributeTest2() {
    Sequence res = new Query("(<a b='' c=''/>)/attribute(b, xs:untypedAtomic)").execute(ctx);
    Node<?> exp = ctx.getNodeFactory().attribute(new QNm("b"), new Una(""));
    ResultChecker.check(exp, res, false);
  }

  @Test
  public void attributeTest3() {
    Sequence res = new Query("(<a b='' c=''/>)/attribute(b, xs:double)").execute(ctx);
    ResultChecker.check(null, res, false);
  }

  @Test
  public void attributeTest4() {
    Sequence res = new Query("(<a b='' c=''/>)/attribute(*, xs:untypedAtomic)").execute(ctx);
    Sequence exp = new ItemSequence(ctx.getNodeFactory().attribute(new QNm("b"), new Una("")),
                                    ctx.getNodeFactory().attribute(new QNm("c"), new Una("")));
    ResultChecker.check(exp, res, false);
  }

  @Test
  public void schemaElementTest() {
    try {
      new Query("(<a><b/></a>)/schema-element(b)");
      fail("missing imported schema not detected");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_UNDEFINED_REFERENCE, e.getCode());
    }
  }

  @Test
  public void schemaAttributeTest() {
    try {
      new Query("(<a b=''/>)/schema-attribute(b)");
      fail("missing imported schema not detected");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_UNDEFINED_REFERENCE, e.getCode());
    }
  }

  @Test
  public void mixedOutputTest() {
    try {
      Sequence s = new Query("(<a>1</a>,<b>2</b>)/(if(position() eq 1) then $$ else data($$))").execute(ctx);
      Iter it = s.iterate();
      try {
        while (it.next() != null)
          ;
      } finally {
        it.close();
      }
      fail("mixed output sequence not detected");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_PATH_STEP_RETURNED_NODE_AND_NON_NODE_VALUES, e.getCode());
    }
  }

  @Test
  public void mixedOutputTest2() {
    try {
      Sequence s = new Query("declare variable $myVar := <e>text</e>; $myVar/text()/(<e/>, (), 1, <e/>)").execute(ctx);
      Iter it = s.iterate();
      try {
        while (it.next() != null)
          ;
      } finally {
        it.close();
      }
      fail("mixed output sequence not detected");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_PATH_STEP_RETURNED_NODE_AND_NON_NODE_VALUES, e.getCode());
    }
  }
}
