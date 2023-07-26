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
package io.brackit.query.jdm.node;

import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.node.parser.NodeSubtreeParser;
import io.brackit.query.jdm.DocumentException;
import io.brackit.query.jdm.Stream;

/**
 * @author Sebastian Baechle
 */
public interface NodeFactory<E extends Node<E>> {
  E element(QNm name) throws DocumentException;

  E attribute(QNm name, Atomic value) throws DocumentException;

  E text(Atomic value) throws DocumentException;

  E pi(QNm target, Str value) throws DocumentException;

  E comment(Str value) throws DocumentException;

  E document(Str name) throws DocumentException;

  E copy(Node<?> source) throws DocumentException;

  E build(NodeSubtreeParser parser) throws DocumentException;

  NodeCollection<E> collection(String name, NodeSubtreeParser parser) throws DocumentException;

  NodeCollection<E> collection(String name, Stream<NodeSubtreeParser> parsers) throws DocumentException;

  NodeCollection<E> collection(String name) throws DocumentException;
}