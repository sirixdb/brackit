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
package org.brackit.xquery.function.io;

import java.io.File;
import java.io.FileFilter;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.util.Regex;
import org.brackit.xquery.util.Regex.Mode;
import org.brackit.xquery.util.annotation.FunctionAnnotation;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.type.AtomicType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * @author Sebastian Baechle
 * 
 */
@FunctionAnnotation(description = "Lists all files in the given path. "
		+ "The optional filter pattern is evaluated according to "
		+ "fn:matches without additional flags, i.e., to match all"
		+ "files in a directory ending with \".xml\" you must use \"\\.xml$\" "
		+ "instead of a shell-like \"*.xml\"", parameters = { "$path",
		"$pattern" })
public class Ls extends AbstractFunction {
	public static final QNm DEFAULT_NAME = new QNm(IOFun.IO_NSURI,
			IOFun.IO_PREFIX, "ls");

	public Ls(boolean withFilter) {
		this(DEFAULT_NAME, withFilter);
	}

	public Ls(QNm name, boolean withFilter) {
		super(name, withFilter ? (new Signature(new SequenceType(
				AtomicType.STR, Cardinality.ZeroOrMany), new SequenceType(
				AtomicType.STR, Cardinality.One), new SequenceType(
				AtomicType.STR, Cardinality.ZeroOrOne))) : (new Signature(
				new SequenceType(AtomicType.STR, Cardinality.ZeroOrMany),
				new SequenceType(AtomicType.STR, Cardinality.One))), true);
	}

	@Override
	public Sequence execute(StaticContext sctx, QueryContext ctx,
			Sequence[] args) throws QueryException {
		File dir = new File(((Atomic) args[0]).stringValue());
		FileFilter filter = null;
		if (args.length > 1) {
			final String pattern = ((Atomic) args[1]).stringValue();
			filter = new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					try {
						if (pattern.isEmpty()) {
							return true;
						}
						Sequence match = Regex.match(Mode.MATCH,
								pathname.getName(), pattern, null, null);
						return match.booleanValue();
					} catch (QueryException e) {
						return false;
					}
				}
			};
		}
		File[] files = dir.listFiles(filter);
		Str[] res = new Str[files.length];
		for (int i = 0; i < files.length; i++) {
			res[i] = new Str(files[i].getAbsolutePath());
		}
		return new ItemSequence(res);
	}
}