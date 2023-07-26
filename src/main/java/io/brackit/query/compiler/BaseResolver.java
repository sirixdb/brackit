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
package io.brackit.query.compiler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.brackit.query.module.Module;
import io.brackit.query.util.io.URIHandler;

/**
 * @author Sebastian Baechle
 */
public class BaseResolver implements ModuleResolver {

  protected Map<String, List<Module>> modules;

  public void register(String targetNSUri, Module module) {
    List<Module> list;
    if (modules == null) {
      modules = new HashMap<>();
    }
    list = modules.computeIfAbsent(targetNSUri, k -> new ArrayList<>(1));
    list.add(module);
  }

  public void unregister(String targetNSUri) {
    if (modules != null) {
      modules.remove(targetNSUri);
    }
  }

  @Override
  public List<Module> resolve(String uri, String... locUris) {
    var list = modules == null ? null : modules.get(uri);
    return list == null ? Collections.emptyList() : list;
  }

  @Override
  public List<String> load(String uri, String[] locations) throws IOException {
    var loaded = new LinkedList<String>();
    for (String loc : locations) {
      String s = load(loc);
      if (s != null) {
        loaded.add(s);
      }
    }
    return loaded;
  }

  protected String load(String loc) throws IOException {
    String s = null;
    try {
      InputStreamReader in = new InputStreamReader(URIHandler.getInputStream(new URI(loc)));
      CharBuffer buf = CharBuffer.allocate(1024 * 521);
      in.read(buf);
      in.close();
      s = buf.rewind().toString();
    } catch (URISyntaxException e) {
      // location URI's must not be valid -> ignore
    }
    return s;
  }
}