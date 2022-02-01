/*
 * [New BSD License]
 * Copyright (c) 2011-2022, Brackit Project Team <info@brackit.org>
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


package org.brackit.xquery.jsonitem;

import org.brackit.xquery.function.json.JSONParser;
import org.brackit.xquery.node.stream.ArrayStream;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.OperationNotSupportedException;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.json.JsonItem;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * @author Johannes Lichtenberger
 */
// TODO: Implement methods
public final class SimpleJsonCollection extends AbstractJsonItemCollection<JsonItem> {
  protected JsonItem[] docs;
  public SimpleJsonCollection(String name, JsonItem doc) {
    super(name);
    this.docs = new JsonItem[] { doc };
  }

  public SimpleJsonCollection(String name, JsonItem... docs) {
    super(name);
    this.docs = docs;
  }

  @Override
  public void delete() throws DocumentException {

  }

  @Override
  public void remove(long documentID) {

  }

  @Override
  public JsonItem getDocument() {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Stream<? extends JsonItem> getDocuments() {
    return new ArrayStream(docs);
  }

  @Override
  public JsonItem add(Path file) {
    throw new OperationNotSupportedException();
  }

  @Override
  public JsonItem add(String json) {
    final var doc = (JsonItem) new JSONParser(json).parse();
    this.docs = Arrays.copyOf(docs, docs.length + 1);
    this.docs[docs.length - 1] = doc;
    return doc;
  }

  public JsonItem add(JsonItem json) {
    this.docs = Arrays.copyOf(docs, docs.length + 1);
    this.docs[docs.length - 1] = json;
    return json;
  }

  @Override
  public long getDocumentCount() {
    return docs.length;
  }
}
