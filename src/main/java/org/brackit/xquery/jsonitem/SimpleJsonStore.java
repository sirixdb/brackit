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
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.json.JsonCollection;
import org.brackit.xquery.xdm.json.JsonStore;

import java.nio.file.Path;

/**
 * @author Johannes Lichtenberger
 */
public final class SimpleJsonStore implements JsonStore {
  @Override
  public JsonCollection<?> lookup(String name) {
    return null;
  }

  @Override
  public JsonCollection<?> create(String name) {
    return null;
  }

  @Override
  public JsonCollection<?> create(String name, Path path) {
    return null;
  }

  @Override
  public JsonCollection<?> create(String collName, String optResName, Path path) {
    return null;
  }

  @Override
  public JsonCollection<?> createFromPaths(String name, Stream<Path> parsers) {
    return null;
  }

  @Override
  public JsonCollection<?> create(String name, String json) {
    return null;
  }

  @Override
  public JsonCollection<?> create(String collName, String optResName, String json) {
    return null;
  }

  @Override
  public JsonCollection<?> createFromJsonStrings(String name, Stream<Str> json) {
    return null;
  }

  @Override
  public void drop(String name) {

  }

  @Override
  public void makeDir(String path) {

  }
}
