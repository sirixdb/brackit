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
package io.brackit.query.node;

import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Una;
import io.brackit.query.jdm.DocumentException;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.Stream;
import io.brackit.query.jdm.node.NodeCollection;
import io.brackit.query.node.parser.DocumentParser;
import io.brackit.query.ResultChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastian Baechle
 */
public abstract class NodeTest<E extends io.brackit.query.jdm.node.Node<E>> extends XQueryBaseTest {

  private static final Path DOCS = RESOURCES.resolve("docs");

  @Test
  public void testStoreDocument() throws Exception {
    createDocument(new DocumentParser(readFile(DOCS, "orga.xml")));
  }

  @Test
  public void testGetFirstChildForDocumentNode() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser("<a><b/><c/></a>"));
    assertEquals(coll.getDocument().getFirstChild(),
                 coll.getDocument().getFirstChild(),
                 "First child is document root node");
  }

  @Test
  public void testGetLastChildForDocumentNode() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser("<a><b/><c/></a>"));
    assertEquals(coll.getDocument().getFirstChild(),
                 coll.getDocument().getLastChild(),
                 "Last child is document root node");
  }

  @Test
  public void testGetChildrenForDocumentNode() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser("<a><b/><c/></a>"));

    Stream<? extends E> children = coll.getDocument().getChildren();
    E n;
    assertNotNull(n = children.next(), "Document node has a child node");
    assertEquals(coll.getDocument().getFirstChild(), n, "First child is document root node");
    assertNull(n = children.next(), "Document node no further children");
    children.close();
  }

  @Test
  public void testGetSubtreeForDocumentNode() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser("<a><b/><c/></a>"));

    Stream<? extends E> subtree = coll.getDocument().getSubtree();

    E n;
    assertNotNull(n = subtree.next(), "Stream not empty");
    assertEquals(coll.getDocument(), n, "First node is document node");
    assertNotNull(n = subtree.next(), "Stream not empty");
    assertEquals(coll.getDocument().getFirstChild(), n, "Second node is document root node");
    assertNotNull(n = subtree.next(), "Stream not empty");
    assertEquals(coll.getDocument().getFirstChild().getFirstChild(),
                 n,
                 "Third node is document root node's first child");
    assertNotNull(n = subtree.next(), "Stream not empty");
    assertEquals(coll.getDocument().getFirstChild().getLastChild(),
                 n,
                 "Fourth node is document root node's last child");
    subtree.close();
  }

  @Test
  public void testGetSubtreeForRootNode() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser("<a><b/><c/></a>"));

    Stream<? extends E> subtree = coll.getDocument().getFirstChild().getSubtree();

    E n;
    assertNotNull(n = subtree.next(), "Stream not empty");
    assertEquals(coll.getDocument().getFirstChild(), n, "First node is document root node");
    assertNotNull(n = subtree.next(), "Stream not empty");
    assertEquals(coll.getDocument().getFirstChild().getFirstChild(),
                 n,
                 "Second node is document root node's first child");
    assertNotNull(n = subtree.next(), "Stream not empty");
    assertEquals(coll.getDocument().getFirstChild().getLastChild(), n, "Third node is document root node's last child");
    subtree.close();
  }

  @Test
  public void testGetSubtreeForNonRootNode() throws Exception {
    NodeCollection<E> coll = createDocument(new DocumentParser("<a><b><d/><e/></b><c/></a>"));

    Stream<? extends E> subtree = coll.getDocument().getFirstChild().getFirstChild().getSubtree();

    E n;
    assertNotNull(n = subtree.next(), "Stream not empty");
    assertEquals(coll.getDocument().getFirstChild().getFirstChild(), n, "First node is document root node");
    assertNotNull(n = subtree.next(), "Stream not empty");
    assertEquals(coll.getDocument().getFirstChild().getFirstChild().getFirstChild(),
                 n,
                 "Second node is document root node's first child first child");
    assertNotNull(n = subtree.next(), "Stream not empty");
    assertEquals(coll.getDocument().getFirstChild().getFirstChild().getLastChild(),
                 n,
                 "Third node is document root node's first child last child");
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
      Assertions.assertEquals(Kind.ELEMENT, node.getKind(), node + " is of type element");

      // System.out.println("Checking name of element " +
      // node.getDeweyID() + " level " + node.getDeweyID().getLevel() +
      // " is " + element.getNodeName());

      Assertions.assertEquals(element.getNodeName(), node.getName().toString(), String.format("Name of node %s", node));
      compareAttributes(node, element);

      NodeList domChildNodes = element.getChildNodes();
      List<E> children = new ArrayList<>();

      for (E c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
        // System.out.println(String.format("-> Found child of %s : %s",
        // node, c));

        int ancestorLevel = 0;
        for (E ancestor = node; ancestor != null; ancestor = ancestor.getParent()) {
          if (ancestorLevel == 0) {
            assertTrue(c.isChildOf(ancestor), String.format("node %s is child of %s", c, ancestor));
            assertTrue(ancestor.isParentOf(c), String.format("node %s is parent of %s", ancestor, c));
          }
          assertTrue(c.isDescendantOf(ancestor), String.format("node %s is descendant of %s", c, ancestor));
          assertTrue(ancestor.isAncestorOf(c), String.format("node %s is ancestor of %s", ancestor, c));
          ancestorLevel++;
        }

        for (E sibling : children) {
          assertTrue(c.isSiblingOf(sibling), String.format("node %s is sibling of %s", c, sibling));
          assertTrue(sibling.isSiblingOf(c), String.format("node %s is sibling of %s", sibling, c));
          assertTrue(sibling.isPrecedingSiblingOf(c), String.format("node %s is preceding sibling of %s", sibling, c));
          assertTrue(c.isFollowingSiblingOf(sibling), String.format("node %s is following sibling of %s", c, sibling));
          assertTrue(sibling.isPrecedingOf(c), String.format("node %s is preceding of %s", sibling, c));
          assertTrue(c.isFollowingOf(sibling), String.format("node %s is following of %s", c, sibling));

          try {
            assertFalse(c.isPrecedingSiblingOf(sibling),
                        String.format("node %s is not preceding sibling of %s", c, sibling));
          } catch (AssertionError e) {
            c.isPrecedingSiblingOf(sibling);
            throw e;
          }
          assertFalse(sibling.isFollowingSiblingOf(c), String.format("node %s is following sibling of %s", sibling, c));

          assertFalse(c.isPrecedingOf(sibling), String.format("node %s is not preceding of %s", c, sibling));
          assertFalse(sibling.isFollowingOf(c), String.format("node %s is following of %s", sibling, c));
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

        assertNotNull(child, String.format("child node %s of node %s", i, node));

        checkSubtreePreOrder(child, domChild);
      }

      assertEquals(domChildNodes.getLength(), children.size(), String.format("child count of element %s", node));

    } else if (domNode instanceof Text) {
      Text text = (Text) domNode;

      Assertions.assertEquals(Kind.TEXT, node.getKind(), node + " is of type text : \"" + text.getNodeValue() + "\"");
      Assertions.assertEquals(text.getNodeValue().trim(),
                              node.getValue().stringValue(),
                              String.format("Text of node %s", node));
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
      Assertions.assertEquals(Kind.ELEMENT, node.getKind(), node + " is of type element");

      // //System.out.println("Checking name of element " + node +
      // " level " + node.getLevel() + " is " + element.getNodeName());

      Assertions.assertEquals(element.getNodeName(),
                              node.getName().stringValue(),
                              String.format("Name of node %s", node));
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

        assertNotNull(child, String.format("child node %s of node %s", i, node));

        checkSubtreePostOrder(child, domChild);
      }

      assertEquals(domChildNodes.getLength(), children.size(), String.format("child count of element %s", node));

    } else if (domNode instanceof Text) {
      Text text = (Text) domNode;

      Assertions.assertEquals(Kind.TEXT, node.getKind(), node + " is of type text");
      Assertions.assertEquals(text.getNodeValue().trim(),
                              node.getValue().stringValue(),
                              String.format("Text of node %s", node));
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
            assertTrue(c.isAttributeOf(ancestor), String.format("node %s is attribute of %s", c, ancestor));
          } catch (AssertionError e) {
            c.isAttributeOf(ancestor);
            throw e;
          }
          assertTrue(ancestor.isParentOf(c), String.format("node %s is parent of %s", ancestor, c));
        }
        assertTrue(ancestor.isAncestorOf(c), String.format("node %s is ancestor of %s", ancestor, c));
        ancestorLevel++;
      }
    }
    attributes.close();

    assertEquals(domAttributes.getLength(), attributesSize, String.format("attribute count of element %s", node));

    // check if all stored attributes really exist
    for (int i = 0; i < domAttributes.getLength(); i++) {
      Attr domAttribute = (Attr) domAttributes.item(i);
      E attribute = node.getAttribute(new QNm(domAttribute.getName()));
      assertNotNull(attribute, String.format("Attribute \"%s\" of node %s", domAttribute.getName(), node));
      Assertions.assertEquals(Kind.ATTRIBUTE, attribute.getKind(), attribute + " is of type attribute");
      Assertions.assertEquals(domAttribute.getValue(),
                              attribute.getValue().stringValue(),
                              String.format("Value of attribute \"%s\" (%s) of node %s",
                                            domAttribute.getName(),
                                            attribute,
                                            node));
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
    Assertions.assertEquals(new Una("CHECKME"),
                            node.getAttribute(new QNm("new")).getValue(),
                            "updated attribute value");
  }

  @AfterEach
  public void tearDown() throws Exception {
  }

  protected abstract NodeCollection<E> createDocument(DocumentParser documentParser) throws Exception;
}