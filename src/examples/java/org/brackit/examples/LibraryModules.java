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
package org.brackit.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.compiler.BaseResolver;
import org.brackit.xquery.compiler.CompileChain;
import org.brackit.xquery.compiler.ModuleResolver;

/**
 * Importing and loading of library modules. 
 */
public class LibraryModules {
	
	private static final String LIBRARY_URI = "http://brackit.org/lib/foo";

	private static final String LIBRARY_MODULE =
			"module namespace foo=\"http://brackit.org/lib/foo\";\n" +
			"declare function foo:echo($s as item()*) as item()*\n" +
			"{ ($s, $s) };";
	
	private static final String QUERY =
			"import module namespace foo=\"http://brackit.org/lib/foo\";\n" +
			"foo:echo('hello')";

	public static void main(String[] args) {
		try {
			compileAndImportLibrary();
			System.out.println();
			dynamicLibraryImport();
		} catch (QueryException e) {
			System.err.print("XQuery error ");
			System.err.print(e.getCode());
			System.err.print(": ");
			System.err.println(e.getMessage());
		}
	}

	private static void compileAndImportLibrary() throws QueryException {
		// initialize query context
		QueryContext ctx = new QueryContext();
		// use a single compile chain for all queries
		CompileChain cc = new CompileChain();

		// compile library module with current compile chain
		System.out.println("Compiling library module:");
		System.out.println(LIBRARY_MODULE);	
		new XQuery(cc, LIBRARY_MODULE);

		// now run a query that imports that library module
		System.out.println();
		System.out.println("Run query with library import:");
		System.out.println(QUERY);	
		XQuery q = new XQuery(cc, QUERY);
		q.setPrettyPrint(true);
		q.serialize(ctx, System.out);
		System.out.println();
	}
	
	private static void dynamicLibraryImport() throws QueryException {
		// initialize query context
		QueryContext ctx = new QueryContext();
		// provide a custom module resolver
		ModuleResolver resolver = new BaseResolver() {			
			@Override
			public List<String> load(String uri, String[] locations)
					throws IOException {
				if (uri.equals(LIBRARY_URI)) {
					System.out.println("-> Resolving module '" + uri + "'");
					ArrayList<String> mod = new ArrayList<String>();
					mod.add(LIBRARY_MODULE);
					return mod;
				}
				return super.load(uri, locations);
			}
		};
		// use a compile chain with our custom resolver
		CompileChain cc = new CompileChain(resolver);

		// now run a query that imports the library module
		System.out.println();
		System.out.println("Run query with library import:");
		System.out.println(QUERY);	
		XQuery q = new XQuery(cc, QUERY);
		q.setPrettyPrint(true);
		q.serialize(ctx, System.out);
		System.out.println();
	}
}