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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Sebastian Baechle
 *
 */
public class Main {

	public static void main(String[] args) {
		try {			
			if (args.length == 0) {
				printUsage();	
			}
			String query = args[args.length - 1];
			if (query.equals("-")) {
				query = readString(System.in);
			}

			new XQuery(query).serialize(new QueryContext(), System.out);
		} catch (QueryException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(-2);
		} catch (IOException e) {
			System.out.println("I/O Error: " + e.getMessage());
			System.exit(-3);
		}
	}
	
	private static String readString(InputStream in) throws IOException {
		int r;
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		while ((r = in.read()) != -1) {
			payload.write(r);
		}
		String string = payload.toString("UTF-8");
		return string;
	}
	
	private static void printUsage() {
		System.out.println("No query provided");
		System.out.println("Usage: java " + Main.class.getName() + " [options] query");
		System.out.println("Options: [options not supported yet]");
		System.exit(-1);
	}
}
