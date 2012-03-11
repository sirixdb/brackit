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
package org.brackit.xquery.node.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Stream;

/**
 * A Stream of SubtreeParsers that delivers one SubtreeParser for each item in
 * the sequence.
 * 
 * @author Martin Hiller
 * 
 */
public class SequenceParser implements Stream<SubtreeParser> {

	private Iter it;
	
	public SequenceParser(Sequence seq) {
		this.it = seq.iterate();
	}
	
	@Override
	public void close() {
		if (it != null) {
			it.close();
			it = null;
		}
	}

	@Override
	public SubtreeParser next() throws DocumentException {
		
		if (it == null) {
			throw new DocumentException("Stream already closed!");
		}
		
		try {
			
			Item item = it.next();
			if (item == null) {
				close();
				return null;
			}
			
			SubtreeParser parser = null;
			if (item instanceof Atomic) {
				
				String s = ((Atomic) item).stringValue();
				
				// string value is either a file URI or an XML fragment
				try {
					
					URI uri = new URI(s);
					if (uri.getScheme() == null || !uri.getScheme().equals("file")) {
						close();
						throw new DocumentException(String.format("URI %s does not specify a file.", uri));
					}
					File file = new File(uri);
					try {
						parser = new DocumentParser(file);
					} catch (FileNotFoundException e) {
						throw new DocumentException(e);
					}
					
				} catch (URISyntaxException e) {
					
					// take string as XML fragment
					parser = new DocumentParser(s);
					
				}
			} else {
				// take subtree as new document
				Node<?> root = (Node<?>) item;
				parser = new StreamSubtreeParser(root.getSubtree());
			}
			
			return parser;
			
		} catch (QueryException e) {
			close();
			throw new DocumentException(e);
		}
	}
}
