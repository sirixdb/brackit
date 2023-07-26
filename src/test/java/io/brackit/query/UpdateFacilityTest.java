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

import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.atomic.Una;
import io.brackit.query.jdm.DocumentException;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.node.parser.DocumentParser;
import io.brackit.query.sequence.ItemSequence;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 */
public class UpdateFacilityTest extends XQueryBaseTest {

  private static final String DOCUMENT = "<a><b>text1<b>text2</b></b><c>text2</c></a>";

  Node<?> doc;
  Node<?> orig;

  @Test
  public void insertInto() {
    ctx.setContextItem(doc);
    orig.getFirstChild().append(Kind.ELEMENT, new QNm("test"), null);
    new Query("insert node <test/> into $$/a").execute(ctx);
    ResultChecker.dCheck(orig, doc, false);
  }

  @Test
  public void simpleDelete() {
    ctx.setContextItem(doc);
    new Query("delete node $$/a/c").execute(ctx);
    orig.getFirstChild().getLastChild().delete();
    ResultChecker.dCheck(orig, doc, false);
  }

  @Test
  public void simpleReplaceNode() {
    ctx.setContextItem(doc);
    new Query("replace node $$/a/c with <d/>").execute(ctx);
    orig.getFirstChild().getLastChild().replaceWith(Kind.ELEMENT, new QNm("d"), null);
    ResultChecker.dCheck(orig, doc, false);
  }

  @Test
  public void simpleRename() {
    ctx.setContextItem(doc);
    new Query("rename node $$/a as 'b'").execute(ctx);
    orig.getFirstChild().setName(new QNm("b"));
    ResultChecker.dCheck(orig, doc, false);
  }

  @Test
  public void transformTestSimple() {
    Sequence res = new Query("copy $n := <a att='1'><b/></a> modify delete node $n/@att return $n").execute(ctx);
    Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
    a.append(Kind.ELEMENT, new QNm("b"), null);
    ResultChecker.dCheck(a, res, false);
  }

  @Test
  public void transformTestDeleteBoundCopy() {
    // Will not result in empty sequence as nodes without parents are
    // ignored by delete operation
    Sequence res = new Query("copy $n := <a att='1'><b/></a> modify delete node $n return $n").execute(ctx);
    Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
    a.setAttribute(new QNm("att"), new Una("1"));
    a.append(Kind.ELEMENT, new QNm("b"), null);
    ResultChecker.dCheck(a, res, false);
  }

  @Test(expected = DocumentException.class)
  public void transformIllegalExpression() {
    // Two attributes with the same name (insert applied before delete).
    new Query("copy $c := <x a='a'/> modify (delete node $c/@a, insert node attribute a { 'b' } into $c) return $c").execute(ctx);
  }

  @Test(expected = QueryException.class)
  public void transformIllegalExpressionSecond() {
    // Two attributes with the same name (insert applied before delete).
    new Query("copy $c := <x a='a'/> modify (delete node $c/@a, insert node attribute a { 'b' } into $c, replace node $c/@a with attribute a { 'b' }) return $c").execute(ctx);
  }

  @Test
  public void transformModifyFirst() {
    Sequence res = new Query(
                              "copy $c := <n><a/><a/></n> modify for $a in $c//a return replace node $a with <b/> return $c").execute(ctx);
    Node<?> a = ctx.getNodeFactory().element(new QNm("n"));
    a.append(Kind.ELEMENT, new QNm("b"), null);
    a.append(Kind.ELEMENT, new QNm("b"), null);
    ResultChecker.dCheck(a, res, false);
  }

  @Test
  public void transformModifySecond() {
    final String xq = "copy $foo := <foo/>" + " modify " + "  for $x in ('b', 'a') " + "  order by $x "
        + "  return insert node text { $x } into $foo return $foo";
    Sequence res = new Query(xq).execute(ctx);
    Node<?> a = ctx.getNodeFactory().element(new QNm("foo"));
    a.append(Kind.TEXT, null, new Str("ab"));
    ResultChecker.dCheck(a, res, false);
  }

  @Test
  public void transformTestTwoCopyVars() {
    Sequence res = new Query(
                              "let $f := <a att='1'><b/></a> return copy $m := $f, $n := $f, $o := $f modify delete node $n/b return ($m, $n)").execute(ctx);
    Node<?> a1 = ctx.getNodeFactory().element(new QNm("a"));
    a1.setAttribute(new QNm("att"), new Una("1"));
    a1.append(Kind.ELEMENT, new QNm("b"), null);
    Node<?> a2 = ctx.getNodeFactory().element(new QNm("a"));
    a2.setAttribute(new QNm("att"), new Una("1"));
    ResultChecker.dCheck(new ItemSequence(a1, a2), res, false);
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    doc = ctx.getNodeFactory().build(new DocumentParser(DOCUMENT));
    orig = ctx.getNodeFactory().build(new DocumentParser(DOCUMENT));
  }

}