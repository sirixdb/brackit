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
package org.brackit.query.node;

import org.brackit.query.ResultChecker;
import org.brackit.query.XQueryBaseTest;
import org.brackit.query.atomic.QNm;
import org.brackit.query.atomic.Una;
import org.brackit.query.node.parser.DocumentParser;
import org.brackit.query.jdm.DocumentException;
import org.brackit.query.jdm.Kind;
import org.brackit.query.jdm.Stream;
import org.brackit.query.jdm.node.Node;
import org.brackit.query.jdm.node.NodeCollection;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Sebastian Baechle
 */
public abstract class NodeTest<E extends Node<E>> extends XQueryBaseTest {

  private static final Path DOCS = RESOURCES.resolve("docs");

  @Test
  public void testStoreDocument() throws Exception {
    createDocument(new DocumentParser(readFile(DOCS, "orga.xml")));
  }

  @Test
  public void testGetFirstChildForDocumentNode() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser("<a><b/><c/></a>"));
    assertEquals("First child is document root node",
                 coll.getDocument().getFirstChild(),
                 coll.getDocument().getFirstChild());
  }

  @Test
  public void testGetLastChildForDocumentNode() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser("<a><b/><c/></a>"));
    assertEquals("Last child is document root node",
                 coll.getDocument().getFirstChild(),
                 coll.getDocument().getLastChild());
  }

  @Test
  public void testGetChildrenForDocumentNode() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser("<a><b/><c/></a>"));

    Stream<? extends E> children = coll.getDocument().getChildren();
    E n;
    assertNotNull("Document node has a child node", n = children.next());
    assertEquals("First child is document root node", coll.getDocument().getFirstChild(), n);
    assertNull("Document node no further children", n = children.next());
    children.close();
  }

  @Test
  public void testGetSubtreeForDocumentNode() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser("<a><b/><c/></a>"));

    Stream<? extends E> subtree = coll.getDocument().getSubtree();

    E n;
    assertNotNull("Stream not empty", n = subtree.next());
    assertEquals("First node is document node", coll.getDocument(), n);
    assertNotNull("Stream not empty", n = subtree.next());
    assertEquals("Second node is document root node", coll.getDocument().getFirstChild(), n);
    assertNotNull("Stream not empty", n = subtree.next());
    assertEquals("Third node is document root node's first child",
                 coll.getDocument().getFirstChild().getFirstChild(),
                 n);
    assertNotNull("Stream not empty", n = subtree.next());
    assertEquals("Fourth node is document root node's last child",
                 coll.getDocument().getFirstChild().getLastChild(),
                 n);
    subtree.close();
  }

  @Test
  public void testGetSubtreeForRootNode() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser("<a><b/><c/></a>"));

    Stream<? extends E> subtree = coll.getDocument().getFirstChild().getSubtree();

    E n;
    assertNotNull("Stream not empty", n = subtree.next());
    assertEquals("First node is document root node", coll.getDocument().getFirstChild(), n);
    assertNotNull("Stream not empty", n = subtree.next());
    assertEquals("Second node is document root node's first child",
                 coll.getDocument().getFirstChild().getFirstChild(),
                 n);
    assertNotNull("Stream not empty", n = subtree.next());
    assertEquals("Third node is document root node's last child", coll.getDocument().getFirstChild().getLastChild(), n);
    subtree.close();
  }

  @Test
  public void testGetSubtreeForNonRootNode() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser("<a><b><d/><e/></b><c/></a>"));

    Stream<? extends E> subtree = coll.getDocument().getFirstChild().getFirstChild().getSubtree();

    E n;
    assertNotNull("Stream not empty", n = subtree.next());
    assertEquals("First node is document root node", coll.getDocument().getFirstChild().getFirstChild(), n);
    assertNotNull("Stream not empty", n = subtree.next());
    assertEquals("Second node is document root node's first child first child",
                 coll.getDocument().getFirstChild().getFirstChild().getFirstChild(),
                 n);
    assertNotNull("Stream not empty", n = subtree.next());
    assertEquals("Third node is document root node's first child last child",
                 coll.getDocument().getFirstChild().getFirstChild().getLastChild(),
                 n);
    subtree.close();
  }

  @Test
  public void traverseDocumentInPreorder() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser(readFile(DOCS, "orga.xml")));
    E root = coll.getDocument().getFirstChild();
    org.w3c.dom.Node domRoot;

    domRoot = createDomTree(new InputSource(new StringReader(readFile(DOCS, "orga.xml"))));

    checkSubtreePreOrder(root, domRoot); // check document index
  }

  protected org.w3c.dom.Node createDomTree(InputSource source) throws Exception {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(source);
      return document.getDocumentElement();
    } catch (Exception e) {
      throw new DocumentException("An error occured while creating DOM input source: %s", e.getMessage());
    }
  }

  protected void checkSubtreePreOrder(final E node, org.w3c.dom.Node domNode) throws Exception {
    E child = null;

    if (domNode instanceof Element) {
      Element element = (Element) domNode;
      assertEquals(node + " is of type element", Kind.ELEMENT, node.getKind());

      // System.out.println("Checking name of element " +
      // node.getDeweyID() + " level " + node.getDeweyID().getLevel() +
      // " is " + element.getNodeName());

      assertEquals(String.format("Name of node %s", node), element.getNodeName(), node.getName().toString());
      compareAttributes(node, element);

      NodeList domChildNodes = element.getChildNodes();
      List<E> children = new ArrayList<>();

      for (E c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
        // System.out.println(String.format("-> Found child of %s : %s",
        // node, c));

        int ancestorLevel = 0;
        for (E ancestor = node; ancestor != null; ancestor = ancestor.getParent()) {
          if (ancestorLevel == 0) {
            assertTrue(String.format("node %s is child of %s", c, ancestor), c.isChildOf(ancestor));
            assertTrue(String.format("node %s is parent of %s", ancestor, c), ancestor.isParentOf(c));
          }
          assertTrue(String.format("node %s is descendant of %s", c, ancestor), c.isDescendantOf(ancestor));
          assertTrue(String.format("node %s is ancestor of %s", ancestor, c), ancestor.isAncestorOf(c));
          ancestorLevel++;
        }

        for (E sibling : children) {
          assertTrue(String.format("node %s is sibling of %s", c, sibling), c.isSiblingOf(sibling));
          assertTrue(String.format("node %s is sibling of %s", sibling, c), sibling.isSiblingOf(c));
          assertTrue(String.format("node %s is preceding sibling of %s", sibling, c), sibling.isPrecedingSiblingOf(c));
          assertTrue(String.format("node %s is following sibling of %s", c, sibling), c.isFollowingSiblingOf(sibling));
          assertTrue(String.format("node %s is preceding of %s", sibling, c), sibling.isPrecedingOf(c));
          assertTrue(String.format("node %s is following of %s", c, sibling), c.isFollowingOf(sibling));

          try {
            assertFalse(String.format("node %s is not preceding sibling of %s", c, sibling),
                        c.isPrecedingSiblingOf(sibling));
          } catch (AssertionError e) {
            c.isPrecedingSiblingOf(sibling);
            throw e;
          }
          assertFalse(String.format("node %s is following sibling of %s", sibling, c), sibling.isFollowingSiblingOf(c));

          assertFalse(String.format("node %s is not preceding of %s", c, sibling), c.isPrecedingOf(sibling));
          assertFalse(String.format("node %s is following of %s", sibling, c), sibling.isFollowingOf(c));
        }

        children.add(c);
      }

      for (int i = 0; i < domChildNodes.getLength(); i++) {
        org.w3c.dom.Node domChild = domChildNodes.item(i);
        // System.out.println("Checking if child  " + ((domChild
        // instanceof Element) ? domChild.getNodeName() :
        // domChild.getNodeValue()) + " exists under " + node);

        if (child == null) {
          child = node.getFirstChild();
          // System.out.println(String.format("First child of %s is %s",
          // node, child));
        } else {
          E oldChild = child;
          child = child.getNextSibling();
          // System.out.println(String.format("Next sibling of %s is %s",
          // oldChild, child));
        }

        assertNotNull(String.format("child node %s of node %s", i, node), child);

        checkSubtreePreOrder(child, domChild);
      }

      assertEquals(String.format("child count of element %s", node), domChildNodes.getLength(), children.size());

    } else if (domNode instanceof Text) {
      Text text = (Text) domNode;

      assertEquals(node + " is of type text : \"" + text.getNodeValue() + "\"", Kind.TEXT, node.getKind());
      assertEquals(String.format("Text of node %s", node), text.getNodeValue().trim(), node.getValue().stringValue());
    } else {
      throw new DocumentException("Unexpected dom node: %s", domNode.getClass());
    }
  }

  @Test
  public void traverseDocumentInPostorder() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser(readFile(DOCS, "orga.xml")));
    E root = coll.getDocument().getFirstChild();
    org.w3c.dom.Node domRoot;

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(new InputSource(new StringReader(readFile(DOCS, "orga.xml"))));
      domRoot = document.getDocumentElement();
    } catch (Exception e) {
      throw new DocumentException("An error occured while creating DOM input source: %s", e.getMessage());
    }

    checkSubtreePostOrder(root, domRoot); // check document index
  }

  protected void checkSubtreePostOrder(E node, org.w3c.dom.Node domNode) throws Exception {
    E child = null;

    if (domNode instanceof Element) {
      Element element = (Element) domNode;
      assertEquals(node + " is of type element", Kind.ELEMENT, node.getKind());

      // //System.out.println("Checking name of element " + node +
      // " level " + node.getLevel() + " is " + element.getNodeName());

      assertEquals(String.format("Name of node %s", node), element.getNodeName(), node.getName().stringValue());
      compareAttributes(node, element);

      NodeList domChildNodes = element.getChildNodes();
      List<E> children = new ArrayList<>();

      for (E c = node.getLastChild(); c != null; c = c.getPreviousSibling()) {
        // //System.out.println(String.format("Parent of %s is %s.", c,
        // c.getParent(transaction)));
        children.add(c);
      }

      for (int i = domChildNodes.getLength() - 1; i >= 0; i--) {
        org.w3c.dom.Node domChild = domChildNodes.item(i);
        // //System.out.println("Checking if child  " + ((domChild
        // instanceof Element) ? domChild.getNodeName() :
        // domChild.getNodeValue()) + " exists under " + node);

        if (child == null) {
          child = node.getLastChild();
          // System.out.println(String.format("Last child of %s is %s",
          // node, child));

        } else {
          E oldChild = child;
          child = child.getPreviousSibling();
          // System.out.println(String.format("Prev sibling of %s is %s",
          // oldChild, child));
        }

        assertNotNull(String.format("child node %s of node %s", i, node), child);

        checkSubtreePostOrder(child, domChild);
      }

      assertEquals(String.format("child count of element %s", node), domChildNodes.getLength(), children.size());

    } else if (domNode instanceof Text) {
      Text text = (Text) domNode;

      assertEquals(node + " is of type text", Kind.TEXT, node.getKind());
      assertEquals(String.format("Text of node %s", node), text.getNodeValue().trim(), node.getValue().stringValue());
    } else {
      throw new DocumentException("Unexpected dom node: %s", domNode.getClass());
    }
  }

  protected void compareAttributes(E node, Element element) throws Exception {
    NamedNodeMap domAttributes = element.getAttributes();
    Stream<? extends E> attributes = node.getAttributes();

    int attributesSize = 0;
    E c;
    while ((c = attributes.next()) != null) {
      attributesSize++;

      int ancestorLevel = 0;
      for (E ancestor = node; ancestor != null; ancestor = ancestor.getParent()) {
        if (ancestorLevel == 0) {
          try {
            assertTrue(String.format("node %s is attribute of %s", c, ancestor), c.isAttributeOf(ancestor));
          } catch (AssertionError e) {
            c.isAttributeOf(ancestor);
            throw e;
          }
          assertTrue(String.format("node %s is parent of %s", ancestor, c), ancestor.isParentOf(c));
        }
        assertTrue(String.format("node %s is ancestor of %s", ancestor, c), ancestor.isAncestorOf(c));
        ancestorLevel++;
      }
    }
    attributes.close();

    assertEquals(String.format("attribute count of element %s", node), domAttributes.getLength(), attributesSize);

    // check if all stored attributes really exist
    for (int i = 0; i < domAttributes.getLength(); i++) {
      Attr domAttribute = (Attr) domAttributes.item(i);
      E attribute = node.getAttribute(new QNm(domAttribute.getName()));
      assertNotNull(String.format("Attribute \"%s\" of node %s", domAttribute.getName(), node), attribute);
      assertEquals(attribute + " is of type attribute", Kind.ATTRIBUTE, attribute.getKind());
      assertEquals(String.format("Value of attribute \"%s\" (%s) of node %s", domAttribute.getName(), attribute, node),
                   domAttribute.getValue(),
                   attribute.getValue().stringValue());
    }
  }

  @Test
  public void testAppendSubtree() throws Exception {
    NodeCollection<E> orig = createDocument(new DocumentParser(readFile(DOCS, "orga.xml")));
    NodeCollection<E> doc = createDocument(new DocumentParser(readFile(DOCS, "orga.xml")));

    E onode = orig.getDocument().getFirstChild().getLastChild();
    E test = onode.append(Kind.ELEMENT, new QNm("test"), null);
    test.append(Kind.ELEMENT, new QNm("a"), null);
    test.append(Kind.ELEMENT, new QNm("b"), null);

    E cnode = doc.getDocument().getFirstChild().getLastChild();
    DocumentParser docParser = new DocumentParser("<test><a/><b/></test>");
    docParser.setParseAsFragment(true);
    cnode.append(docParser);
    ResultChecker.check(orig.getDocument(), doc.getDocument(), false);
  }

  @Test
  public void testReplaceSubtree() throws Exception {
    NodeCollection<E> orig = createDocument(new DocumentParser(readFile(DOCS, "orga.xml")));
    NodeCollection<E> doc = createDocument(new DocumentParser(readFile(DOCS, "orga.xml")));

    E onode = orig.getDocument().getFirstChild().getLastChild();
    E test = onode.replaceWith(Kind.ELEMENT, new QNm("test"), null);
    test.append(Kind.ELEMENT, new QNm("a"), null);
    test.append(Kind.ELEMENT, new QNm("b"), null);

    E cnode = doc.getDocument().getFirstChild().getLastChild();
    DocumentParser docParser = new DocumentParser("<test><a/><b/></test>");
    docParser.setParseAsFragment(true);
    cnode.replaceWith(docParser);

    ResultChecker.check(orig.getDocument(), doc.getDocument(), false);
  }

  @Test
  public void testSetAttribute() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser(readFile(DOCS, "orga.xml")));

    E root = coll.getDocument().getFirstChild();
    E node = root.getFirstChild();
    node = root.getFirstChild();
    node = root.getFirstChild();
    node = node.getNextSibling();
    node = node.getNextSibling();
    node.setAttribute(new QNm("new"), new Una("CHECKME"));
    assertEquals("updated attribute value", new Una("CHECKME"), node.getAttribute(new QNm("new")).getValue());
  }

  @After
  public void tearDown() throws Exception {
  }

  protected abstract NodeCollection<E> createDocument(DocumentParser documentParser) throws Exception;
}