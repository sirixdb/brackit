/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.compiler;

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

import org.brackit.xquery.module.Module;
import org.brackit.xquery.util.URIHandler;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class BaseResolver implements ModuleResolver {

	protected Map<String, List<Module>> modules;

	public void register(String targetNSUri, Module module) {
		List<Module> list = null;
		if (modules == null) {
			modules = new HashMap<String, List<Module>>();
		}
		list = modules.get(targetNSUri);
		if (list == null) {
			list = new ArrayList<Module>(1);
			modules.put(targetNSUri, list);
		}
		list.add(module);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Module> resolve(String uri, String... locUris) {
		List<Module> list = (modules != null) ? modules.get(uri) : null;
		return (list == null) ? Collections.EMPTY_LIST : list;
	}

	@Override
	public List<String> load(String uri, String[] locations) throws IOException {
		List<String> loaded = new LinkedList<String>();
		for (String loc : locations) {
			try {
				InputStreamReader in = new InputStreamReader(URIHandler
						.getInputStream(new URI(loc)));
				CharBuffer buf = CharBuffer.allocate(1024 * 521);
				int read = in.read(buf);
				in.close();
				loaded.add(buf.rewind().toString());
			} catch (URISyntaxException e) {
				// location URI's must not be valid -> ignore
			}
		}
		return loaded;
	}
}