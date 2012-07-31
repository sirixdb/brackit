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
package org.brackit.xquery;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.brackit.xquery.compiler.CompileChain;
import org.brackit.xquery.module.Module;
import org.brackit.xquery.operator.TupleImpl;
import org.brackit.xquery.util.Cfg;
import org.brackit.xquery.util.serialize.Serializer;
import org.brackit.xquery.util.serialize.StringSerializer;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class XQuery {
	public static final String DEBUG_CFG = "org.brackit.xquery.debug";
	public static final String DEBUG_DIR_CFG = "org.brackit.xquery.debugDir";
	public static boolean DEBUG = Cfg.asBool(DEBUG_CFG, false);
	public static String DEBUG_DIR = Cfg.asString(DEBUG_DIR_CFG, "debug/");

	private final Module module;
	private boolean prettyPrint;

	public XQuery(Module module) {
		this.module = module;
	}

	public XQuery(String query) throws QueryException {
		this.module = new CompileChain().compile(query);
	}

	public XQuery(CompileChain chain, String query) throws QueryException {
		this.module = chain.compile(query);
	}

	public Module getModule() {
		return module;
	}

	public Sequence execute(QueryContext ctx) throws QueryException {
		return run(ctx, true);
	}
	
	public Sequence evaluate(QueryContext ctx) throws QueryException {
		return run(ctx, false);
	}

	private Sequence run(QueryContext ctx, boolean lazy) throws QueryException {
		Expr body = module.getBody();
		if (body == null) {
			throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR,
					"Module does not contain a query body.");
		}
		Sequence result = body.evaluate(ctx, new TupleImpl());

		if ((!lazy) || (body.isUpdating())) {
			// iterate possibly lazy result sequence to "pull-in" all pending
			// updates
			if ((result != null) && (!(result instanceof Item))) {
				Iter it = result.iterate();
				try {
					while (it.next() != null)
						;
				} finally {
					it.close();
				}
			}
			ctx.applyUpdates();
		}

		return result;
	}

	public void serialize(QueryContext ctx, PrintStream out)
			throws QueryException {
		serialize(ctx, new PrintWriter(out));
	}

	public void serialize(QueryContext ctx, PrintWriter out)
			throws QueryException {
		Sequence result = run(ctx, true);
		if (result == null) {
			return;
		}
		StringSerializer serializer = new StringSerializer(out);
		serializer.setFormat(prettyPrint);
		serializer.serialize(result);
	}
	
	public void serialize(QueryContext ctx, Serializer serializer) throws QueryException {
		Sequence result = run(ctx, true);
		if (result == null) {
			return;
		}
		serializer.serialize(result);
	}

	public boolean isPrettyPrint() {
		return prettyPrint;
	}

	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}
}
