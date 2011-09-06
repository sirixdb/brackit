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
package org.brackit.xquery.xdm;

/**
 * <p>
 * A scope comprises the in-scope namespace mappings and in-scope schema types.
 * </p>
 * 
 * @see http://www.w3.org/TR/xpath-datamodel-30/
 * @see http://www.w3.org/TR/xml-names11/
 * 
 * @author Sebastian Baechle
 * 
 */
public interface Scope {

	public Stream<String> localPrefixes() throws DocumentException;

	/**
	 * Returns the namespace URI for unprefixed QNames.
	 * 
	 * @return the namespace URI for unprefixed QNames
	 * @throws DocumentException
	 */
	public String defaultNS() throws DocumentException;

	/**
	 * Add a new namespace mapping to this scope. If the prefix is
	 * <code>null</code> or the empty string, the default namespace URI is set.
	 * 
	 * <p>
	 * It must be ensured that:
	 * <ul>
	 * <li>the prefix xml is not bound to some namespace URI other than
	 * http://www.w3.org/XML/1998/namespace</li>
	 * <li>a prefix other than xml is bound to the namespace URI
	 * http://www.w3.org/XML/1998/namespace</li>
	 * <li>the prefix xmlns is bound to any namespace URI</li>
	 * <li>a prefix is bound to the namespace URI http://www.w3.org/2000/xmlns/</li>
	 * 
	 * @throws DocumentException
	 */
	public void addPrefix(String prefix, String uri) throws DocumentException;

	/**
	 * Resolves the namespace URI prefix for a given prefix. If the prefix is
	 * <code>null</code> or the empty string, the default namespace URI is
	 * returned. The method returns <code>null</code> if the given prefix is
	 * neither <code>null</code> nor the empty string and is not bound to a
	 * namespace URI.
	 * 
	 * <p>
	 * It must be ensured that:
	 * <ul>
	 * <li>the prefix xml is not bound to some namespace URI other than
	 * http://www.w3.org/XML/1998/namespace</li>
	 * <li>a prefix other than xml is bound to the namespace URI
	 * http://www.w3.org/XML/1998/namespace</li>
	 * <li>the prefix xmlns is bound to any namespace URI</li>
	 * <li>a prefix is bound to the namespace URI http://www.w3.org/2000/xmlns/</li>
	 * 
	 * @throws DocumentException
	 */
	public String resolvePrefix(String prefix) throws DocumentException;

	public void setDefaultNS(String uri) throws DocumentException;
}
