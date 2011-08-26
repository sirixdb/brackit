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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.ResultChecker;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.XQueryBaseTest;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.module.LibraryModule;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 * 
 */
public class LibraryModulesTest extends XQueryBaseTest {

	private static final String FOO = "module namespace foo=\"http://brackit.org/lib/foo\"; "
			+ "declare function echo($s as item()*) as item()* "
			+ "{ ($s, $s) };";

	private static final String FOO2 = "module namespace foo=\"http://brackit.org/lib/foo\"; "
			+ "declare function echo2($s as item()*) as item()* "
			+ "{ ($s, $s) };";

	private static final String BAR = "module namespace bar=\"http://brackit.org/lib/bar\"; "
			+ "declare function echo2($s as item()*) as item()* "
			+ "{ ($s, $s) };";

	private static final String IMPORT_FOO = "import module namespace foo=\"http://brackit.org/lib/foo\"; ";

	private static final String IMPORT_BAR = "import module namespace bar=\"http://brackit.org/lib/bar\"; ";

	@Test
	public void defineModule() throws Exception {
		XQuery xq = new XQuery(FOO);
		xq.getModule();
		// simply rest if no error happens
	}

	@Test
	public void importModule() throws Exception {
		final BaseResolver res = new BaseResolver();
		CompileChain chain = new CompileChain() {
			private final ModuleResolver resolver = res;

			@Override
			protected ModuleResolver getModuleResolver() {
				return resolver;
			}
		};
		XQuery xq = new XQuery(chain, FOO);
		LibraryModule module = (LibraryModule) xq.getModule();
		res.register(module.getTargetNS().getUri(), module);
		XQuery xq2 = new XQuery(chain, IMPORT_FOO + "foo:echo('y')");
		QueryContext ctx = createContext();
		Sequence result = xq2.execute(ctx);
		ResultChecker.check(new ItemSequence(new Str("y"), new Str("y")), result);
	}

	@Test
	public void importModulesInSameTargetNS() throws Exception {
		final BaseResolver res = new BaseResolver();
		CompileChain chain = new CompileChain() {
			private final ModuleResolver resolver = res;

			@Override
			protected ModuleResolver getModuleResolver() {
				return resolver;
			}
		};
		XQuery xq = new XQuery(chain, FOO);
		LibraryModule module = (LibraryModule) xq.getModule();
		res.register(module.getTargetNS().getUri(), module);
		xq = new XQuery(chain, FOO2);
		module = (LibraryModule) xq.getModule();
		res.register(module.getTargetNS().getUri(), module);
		XQuery xq2 = new XQuery(chain, IMPORT_FOO
				+ "(foo:echo('y'), foo:echo2('y'))");
		QueryContext ctx = createContext();
		Sequence result = xq2.execute(ctx);
		ResultChecker.check(new ItemSequence(new Str("y"), new Str("y"),
				new Str("y"), new Str("y")), result);
	}

	@Test
	public void importModulesInDifferentTargetNS() throws Exception {
		final BaseResolver res = new BaseResolver();
		CompileChain chain = new CompileChain() {
			private final ModuleResolver resolver = res;

			@Override
			protected ModuleResolver getModuleResolver() {
				return resolver;
			}
		};
		XQuery xq = new XQuery(chain, FOO);
		LibraryModule module = (LibraryModule) xq.getModule();
		res.register(module.getTargetNS().getUri(), module);
		xq = new XQuery(chain, BAR);
		module = (LibraryModule) xq.getModule();
		res.register(module.getTargetNS().getUri(), module);
		XQuery xq2 = new XQuery(chain, IMPORT_FOO + IMPORT_BAR
				+ "(foo:echo('y'), bar:echo2('y'))");
		QueryContext ctx = createContext();
		Sequence result = xq2.execute(ctx);
		ResultChecker.check(new ItemSequence(new Str("y"), new Str("y"),
				new Str("y"), new Str("y")), result);
	}

	@Test
	public void importModulesInSameTargetNSWithConflict() throws Exception {
		final BaseResolver res = new BaseResolver();
		CompileChain chain = new CompileChain() {
			private final ModuleResolver resolver = res;

			@Override
			protected ModuleResolver getModuleResolver() {
				return resolver;
			}
		};
		XQuery xq = new XQuery(chain, FOO);
		LibraryModule module = (LibraryModule) xq.getModule();
		res.register(module.getTargetNS().getUri(), module);
		xq = new XQuery(chain, FOO);
		module = (LibraryModule) xq.getModule();
		res.register(module.getTargetNS().getUri(), module);
		try {
			new XQuery(chain, IMPORT_FOO + "foo:echo('y')");
			fail("double definition of function not detected");
		} catch (QueryException e) {
			assertEquals("XQST0034", e.getCode().getLocalName());
		}
	}
}
