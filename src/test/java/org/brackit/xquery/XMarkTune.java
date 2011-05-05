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
package org.brackit.xquery;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.compiler.profiler.ProfilingCompiler.ProfilingMainModule;
import org.brackit.xquery.node.linked.LNode;
import org.brackit.xquery.node.linked.LNodeBuilder;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Before;
import org.junit.Test;

public class XMarkTune extends XQueryBaseTest {

	private static final String QUERY_DIR = "/xmark/queries/orig/";

	@Test
	public void testNormal() throws Exception, IOException {
		LNodeBuilder builder = new LNodeBuilder();
		new DocumentParser(new File("/docs/xmark20.xml")).parse(builder);
		LNode subtreeRoot = builder.root();
		Collection<?> locator = subtreeRoot.getCollection();
		for (int i = 0; i < 100; i++)
			runQuery(ctx, locator, "q10.xq");
		// System.out.println(ParallelExpr.);
	}

	@Test
	public void join() throws Exception, IOException {
		// storeDocument("tpcc.xml", new File("/docs/tpcc.xml"));
		XQuery xQuery = new XQuery(readQuery("./", "fulljoin.xq"));
		xQuery.serialize(ctx, System.out);
		// ((ProfilingMainModule)
		// xQuery.getMainModule()).visualize("/media/ramdisk");
	}

	@Test
	public void outerjoin() throws Exception, IOException {
		// storeDocument("tpcc.xml", new File("/docs/tpcc.xml"));
		XQuery xQuery = new XQuery(readQuery("./", "leftouterjoin.xq"));
		xQuery.serialize(ctx, System.out);
		// ((ProfilingMainModule)
		// xQuery.getMainModule()).visualize("/media/ramdisk");
	}

	@Test
	public void data() throws Exception, IOException {
		// storeDocument("tpcc.xml", new File("/docs/tpcc.xml"));
		XQuery xQuery = new XQuery(
				"io:writeline(xs:anyURI('file://media/ramdisk/filtered_xq.dat'), for $line in io:readline(xs:anyURI('file://data/1m.dat')) where fn:contains($line, '44443333') return $line)");
		xQuery.serialize(ctx, System.out);
		// ((ProfilingMainModule)
		// xQuery.getMainModule()).visualize("/media/ramdisk");
	}

	@Test
	public void experimental2() throws Exception, IOException {
		for (int i = 0; i < 100; i++) {
			long start = System.currentTimeMillis();
			XQuery xQuery = new XQuery(
					"io:writeline(xs:anyURI('file://media/ramdisk/filtered_xq.dat'), for $line in io:readline(xs:anyURI('file://data/10m.dat')) where fn:contains($line, '44443333') return $line)");
			xQuery.serialize(ctx, new PrintStream(new OutputStream() {
				@Override
				public void write(int b) throws IOException {
				}
			}));
			long end = System.currentTimeMillis();
			System.out.println((end - start));
		}
	}

	@Test
	public void experimental() throws Exception, IOException {
		long start = System.currentTimeMillis();
		new XQuery(readQuery("./", "query.xq")).serialize(ctx, new PrintStream(
				new OutputStream() {
					@Override
					public void write(int b) throws IOException {
					}
				}));
		long end = System.currentTimeMillis();
		System.out.println((end - start));
	}

	private void runQuery(QueryContext ctx, Collection<?> locator, String query)
			throws QueryException, DocumentException, IOException {
		System.gc();
		System.gc();
		System.gc();
		System.out.println((Runtime.getRuntime().totalMemory() - Runtime
				.getRuntime().freeMemory())
				/ (double) (1024 * 1024));

		ctx.setDefaultContext(locator.getDocument(), Int32.ONE, Int32.ONE);
		long start = System.currentTimeMillis();
		XQuery xq = new XQuery(readQuery(QUERY_DIR, query));
		long compile = System.currentTimeMillis();
		Sequence result = xq.execute(ctx);
		Iter it = result.iterate();
		// try
		// {
		// while (it.next() != null);
		// }
		// finally
		// {
		// it.close();
		// }
		xq.serialize(ctx, new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
			}
		}));
		long end = System.currentTimeMillis();
		System.out.println((end - start));
		if (xq.getMainModule() instanceof ProfilingMainModule)
			((ProfilingMainModule) xq.getMainModule())
					.visualize(XQuery.DEBUG_DIR);
	}

	@Before
	public void setUp() throws Exception, FileNotFoundException {
		super.setUp();
		System.gc();
		System.gc();
	}
}
