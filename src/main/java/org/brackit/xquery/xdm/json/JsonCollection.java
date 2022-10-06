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
package org.brackit.xquery.xdm.json;

import java.nio.file.Path;

import org.brackit.xquery.function.json.JSONParser;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.OperationNotSupportedException;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.StructuredItem;
import org.brackit.xquery.xdm.StructuredItemCollection;

/**
 * @param <E>
 * @author Sebastian Baechle
 */
public interface JsonCollection<E extends StructuredItem> extends StructuredItemCollection<E> {

  @Override
  String getName();

  @Override
  void delete() throws DocumentException;

  @Override
  void remove(long documentID);

  @Override
  E getDocument();

  @Override
  Stream<? extends E> getDocuments();

  /**
   * Add a file to the JSON collection.
   *
   * @param file the file to add to the collection
   * @return the JSON root
   * @throws OperationNotSupportedException if the operation is not supported
   * @throws DocumentException              if anything else went wrong.
   */
  E add(Path file);

  /**
   * Add a JSON string to the JSON collection.
   *
   * @param json the JSON string to add to the collection
   * @return the JSON root
   * @throws OperationNotSupportedException if the operation is not supported
   * @throws DocumentException              if anything else went wrong.
   */
  E add(String json);

  @Override
  long getDocumentCount();
}
