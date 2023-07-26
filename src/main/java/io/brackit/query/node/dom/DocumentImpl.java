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
package io.brackit.query.node.dom;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

/**
 * @author Sebastian Baechle
 */
public class DocumentImpl extends NodeImpl implements org.w3c.dom.Document {
  private Element root;

  public DocumentImpl() {
    super(null, null, Node.DOCUMENT_NODE, null, null);
  }

  @Override
  public org.w3c.dom.Node adoptNode(org.w3c.dom.Node source) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Attr createAttribute(String name) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CDATASection createCDATASection(String data) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Comment createComment(String data) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DocumentFragment createDocumentFragment() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Element createElement(String tagName) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public EntityReference createEntityReference(String name) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ProcessingInstruction createProcessingInstruction(String target, String data) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Text createTextNode(String data) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DocumentType getDoctype() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Element getDocumentElement() {
    return this.root;
  }

  public void setDocumentElement(Element element) {
    this.root = element;
  }

  @Override
  public String getDocumentURI() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DOMConfiguration getDomConfig() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Element getElementById(String elementId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NodeList getElementsByTagName(String tagname) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DOMImplementation getImplementation() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getInputEncoding() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean getStrictErrorChecking() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getXmlEncoding() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean getXmlStandalone() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getXmlVersion() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public org.w3c.dom.Node importNode(org.w3c.dom.Node importedNode, boolean deep) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void normalizeDocument() {
    // TODO Auto-generated method stub

  }

  @Override
  public org.w3c.dom.Node renameNode(org.w3c.dom.Node n, String namespaceURI, String qualifiedName)
      throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setDocumentURI(String documentURI) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setStrictErrorChecking(boolean strictErrorChecking) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setXmlStandalone(boolean xmlStandalone) throws DOMException {
    // TODO Auto-generated method stub

  }

  @Override
  public void setXmlVersion(String xmlVersion) throws DOMException {
    // TODO Auto-generated method stub

  }

}