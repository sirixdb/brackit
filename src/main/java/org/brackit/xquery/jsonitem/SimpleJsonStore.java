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

import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.json.JSONParser;
import org.brackit.xquery.util.io.URIHandler;
import org.brackit.xquery.jdm.DocumentException;
import org.brackit.xquery.jdm.OperationNotSupportedException;
import org.brackit.xquery.jdm.Stream;
import org.brackit.xquery.jdm.json.JsonCollection;
import org.brackit.xquery.jdm.json.JsonItem;
import org.brackit.xquery.jdm.json.JsonStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Johannes Lichtenberger
 */
public final class SimpleJsonStore implements JsonStore {
  private final Map<String, JsonCollection<?>> docs = new HashMap<>();
  @Override
  public JsonCollection<?> lookup(String name) {
    JsonCollection<?> coll = docs.get(name);
    if (coll != null) {
      return coll;
    }
    try {
      InputStream in = URIHandler.getInputStream(URI.create(name));
      coll = create(name, new String(in.readAllBytes(), StandardCharsets.UTF_8));
      return coll;
    } catch (IOException e) {
      throw new DocumentException(e, "Collection %s not found", name);
    }
  }

  @Override
  public JsonCollection<?> create(String name) {
    return create(name, name);
  }

  @Override
  public JsonCollection<?> create(String name, Path path) {
    return create(name, path.toUri().toString());
  }

  @Override
  public JsonCollection<?> createFromPaths(String name, Stream<Path> paths) {
    ArrayList<JsonItem> jsonDocs = new ArrayList<>();
    try (paths) {
      Path path;
      while ((path = paths.next()) != null) {
        try {
          InputStream in = URIHandler.getInputStream(path.toUri());
          jsonDocs.add((JsonItem) new JSONParser(new String(in.readAllBytes(), StandardCharsets.UTF_8)).parse());
        } catch (IOException e) {
          throw new DocumentException(e, "Collection %s not found", name);
        }
      }
    }
    var coll = new SimpleJsonCollection(name, jsonDocs.toArray(new JsonItem[0]));
    docs.put(name, coll);
    return coll;
  }

  @Override
  public JsonCollection<?> create(String name, String json) {
    JsonItem doc = (JsonItem) new JSONParser(json).parse();
    var coll = new SimpleJsonCollection(name, doc);
    docs.put(name, coll);
    return coll;
  }

  @Override
  public JsonCollection<?> createFromJsonStrings(String name, Stream<Str> jsons) {
    ArrayList<JsonItem> jsonDocs = new ArrayList<>();
    try (jsons) {
      Str json;
      while ((json = jsons.next()) != null) {
        jsonDocs.add((JsonItem) new JSONParser(json.stringValue()).parse());
      }
    }
    var coll = new SimpleJsonCollection(name, jsonDocs.toArray(new JsonItem[0]));
    docs.put(name, coll);
    return coll;
  }

  @Override
  public void drop(String name) {
    docs.remove(name);
  }

  @Override
  public void makeDir(String path) {
    throw new OperationNotSupportedException();
  }
}
