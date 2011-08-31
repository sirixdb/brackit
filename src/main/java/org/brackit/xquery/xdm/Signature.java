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

import org.brackit.xquery.xdm.type.SequenceType;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Signature {
	private final SequenceType resultType;

	private final SequenceType[] params;

	private final boolean lastIsVarArg;

	private final boolean defaultsIsContextItem;

	public Signature(SequenceType resultType, SequenceType... params) {
		this.resultType = resultType;
		this.params = params;
		this.lastIsVarArg = false;
		this.defaultsIsContextItem = false;
	}

	public Signature(SequenceType resultType, boolean lastIsVarArg,
			boolean defaultIsContextItem, SequenceType... params) {
		this.resultType = resultType;
		this.params = params;
		this.lastIsVarArg = lastIsVarArg;
		this.defaultsIsContextItem = defaultIsContextItem;
	}

	public SequenceType getResultType() {
		return resultType;
	}

	public SequenceType[] getParams() {
		return params;
	}

	public boolean lastIsVarArg() {
		return lastIsVarArg;
	}

	public boolean defaultIsContextItem() {
		return defaultsIsContextItem;
	}

	public String toString() {
		StringBuilder st = new StringBuilder();
		st.append("(");
		if (params.length > 0) {
			st.append(params[0]);
			for (int i = 1; i < params.length; i++) {
				st.append(", ");
				st.append(params[i]);
			}
		}
		st.append(") : ");
		st.append(resultType);
		return st.toString();
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Signature)) {
			return false;
		}
		Signature s = (Signature) obj;
		if (params.length != s.params.length) {
			return false;
		}
		if ((!resultType.equals(s.resultType))
				|| (lastIsVarArg != s.lastIsVarArg)
				|| (defaultsIsContextItem != s.defaultsIsContextItem)) {
			return false;
		}
		for (int i = 0; i < params.length; i++) {
			if (!params[i].equals(s.params[i])) {
				return false;
			}
		}
		return true;
	}
}
