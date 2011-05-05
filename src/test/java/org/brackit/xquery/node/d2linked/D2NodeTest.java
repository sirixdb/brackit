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
package org.brackit.xquery.node.d2linked;

import static org.junit.Assert.assertTrue;

import org.brackit.xquery.node.NodeTest;
import org.brackit.xquery.node.SubtreePrinter;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.node.parser.NavigationalSubtreeProcessor;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Stream;
import org.junit.Test;

public class D2NodeTest extends NodeTest<D2Node> {

	@Test
	public void testScanner() throws DocumentException {
		D2Node root = new DocumentD2Node().append(Kind.ELEMENT, "root");
		root.setAttribute("rootAtt1", "rootAtt1Value");
		root.setAttribute("rootAtt2", "rootAtt2Value");
		D2Node child1 = root.append(Kind.ELEMENT, "child1");
		D2Node child2 = root.append(Kind.ELEMENT, "child2");
		D2Node child3 = root.append(Kind.ELEMENT, "child3");
		D2Node child4 = root.append(Kind.ELEMENT, "child4");
		D2Node child5 = root.append(Kind.ELEMENT, "child5");
		D2Node child6 = root.append(Kind.ELEMENT, "child6");
		//		
		// Stream<? extends PalAbstractConstructedNode> children =
		// root.getChildren(transaction);
		//		
		// while (children.hasNext())
		// {
		// System.out.println(children.next());
		// }
		// children.close();
		//		
		D2Node grandChild11 = child1.append(Kind.ELEMENT, "grandChild11");
		D2Node grandChild12 = child1.append(Kind.TEXT, "grandChild12");
		D2Node grandChild13 = child1.append(Kind.ELEMENT, "grandChild13");
		grandChild13.setAttribute("grandChild12Att1", "grandChild12Att1Value");
		grandChild13.setAttribute("grandChild12Att2", "grandChild12Att2Value");

		D2Node grandChild31 = child3.append(Kind.ELEMENT, "grandChild31");
		D2Node ggrandChild311 = grandChild31.append(Kind.ELEMENT,
				"grandChild311");
		boolean followingOf = ggrandChild311.isFollowingOf(child1);
		assertTrue(followingOf);
		D2Node gggrandChild3111 = ggrandChild311.append(Kind.ELEMENT,
				"grandChild3111");
		D2Node gggrandChild31111 = gggrandChild3111.append(Kind.ELEMENT,
				"grandChild31111");

		D2Node grandChild51 = child5.append(Kind.ELEMENT, "grandChild51");
		D2Node ggrandChild511 = grandChild51.append(Kind.ELEMENT,
				"grandChild511");
		D2Node gggrandChild5111 = ggrandChild511.append(Kind.ELEMENT,
				"grandChild5111");

		ggrandChild511.setAttribute("ggrandChild511Att1",
				"ggrandChild511Att1Value");
		ggrandChild511.setAttribute("ggrandChild511Att2",
				"ggrandChild511Att2Value");
		gggrandChild5111.setAttribute("gggrandChild5111Att1",
				"gggrandChild5111Att1Value");
		gggrandChild5111.setAttribute("gggrandChild5111Att2",
				"gggrandChild5111Att2Value");

		NavigationalSubtreeProcessor<? extends Node<?>> processor = new NavigationalSubtreeProcessor(
				root, new SubtreePrinter(System.out));
		processor.process();

		Stream<? extends D2Node> subtree = root.getSubtree();

		D2Node n;
		while ((n = subtree.next()) != null) {
			System.out.println(n);
		}
		subtree.close();
	}

	/*
	 * <a> <b> <c>c1</c> <b> <c>c2</c> </b> <c>c3</c> </b> </a>
	 */

	@Test
	public void testCmp() throws DocumentException {
		Collection<D2Node> coll = createDocument(new DocumentParser(
				"<a><b><c>c1</c><b><c>c2</c></b><c>c3</c></b></a>"));
		D2Node c1 = coll.getDocument().getFirstChild().getFirstChild()
				.getFirstChild().getFirstChild();
		D2Node c3 = coll.getDocument().getFirstChild().getFirstChild()
				.getLastChild().getFirstChild();
		boolean c1PrecOfc3 = c1.isPrecedingOf(c3);
		boolean c3FollowingOfc3 = c3.isFollowingOf(c1);
		int res = c1.cmp(c3);
		int res2 = c3.cmp(c1);
	}

	@Override
	protected Collection<D2Node> createDocument(DocumentParser documentParser)
			throws DocumentException {
		return new D2NodeFactory().build(documentParser).getCollection();
	}
}
