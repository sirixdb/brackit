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
package org.brackit.xquery.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.expr.ExtVariable;

/**
 * @author Sebastian Baechle
 * 
 */
public class Variables {
	protected final List<ExtVariable> variables = new ArrayList<ExtVariable>(0);

	protected final List<Variables> imported = new ArrayList<Variables>(0);
	
	public void declare(ExtVariable variable) throws QueryException {
		QNm name = variable.getName();
		for (ExtVariable v : variables) {
			if (v.getName().eq(name)) {
				throw new QueryException(ErrorCode.ERR_DUPLICATE_VARIABLE_DECL,
						"Variable $%s has already been declared", v.getName());
			}
		}
		variables.add(variable);
	}
	
	public void importVariables(Variables variables) throws QueryException {
		for (ExtVariable imported : variables.variables) {
			QNm name = imported.getName();
			for (ExtVariable v : this.variables) {
				if (v.getName().eq(name)) {
					throw new QueryException(ErrorCode.ERR_DUPLICATE_VARIABLE_DECL,
							"Variable $%s has already been declared", v.getName());
				}
			}
		}
		imported.add(variables);
	}
	
	public List<ExtVariable> getDeclaredVariables() {
		return Collections.unmodifiableList(variables);
	}
}
