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
package org.brackit.xquery.compiler.parser;

import java.io.File;
import java.io.FileWriter;

import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.tree.DOTTreeGenerator;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeAdaptor;
import org.antlr.stringtemplate.StringTemplate;
import org.apache.log4j.Logger;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class DotUtil {
	public static final String PLOT_TYPE = "svg";

	private static final Logger log = Logger.getLogger(DotUtil.class);

	public static class DotTypedTreeGenerator extends DOTTreeGenerator {
		protected String[] tokenNames;

		public DotTypedTreeGenerator(String[] tokenNames) {
			this.tokenNames = tokenNames;
		}

		@Override
		protected StringTemplate getNodeST(TreeAdaptor adaptor, Object t) {
			// if (true)
			// return super.getNodeST(adaptor, t);

			String type = tokenNames[adaptor.getType(t)];
			String text = adaptor.getText(t);
			if (!type.equals(text)) {
				text = type + "['" + text + "']";
			}

			StringTemplate nodeST = _nodeST.getInstanceOf();
			String uniqueName = "n" + getNodeNumber(t);
			nodeST.setAttribute("name", uniqueName);

			nodeST.setAttribute("text", fixString(text));
			return nodeST;
		}
	}

	public static void drawToDotFile(Tree ast, String[] tokenNames, String dir,
			String name) {
		drawDotToFile(getDotFromAst(ast, tokenNames), dir, name);
	}

	private static String getDotFromAst(Tree t, String[] tokenNames) {
		DOTTreeGenerator gen = new DotTypedTreeGenerator(tokenNames);
		StringTemplate st = gen.toDOT(t, new CommonTreeAdaptor());
		return st.toString();
	}

	public static void drawDotToFile(String dotString, String dir, String name) {
		String svgFile = dir + name + "." + PLOT_TYPE;

		try {
			File f = File.createTempFile("ast", "dot");
			f.deleteOnExit();

			// Create the output file and write the dot spec to it
			FileWriter outputStream = new FileWriter(f);
			outputStream.write(dotString);
			outputStream.close();

			// Invoke dot to generate a .png file
			if (log.isDebugEnabled()) {
				log
						.debug(String.format(
								"Drawing AST '%s' with dot to SVG '%s'", name,
								svgFile));
			}
			Process proc = Runtime.getRuntime().exec(
					"dot -T" + PLOT_TYPE + " -o" + svgFile + " " + f);
			proc.waitFor();
			f.delete();
		} catch (Exception e) {
			log.error(String.format("Creating dot plan '%s' failed.", svgFile),
					e);
		}
	}
}
