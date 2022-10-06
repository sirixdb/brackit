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
package org.brackit.xquery.jsonitem;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.json.JSONParser;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.node.parser.StreamSubtreeParser;
import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.util.io.URIHandler;
import org.brackit.xquery.util.serialize.StringSerializer;
import org.brackit.xquery.xdm.*;
import org.brackit.xquery.xdm.json.JsonItem;
import org.brackit.xquery.xdm.node.Node;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * A Stream of SubtreeParsers that delivers one SubtreeParser for each item in
 * the sequence.
 *
 * @author Martin Hiller
 */
public final class ParserStream implements Stream<String> {
  private final Iter it;

  public ParserStream(Sequence locs) {
    it = locs.iterate();
  }

  @Override
  public String next() throws DocumentException {
    try {
      Item item = it.next();
      if (item == null) {
        return null;
      }
      if (item instanceof Atomic) {
        String str = ((Atomic) item).stringValue();
        return new String(URIHandler.getInputStream(str).readAllBytes(), StandardCharsets.UTF_8);
      } else if (item instanceof JsonItem jsonItem) {
        final var writer = new PrintWriter(new StringWriter());
        new StringSerializer(writer).serialize(jsonItem);
        return writer.toString();
      } else {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                 "Cannot create string for item of type: %s",
                                 item.itemType());
      }
    } catch (IOException | QueryException e) {
      throw new DocumentException(e);
    }
  }

  @Override
  public void close() {
    it.close();
  }
}