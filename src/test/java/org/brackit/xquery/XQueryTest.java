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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.Dbl;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.module.MainModule;
import org.brackit.xquery.node.SubtreePrinter;
import org.brackit.xquery.node.linked.ElementLNode;
import org.brackit.xquery.operator.IntegerSource;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class XQueryTest extends XQueryBaseTest {
	@Test
	public void forExpr() throws Exception {
		Sequence result = new XQuery("for $a in (1,2,3) return $a + 1")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(2), new Int32(3),
				new Int32(4)), result);
	}

	@Test
	public void twoForExprs() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) for $b in (4,5,6) return $a + $b")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(5), new Int32(6),
				new Int32(7), new Int32(6), new Int32(7), new Int32(8),
				new Int32(7), new Int32(8), new Int32(9)), result);
	}

	@Test
	public void forExpr2() throws Exception {
		Sequence result = new XQuery("//a/b/concat('a', 'b')/c").execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(2), new Int32(3),
				new Int32(4)), result);
	}

	@Test
	public void forExprWithRunVariable() throws Exception {
		Sequence result = new XQuery("for $a  at $b in (4,5,6) return $b + 1")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(2), new Int32(3),
				new Int32(4)), result);
	}

	@Test
	public void forExprWithWhereClause() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1 to 5) where $a > 3 return $a").execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(4), new Int32(5)),
				result);
	}

	// TODO multiple where clauses are XQuery 1.1
	@Ignore
	@Test
	public void forExprWithTwoWhereClauses() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1 to 5) where $a > 3 where $a < 5 return $a")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new Int32(4), result);
	}

	@Test
	public void forExprWithOrderByClause() throws Exception {
		Sequence result = new XQuery(
				"for $a in (3,2,1) order by $a ascending return $a")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3)), result);
	}

	@Test
	public void forExprOneLetBinding() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) let $b:=4 return $a + $b").execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(5), new Int32(6),
				new Int32(7)), result);
	}

	@Test
	public void forExprTwoLetBindings() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) let $b:=4 let $c:= 1 return $a + $b + $c")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(6), new Int32(7),
				new Int32(8)), result);
	}

	@Test
	public void forExprThreeLetBindingsOneLetUnused() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) let $b:=4 let $c:= 9 let $d:=1 return $a + $b + $d")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(6), new Int32(7),
				new Int32(8)), result);
	}

	@Test
	public void forExprTwoLetBindingsWithWhereClause() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) let $b:=4 let $c:= 1 where $a + $b + $c > 7 return $a + $b + $c")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new Int32(8), result);
	}

	@Test
	public void forExprTwoLetBindingsWithOrderByRunVar() throws Exception {
		Sequence result = new XQuery(
				"for $a in (3,2,1) let $b:=4 let $c:= 1 order by $a ascending return $a + $b + $c")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(6), new Int32(7),
				new Int32(8)), result);
	}

	@Test
	public void forExprTwoLetBindingsWithOrderByLetVarAndRunVar()
			throws Exception {
		Sequence result = new XQuery(
				"for $a in (3,2,1) let $b:=4 let $c:= 1 order by $c, $a ascending return $a + $b + $c")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(6), new Int32(7),
				new Int32(8)), result);
	}

	@Test
	public void letExpr() throws Exception {
		Sequence result = new XQuery("let $a:= (1,2,3) return $a").execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3)), result);
	}

	@Test
	public void letExprVarUnused() throws Exception {
		Sequence result = new XQuery("let $a:= (1,2,3) return (1,2,3)")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3)), result);
	}

	@Test
	public void letExprWithWhereClause() throws Exception {
		Sequence result = new XQuery("let $a:= 2 where $a > 3 return $a")
				.execute(ctx);
		ResultChecker.dCheck(ctx, null, result);
	}

	@Test
	public void nestedFor() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) return for $b in (4,5,6) return $a + $b")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(5), new Int32(6),
				new Int32(7), new Int32(6), new Int32(7), new Int32(8),
				new Int32(7), new Int32(8), new Int32(9)), result);
	}

	@Test
	public void variableShadowingWithNormalize() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) for $b in (for $a in (4,5,6) return $a) return $a + $b")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(5), new Int32(6),
				new Int32(7), new Int32(6), new Int32(7), new Int32(8),
				new Int32(7), new Int32(8), new Int32(9)), result);
	}

	@Test
	public void variableShadowing() throws Exception {
		new XQuery(
				"for $a in (1,2,3) return for $b in (for $a in (4,5,6) return $a) return $a + $b");
	}

	@Test
	public void nestedForWithAnotherForInNestedIn() throws Exception {
		new XQuery(
				"for $a in (1,2,3) return for $b in (for $c in (4,5,6) return $c) return $a + $b");
	}

	@Test
	public void someQuantifiedExprOneBindingTrue() throws Exception {
		Sequence result = new XQuery("some $a in (1,2,3) satisfies $a > 2")
				.execute(ctx);
		ResultChecker.dCheck(ctx, Bool.TRUE, result);
	}

	@Test
	public void someQuantifiedExprOneBindingFalse() throws Exception {
		Sequence result = new XQuery("some $a in (1,2,3) satisfies $a > 3")
				.execute(ctx);
		ResultChecker.dCheck(ctx, Bool.FALSE, result);
	}

	@Test
	public void someQuantifiedExprTwoBindingsTrue() throws Exception {
		Sequence result = new XQuery(
				"some $a in (1,2,3), $b in (3,4,5) satisfies $a = $b")
				.execute(ctx);
		ResultChecker.dCheck(ctx, Bool.TRUE, result);
	}

	@Test
	public void someQuantifiedExprTwoBindingsFalse() throws Exception {
		Sequence result = new XQuery(
				"some $a in (1,2,3), $b in (4,5,6) satisfies $a = $b")
				.execute(ctx);
		ResultChecker.dCheck(ctx, Bool.FALSE, result);
	}

	@Test
	public void everyQuantifiedExprOneBindingTrue() throws Exception {
		Sequence result = new XQuery("every $a in (1,2,3) satisfies $a < 4")
				.execute(ctx);
		ResultChecker.dCheck(ctx, Bool.TRUE, result);
	}

	@Test
	public void everyQuantifiedExprOneBindingFalse() throws Exception {
		Sequence result = new XQuery("every $a in (1,2,3) satisfies $a < 3")
				.execute(ctx);
		ResultChecker.dCheck(ctx, Bool.FALSE, result);
	}

	@Test
	public void everyQuantifiedExprTwoBindingsTrue() throws Exception {
		Sequence result = new XQuery(
				"every $a in (1,2,3), $b in (4,5,6) satisfies $a < $b")
				.execute(ctx);
		ResultChecker.dCheck(ctx, Bool.TRUE, result);
	}

	@Test
	public void everyQuantifiedExprTwoBindingsFalse() throws Exception {
		Sequence result = new XQuery(
				"every $a in (1,2,3), $b in (3,4,5) satisfies $a < $b")
				.execute(ctx);
		ResultChecker.dCheck(ctx, Bool.FALSE, result);
	}

	@Test
	public void variableDeclarationWithAccess() throws Exception {
		Sequence result = new XQuery(
				"declare variable $x := 1; for $a in (1,2,3) return $a + $x")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(2), new Int32(3),
				new Int32(4)), result);
	}

	@Test
	public void twoVariableDeclarationsWithAccess() throws Exception {
		Sequence result = new XQuery(
				"declare variable $x := 1; declare variable $y := $x + 2; for $a in (1,2,3) return $a + $x + $y")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(5), new Int32(6),
				new Int32(7)), result);
	}

	@Test
	public void externalVariableDeclarationWithAccess() throws Exception {
		ctx.bind(new QNm("x"), Int32.ZERO_TWO_TWENTY[2]);
		XQuery query = new XQuery(
				"declare variable $x external := 1; for $a in (1,2,3) return $a + $x");
		MainModule module = query.getMainModule();
		System.out.println(module.getVariables());
		Sequence result = query.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(3), new Int32(4),
				new Int32(5)), result);
	}

	@Test
	public void forExprWithElementConstruction() throws Exception {
		new XQuery("for $a in (1,2,3) return <x>{$a + 1}</x>");
		IntegerSource op = new IntegerSource(1, 2, 3);
	}

	@Test
	public void pathExpressionWithPredicate() throws Exception {
		new XQuery(
				"doc('auction.xml')/site/people/person[profile/interest/@category = 1]");
	}

	@Test
	public void letWithNestedForInExpression() throws Exception {
		new XQuery(
				"let $a := (1, 2, 3) return <aha>{for $b in $a return $b + 1}</aha>");
	}

	@Test
	public void sequenceOfLetExpressions() throws Exception {
		new XQuery(
				"let $a := (1,2,3) let $b := (4,5,6) let $c := (7,8,9) return $a + $b + $c ");
	}

	@Test
	public void sequenceOfLetAndForExpressions() throws Exception {
		new XQuery(
				"let $a := (1,2,3) let $b := (4,5,6) let $c := (7,8,9) for $d in $a for $e in $b for $f in $c let $g := ($d + $f) return $d + $d + $f ");
	}

	@Test
	public void simpleLetExpr() throws Exception {
		Sequence result = new XQuery("let $a := (1,2,3) return 1").execute(ctx);
		ResultChecker.dCheck(ctx, new Int32(1), result);
	}

	@Test
	public void directElementExpr() throws Exception {
		Sequence result = new XQuery("<a/>").execute(ctx);
		ResultChecker.dCheck(ctx, new ElementLNode("a"), result);
	}

	@Test
	public void directElementExprWithEmptyAttribute() throws Exception {
		Sequence result = new XQuery("<a b=''/>").execute(ctx);
		Node<?> element = new ElementLNode("a");
		element.setAttribute("b", "");
		ResultChecker.dCheck(ctx, element, result);
	}

	@Test
	public void directElementExprWithAttribute() throws Exception {
		Sequence result = new XQuery("<a b='c'/>").execute(ctx);
		Node<?> element = new ElementLNode("a");
		element.setAttribute("b", "c");
		ResultChecker.dCheck(ctx, element, result);
	}

	@Test
	public void directElementExprWithAttributeAndComputedValue()
			throws Exception {
		Sequence result = new XQuery("<a b=\"{1 + 2}\"/> ").execute(ctx);
		Node<?> element = new ElementLNode("a");
		element.setAttribute("b", "3");
		ResultChecker.dCheck(ctx, element, result);
	}

	@Test
	public void directElementExprWith2Attributes() throws Exception {
		Sequence result = new XQuery("<a b='c' d='e'/>").execute(ctx);
		Node<?> element = new ElementLNode("a");
		element.setAttribute("b", "c");
		element.setAttribute("d", "e");
		ResultChecker.dCheck(ctx, element, result);
	}

	@Test
	public void directElementExprWith2AttributesAndComputedValue()
			throws Exception {
		Sequence result = new XQuery("<a b='{1 + 2}'   c='{2 + 2}'/>")
				.execute(ctx);
		Node<?> element = new ElementLNode("a");
		element.setAttribute("b", "3");
		element.setAttribute("c", "4");
		ResultChecker.dCheck(ctx, element, result);
	}

	@Test
	public void directElementExprWithText() throws Exception {
		Sequence result = new XQuery("<a>test</a>").execute(ctx);
		Node<?> element = new ElementLNode("a");
		element.append(Kind.TEXT, "test");
		ResultChecker.dCheck(ctx, element, result);
	}

	@Test
	public void directElementExprWithChildren() throws Exception {
		Sequence result = new XQuery("<a><b/><c/></a>").execute(ctx);
		Node<?> element = new ElementLNode("a");
		element.append(Kind.ELEMENT, "b");
		element.append(Kind.ELEMENT, "c");
		SubtreePrinter.print((Node<?>) result, System.out);
		ResultChecker.dCheck(ctx, element, result);
	}

	@Test
	public void directElementExprWithComputedContent() throws Exception {
		Sequence result = new XQuery("<a>{(1 to 3)}</a>").execute(ctx);
		Node<?> element = new ElementLNode("a");
		element.append(Kind.TEXT, "1 2 3");
		SubtreePrinter.print((Node<?>) result, System.out);
		ResultChecker.dCheck(ctx, element, result);
	}

	@Test
	public void directElementExprWithComputedSequenceContent() throws Exception {
		Sequence result = new XQuery("<a>{1, 2, 3}</a>").execute(ctx);
		Node<?> element = new ElementLNode("a");
		element.append(Kind.TEXT, "1 2 3");
		SubtreePrinter.print((Node<?>) result, System.out);
		ResultChecker.dCheck(ctx, element, result);
	}

	@Test
	public void nesteddirectElementExprInSequence() throws Exception {
		Sequence result = new XQuery("<x>{<id>{2 + 4}</id>, 1}</x>")
				.execute(ctx);
		ResultChecker.dCheck(ctx, null, result);
	}

	@Test
	public void generalValueComparison() throws Exception {
		Sequence result = new XQuery("1 > 2").execute(ctx);
		ResultChecker.dCheck(ctx, Bool.FALSE, result);
	}

	@Test
	public void valueComparison() throws Exception {
		Sequence result = new XQuery("1 lt 2").execute(ctx);
		ResultChecker.dCheck(ctx, Bool.TRUE, result);
	}

	@Test
	public void addOperation() throws Exception {
		Sequence result = new XQuery("1 + 2").execute(ctx);
		ResultChecker.dCheck(ctx, new Int32(3), result);
	}

	@Test
	public void composedArithmeticOperationWithMultPrecedence1()
			throws Exception {
		Sequence result = new XQuery("1 + 2 * 3").execute(ctx);
		ResultChecker.dCheck(ctx, new Int32(7), result);
	}

	@Test
	public void composedArithmeticOperationWithMultPrecedence2()
			throws Exception {
		Sequence result = new XQuery("1 * 2 + 3").execute(ctx);
		ResultChecker.dCheck(ctx, new Int32(5), result);
	}

	@Test
	public void composedArithmeticOperationWithParenthesizedPrecedence()
			throws Exception {
		Sequence result = new XQuery("(1 + 2) * 3").execute(ctx);
		ResultChecker.dCheck(ctx, new Int32(9), result);
	}

	@Test
	public void multOperation() throws Exception {
		Sequence result = new XQuery("1 * 2").execute(ctx);
		ResultChecker.dCheck(ctx, new Int32(2), result);
	}

	@Test
	public void generalComparisonNodeAndAtomics() throws Exception {
		// the content must be converted to numeric double
		Sequence result = new XQuery("(<a>12</a> < 24) and (<b>122</b> > 24)")
				.execute(ctx);
		ResultChecker.dCheck(ctx, Bool.TRUE, result);
	}

	@Test
	public void valueComparisonNodeAndAtomics() throws Exception {
		// the content must be converted to numeric double
		Sequence result = new XQuery("(<a>12</a> lt 24) and (<b>122</b> gt 24)")
				.execute(ctx);
		ResultChecker.dCheck(ctx, Bool.TRUE, result);
	}

	@Test
	public void forWithOrderOnMixedData() throws Exception {
		Sequence result = new XQuery(
				"for $a in ('1aha', <a>12</a>, 19, 4) order by $a return $a")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, Bool.TRUE, result);
	}

	@Test
	public void doubleArithmeticWithNegativeZero() throws Exception {
		Sequence result = new XQuery("1 + xs:double('.21E0')").execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, new Dbl(-0), result);
	}

	@Test
	public void rangeExpr() throws Exception {
		Sequence result = new XQuery("(1 to 5)").execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3), new Int32(4), new Int32(5)), result);
	}

	@Test
	public void ifExpr() throws Exception {
		Sequence result = new XQuery("if (0) then 1 else 0").execute(ctx);
		ResultChecker.dCheck(ctx, new Int32(0), result);
	}

	@Test
	public void stringLiteralInQuotes() throws Exception {
		Sequence result = new XQuery("\"test\"").execute(ctx);
		ResultChecker.dCheck(ctx, new Str("test"), result);
	}

	@Test
	public void stringLiteralInApostrophes() throws Exception {
		Sequence result = new XQuery("'test'").execute(ctx);
		ResultChecker.dCheck(ctx, new Str("test"), result);
	}

	@Test
	public void intLiteral() throws Exception {
		Sequence result = new XQuery("23").execute(ctx);
		ResultChecker.dCheck(ctx, new Int32(23), result);
	}

	@Test
	public void stringLiteral() throws Exception {
		Sequence result = new XQuery("' a h a '").execute(ctx);
		ResultChecker.dCheck(ctx, new Str(" a h a "), result);
	}

	@Test
	public void singleStepPathExpr() throws Exception {
		Sequence result = new XQuery("./a//node()").execute(ctx);
		Collection<?> locator = store.lookup("test.xml");
		Node<?> n = locator.getDocument().getFirstChild().getFirstChild();
		ResultChecker.dCheck(ctx, n, result);
	}

	@Test
	public void oneChildStepPathExpr() throws Exception {
		Sequence result = new XQuery("doc('test.xml')/a").execute(ctx);
		Collection<?> locator = store.lookup("test.xml");
		Node<?> documentNode = locator.getDocument();
		Node<?> n = documentNode.getFirstChild().getFirstChild();
		ResultChecker.dCheck(ctx, n, result);
	}

	@Test
	public void twoChildStepsPathExpr() throws Exception {
		Sequence result = new XQuery("doc('test.xml')/a/b").execute(ctx);
		Collection<?> locator = store.lookup("test.xml");
		Node<?> documentNode = locator.getDocument();
		Node<?> n = documentNode.getFirstChild().getFirstChild()
				.getFirstChild();
		ResultChecker.dCheck(ctx, n, result);
	}

	@Test
	public void oneChildOneDescendantStepPathExpr() throws Exception {
		Sequence result = new XQuery("doc('test.xml')/a//b").execute(ctx);
		Collection<?> locator = store.lookup("test.xml");
		Node<?> documentNode = locator.getDocument();
		Node<?> a = documentNode.getFirstChild().getFirstChild();
		Node<?> b = a.getFirstChild().getNextSibling();
		ResultChecker.dCheck(ctx, new ItemSequence(a, b), result);
	}

	@Test
	public void oneChildOneDescendantStepPathToTextExpr() throws Exception {
		Sequence result = new XQuery("doc('test.xml')/a//b/text()")
				.execute(ctx);
		Collection<?> locator = store.lookup("test.xml");
		Node<?> documentNode = locator.getDocument();
		Node<?> a = documentNode.getFirstChild().getFirstChild()
				.getFirstChild();
		Node<?> b = a.getNextSibling().getFirstChild();
		ResultChecker.dCheck(ctx, new ItemSequence(a, b), result);
	}

	@Test
	public void oneChildOneDescendantStepWithWildcardPathExpr()
			throws Exception {
		Sequence result = new XQuery("doc('test.xml')/a//*").execute(ctx);
		Collection<?> locator = store.lookup("test.xml");
		Node<?> documentNode = locator.getDocument();
		Node<?> a = documentNode.getFirstChild().getFirstChild();
		Node<?> b = a.getFirstChild().getNextSibling();
		Node<?> c = a.getNextSibling();
		print(result);
		ResultChecker.dCheck(ctx, new ItemSequence(a, b, c), result);
	}

	@Test
	public void simplePathExprWithTrailingFilterExpr() throws Exception {
		Collection<?> locator = store.lookup("test.xml");
		Sequence result = new XQuery("doc('test.xml')/a/*[last()]")
				.execute(ctx);
		print(result);
		Node<?> node = locator.getDocument().getFirstChild().getLastChild();
		ResultChecker.dCheck(ctx, node, result);
	}

	@Test
	public void simplePathExprWithBoundStartContext() throws Exception {
		Sequence result = new XQuery(
				"for $a in (<a><b>text1<b>text2</b></b></a>) return $a//b")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new Int32("10000000"), result);
	}

	@Test
	public void simplePathExprWithDefaultCtxItemAsStartContext()
			throws Exception {
		Collection<?> locator = store.lookup("test.xml");
		ctx.setDefaultContext(locator.getDocument(), Int32.ONE, Int32.ONE);
		Sequence result = new XQuery("//b").execute(ctx);
		ResultChecker.dCheck(ctx, new Int32("10000000"), result);
	}

	@Test
	public void filterExprWithnestedSimplePathExprWithContextItemAccess()
			throws Exception {
		Sequence result = new XQuery(
				"data((<a><b>frag1</b></a>, <a><b>frag2</b></a>, <a><c>frag3</c></a>)[.//b]//text())")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, new ItemSequence(new Una("frag1"), new Una(
				"frag2")), result);
	}

	@Test
	public void simpleInsert() throws Exception {
		System.out.println("before:");
		new XQuery("fn:doc('test.xml')").serialize(ctx, System.out);
		new XQuery("insert node <test/> into fn:doc('test.xml')/a").serialize(
				ctx, System.out);
		System.out.println("after:");
		new XQuery("fn:doc('test.xml')").serialize(ctx, System.out);
	}

	@Test
	public void simpleDelete() throws Exception {
		new XQuery("delete node <a/>").serialize(ctx, System.out);
	}

	@Test
	public void simpleReplaceNode() throws Exception {
		new XQuery("replace node (<a><b/></a>)//b with <c/>").serialize(ctx,
				System.out);
	}

	@Test
	public void simpleRename() throws Exception {
		new XQuery("rename node <a/> as \"b\"").serialize(ctx, System.out);
	}

	@Test
	public void declareFunction() throws Exception {
		new XQuery(
				"declare function local:addOne($a as item()) { $a + 1 }; local:addOne(1)")
				.serialize(ctx, System.out);
	}

	@Test
	public void declareRecursiveFunction() throws Exception {
		new XQuery(
				"declare function local:countdown($a as xs:integer) { if ($a > 0) then ($a, local:countdown($a - 1)) else $a }; local:countdown(3)")
				.serialize(ctx, System.out);
	}

	@Test
	public void declareFunctionInIllegalNamespace() throws Exception {
		try {
			new XQuery("declare function xs:addOne($a as item()) { $a + 1 }; 1");
			fail("Illegal declaration not detected");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_FUNCTION_DECL_IN_ILLEGAL_NAMESPACE, e
							.getCode());
		}
	}

	@Test
	public void declareFunctionWithoutNamespace() throws Exception {
		try {
			new XQuery("declare function addOne($a as item()) { $a + 1 }; 1");
			fail("Illegal declaration not detected");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_FUNCTION_DECL_NOT_IN_NS, e.getCode());
		}
	}

	@Test
	public void castStringAsDouble() throws Exception {
		Sequence result = new XQuery("'-1.000E4' cast as xs:double")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, new Dbl(-10000), result);
	}

	@Test
	public void doubleConstructorFunction() throws Exception {
		Sequence result = new XQuery("xs:double('-1.000E4')").execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, new Dbl(-10000), result);
	}

	@Test
	public void testBug() throws Exception {
		Sequence result = new XQuery("xs:float(xs:integer(3))").execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, new Dbl(-10000), result);
	}

	@Test
	public void castStringAsUnsignedByte() throws Exception {
		Sequence result = new XQuery("'     255    ' cast as xs:unsignedByte")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new Int32(255).asType(Type.UBYT), result);
	}

	@Test
	public void illegalCastStringAsUnsignedByte() throws Exception {
		try {
			Sequence result = new XQuery("'256' cast as xs:unsignedByte")
					.execute(ctx);
			fail("Illegal cast not detected");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_INVALID_VALUE_FOR_CAST, e.getCode());
		}
	}

	@Test
	public void stringCastableAsDouble() throws Exception {
		Sequence result = new XQuery("'-1.000E4' castable as xs:double")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, Bool.TRUE, result);
	}

	@Test
	public void integerTreatAsDecimal() throws Exception {
		Sequence result = new XQuery("3 treat as xs:decimal").execute(ctx);
		ResultChecker.dCheck(ctx, new Int32(3), result);
	}

	@Test
	public void integerInstanceOfDecimal() throws Exception {
		Sequence result = new XQuery("3 instance of xs:decimal").execute(ctx);
		ResultChecker.dCheck(ctx, Bool.TRUE, result);
	}

	@Test
	public void stringInstanceOfDecimal() throws Exception {
		Sequence result = new XQuery("'Foo' instance of xs:decimal")
				.execute(ctx);
		ResultChecker.dCheck(ctx, Bool.FALSE, result);
	}

	@Test
	public void illegalStringTreatAsDouble() throws Exception {
		try {
			new XQuery("'foo' treat as xs:double").execute(ctx);
			fail("Illegal treat not detected");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE, e
							.getCode());
		}
	}

	@Test
	public void stringNotCastableAsDouble() throws Exception {
		Sequence result = new XQuery("'Foo' castable as xs:double")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, Bool.FALSE, result);
	}

	@Test
	public void castLegalEmptySequenceAsDouble() throws Exception {
		Sequence result = new XQuery("() cast as xs:double?").execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, null, result);
	}

	@Test
	public void castIllegalEmptySequenceAsDouble() throws Exception {
		try {
			new XQuery("() cast as xs:double").execute(ctx);
			fail("Illegal case not detected");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, e.getCode());
		}
	}

	@Test
	public void deepEqualA() throws Exception {
		Sequence result = new XQuery(
				"let $at := <attendees> <name last='Parker'"
						+ "        first='Peter'/> <name last='Barker' first='Bob'/>"
						+ "        <name last='Parker' first='Peter'/> </attendees>"
						+ "return fn:deep-equal($at, $at/*) ").execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, Bool.FALSE, result);
	}

	@Test
	public void deepEqualB() throws Exception {
		Sequence result = new XQuery(
				"let $at := <attendees> <name last='Parker'"
						+ "        first='Peter'/> <name last='Barker' first='Bob'/>"
						+ "        <name last='Parker' first='Peter'/> </attendees>"
						+ "return fn:deep-equal($at/name[1], $at/name[2]) ")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, Bool.FALSE, result);
	}

	@Test
	public void deepEqualC() throws Exception {
		Sequence result = new XQuery(
				"let $at := <attendees> <name last='Parker'"
						+ "        first='Peter'/> <name last='Barker' first='Bob'/>"
						+ "        <name last='Parker' first='Peter'/> </attendees>"
						+ "return fn:deep-equal($at/name[1], $at/name[3]) ")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, Bool.TRUE, result);
	}

	@Test
	public void deepEqualD() throws Exception {
		Sequence result = new XQuery(
				"let $at := <attendees> <name last='Parker'"
						+ "        first='Peter'/> <name last='Barker' first='Bob'/>"
						+ "        <name last='Parker' first='Peter'/> </attendees>"
						+ "return fn:deep-equal($at/name[1], 'Peter Parker') ")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, Bool.FALSE, result);
	}

	@Test
	public void unionA() throws Exception {
		Sequence result = new XQuery(
				"let $at := (document {<a><b>text1<b>text2</b></b><c>text2<b>text3</b></c><b>text4</b></a>})/a "
						+ "return $at//b | $at//c").execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, Bool.FALSE, result);
	}

	@Test
	public void exceptA() throws Exception {
		Sequence result = new XQuery(
				"let $at := (document {<a><b>text1<b>text2</b></b><c>text2<b>text3</b></c><b>text4</b></a>})/a "
						+ "return $at//b except $at/b").execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, Bool.FALSE, result);
	}

	@Test
	public void arithmeticTest1() throws Exception {
		Sequence result = new XQuery("xs:double(1) + 1").execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, new Dbl(2), result);
	}

	@Test
	public void arithmeticTest2() throws Exception {
		ctx.bind(new QNm("ext"), new Una("1"));
		Sequence result = new XQuery(
				"declare variable $ext external; let $ctx := (<a att=\"1\"/>) return xs:integer(fn:data($ctx/@att)) + $ext")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(ctx, new Dbl(2), result);
	}

	@Test
	public void distinctTest() throws Exception {
		Sequence result = new XQuery(
				"fn:distinct-values((1, 2.0, 3, 'a', 'b', 'a', fn:true(), fn:false(), 1 le 2))")
				.execute(ctx);
		print(result);
	}

	@Test
	public void distinctTest2() throws Exception {
		storeDocument("test2.xml", "<doc><a att1='16'/><a att1='12'/></doc>");
		new XQuery("fn:distinct-values(fn:doc('test2.xml')/doc/a/@att1)")
				.serialize(ctx, System.out);
		// new
		// XQuery("fn:doc('tpcc.xml')/tpcc/Warehouses/Warehouse/Districts/District/Customers/Customer[@c_w_id = 1 and @c_d_id = 1 and @c_id = 416]/@c_balance/fn:data(.)").serialize(ctx,
		// System.out);
	}

	@Test
	public void typeswitchTest() throws QueryException {
		Sequence result = new XQuery(
				"let $d := xs:dayTimeDuration('PT2H') return typeswitch($d)"
						+ "case $a as xs:yearMonthDuration return fn:concat('YMD:', $a)"
						+ "case $v as xs:dayTimeDuration return fn:concat('DTD:', $v)"
						+ "default $d return fn:concat('DUR:', $d)")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new Str("DTD:PT2H"), result);
	}

	@Test
	public void typeswitchTestDefault() throws QueryException {
		Sequence result = new XQuery(
				"let $d := xs:duration('PT2H') return typeswitch($d)"
						+ "case $a as xs:yearMonthDuration return fn:concat('YMD:', $a)"
						+ "case $v as xs:dayTimeDuration return fn:concat('DTD:', $v)"
						+ "default $d return fn:concat('DUR:', $d)")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new Str("DUR:PT2H"), result);
	}

	@Test
	public void transformTestSimple() throws QueryException {
		new XQuery(
				"copy $n := <a att='1'><b/></a> modify delete node $n/@att return $n")
				.serialize(ctx, System.out);
	}

	@Test
	public void transformTestDeleteBoundCopy() throws QueryException {
		// Will not result in empty sequence as nodes without parents are
		// ignored by delete operation
		new XQuery(
				"copy $n := <a att='1'><b/></a> modify delete node $n return $n")
				.serialize(ctx, System.out);
	}

	@Test
	public void transformTest() throws QueryException {
		Sequence result = new XQuery(
				"let $d := <a><b><c/></b><d/></a>"
						+ "return copy $n := $d, $v := $d/a modify delete node $n/b return $n")
				.execute(ctx);
		ResultChecker.dCheck(ctx, null, result);
	}

	@Test
	public void transformTestImmediateVarRef() throws QueryException {

		try {
			new XQuery(
					"let $d := <a><b><c/></b><d/></a>"
							+ "return copy $n := $d, $a := $a modify delete node $n/b return $n/b")
					.execute(ctx);
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_UNDEFINED_REFERENCE, e.getCode());
		}
	}

	@Test
	public void insertTest2() throws Exception {
		storeDocument("test3.xml",
				"<doc><a att1='16.0'>16.0</a><a att1='12.0'>12.0</a></doc>");
		new XQuery(
				"for $i in fn:doc('test3.xml')/doc/a return <res>{element {'a'}{fn:data($i)}}</res>")
				.serialize(ctx, System.out);
	}

	@Test
	public void insertTest3() throws Exception {
		storeDocument("test4.xml",
				"<doc><a att1='16.0'>16</a><b att1='12.0'>12</b></doc>");
		ctx.bind(new QNm("intVar"), new Una("4.0"));
		new XQuery(
				"declare variable $intVar external; "
						+ "replace value of node fn:doc('test4.xml')/doc/a with $intVar;")
				.execute(ctx);
		new XQuery("fn:doc('test4.xml')").serialize(ctx, System.out);
	}

	@Test
	public void nullTest() throws Exception {
		ctx.bind(new QNm("intVar"), new Una("4.0"));
		new XQuery(
				"for $i in 1 to 3 return <res>{ attribute {'att'} {} }</res>;")
				.serialize(ctx, System.out);
	}

	@Test
	public void uriTest() {
		try {
			URI uri = new java.net.URI("test.xml");
			System.out.println(uri.toASCIIString());
		} catch (URISyntaxException e) {
			//
		}
	}

	@Test
	public void regexTest1() {
		try {
			Pattern p = Pattern.compile(".*ab.*");
			Matcher m = p.matcher("xabc");
			System.out.println(m.matches());
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	@Test
	public void regexTest2() {
		try {
			Pattern p = Pattern.compile("ab(x*)");
			Matcher m = p.matcher("ababx");
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				m.appendReplacement(sb, "$ncd");
			}
			m.appendTail(sb);

			System.out.println(sb.toString());
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	@Test
	public void replaceTest() throws QueryException {
		new XQuery("replace node fn:doc('test.xml')/a/c with <d/>")
				.execute(ctx);
	}

	@Before
	public void setUp() throws Exception, FileNotFoundException {
		super.setUp();
		storeDocument("test.xml", "<a><b>text1<b>text2</b></b><c>text2</c></a>");
	}
}
