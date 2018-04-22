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
package org.brackit.xquery.function.json;

import java.util.ArrayList;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.array.DArray;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.Dbl;
import org.brackit.xquery.atomic.Dec;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.Numeric;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.compiler.parser.Tokenizer;
import org.brackit.xquery.record.ArrayRecord;
import org.brackit.xquery.util.serialize.StringSerializer;
import org.brackit.xquery.xdm.Item;

/**
 * <p>
 * Simple recursive descent parser for JSON documents.
 * </p>
 * <p>
 * Note this parser is not 100% conform to the JSON RFC because we simply reuse parts from the
 * general (XQuery) {@link Tokenizer}. When we have time, we should make it JSON conform. ;-)
 * </p>
 *
 * @author Sebastian Baechle
 *
 */
public class JSONParser extends Tokenizer {

  public JSONParser(final String query) {
    super(query);
  }

  public Item parse() throws QueryException {
    try {
      Item i = object();
      i = (i != null)
          ? i
          : array();
      if (i == null) {
        throw new QueryException(JSONFun.ERR_PARSING_ERROR, "No JSON data found");
      }
      consumeEOF();
      return i;
    } catch (final IllegalCharRefException e) {
      throw new QueryException(e, JSONFun.ERR_PARSING_ERROR, e.getMessage());
    } catch (final Exception e) {
      throw new QueryException(e, JSONFun.ERR_PARSING_ERROR, e.getMessage());
    }
  }

  protected void consumeSkipS(final String token) throws TokenizerException {
    final Token la = laSkipS(token);
    if (la == null) {
      throw new TokenizerException("Expected '%s': '%s'", token, paraphrase());
    }
    consume(la);
  }

  private Item value(final boolean required) throws TokenizerException, QueryException {
    Item i = string();
    i = (i != null)
        ? i
        : number();
    i = (i != null)
        ? i
        : object();
    i = (i != null)
        ? i
        : array();
    i = (i != null)
        ? i
        : symbol(required);
    return i;
  }

  private Item symbol(final boolean required) throws TokenizerException {
    if (attemptSkipS("true")) {
      return Bool.TRUE;
    } else if (attemptSkipS("false")) {
      return Bool.FALSE;
    } else if (attemptSkipS("null")) {
      return null;
    } else if (required) {
      throw new TokenizerException("JSON value expected: %s", paraphrase());
    }
    return null;
  }

  private Item array() throws TokenizerException, QueryException {
    if (!attemptSkipS("[")) {
      return null;
    }
    if (attemptSkipS("]")) {
      return new ArrayRecord(new QNm[0], new Item[0]);
    }
    final ArrayList<Item> values = new ArrayList<Item>();
    do {
      values.add(value(true));
    } while (attemptSkipS(","));
    consumeSkipS("]");

    return new DArray(values.toArray(new Item[values.size()]));
  }

  private Item object() throws TokenizerException, QueryException {
    if (!attemptSkipS("{")) {
      return null;
    }
    if (attemptSkipS("}")) {
      return new ArrayRecord(new QNm[0], new Item[0]);
    }
    int len = 0;
    final ArrayList<QNm> fields = new ArrayList<QNm>();
    final ArrayList<Item> values = new ArrayList<Item>();
    do {
      final Str name = string();
      if (name == null) {

      }
      consumeSkipS(":");
      final Item value = value(true);
      fields.add(new QNm(null, null, name.stringValue()));
      values.add(value);
      len++;
    } while (attemptSkipS(","));
    consumeSkipS("}");

    return new ArrayRecord(fields.toArray(new QNm[len]), values.toArray(new Item[len]));
  }

  private Numeric number() throws QueryException, TokenizerException {
    Token la = laS();
    if (la != null) {
      Token la2 = laInteger(la, true);
      if (la2 != null) {
        consume(la);
        consume(la2);
        return Int32.parse(la2.string());
      } else if ((la2 = laDecimal(la, true)) != null) {
        consume(la);
        consume(la2);
        return new Dec(la2.string());
      } else if ((la2 = laDouble(la, true)) != null) {
        consume(la);
        consume(la2);
        return new Dbl(la2.string());
      }
    } else {
      la = laInteger(true);
      if (la != null) {
        consume(la);
        return Int32.parse(la.string());
      } else if ((la = laDecimal(true)) != null) {
        consume(la);
        return new Dec(la.string());
      } else if ((la = laDouble(true)) != null) {
        consume(la);
        return new Dbl(la.string());
      }
    }
    return null;
  }

  private Str string() throws TokenizerException {
    Token la = laS();
    if (la != null) {
      final Token la2 = laString(la, true);
      if (la2 != null) {
        consume(la);
        consume(la2);
        return new Str(la2.string());
      }
    } else {
      la = laString(true);
      if (la != null) {
        consume(la);
        return new Str(la.string());
      }
    }
    return null;
  }

  public static void main(final String[] args) throws Exception {
    final String s =
        "{\"bindings\": [        {\"ircEvent\": \"PRIVMSG\", \"method\": \"newURI\", \"regex\": \"^http://.*\"},        {\"ircEvent\": \"PRIVMSG\", \"method\": \"deleteURI\", \"regex\": \"^delete.*\"},        {\"ircEvent\": \"PRIVMSG\", \"method\": \"randomURI\", \"regex\": \"^random.*\"}    ]}";
    final Item item = new JSONParser(s).parse();
    new StringSerializer(System.out).setFormat(true).serialize(item);
  }
}
