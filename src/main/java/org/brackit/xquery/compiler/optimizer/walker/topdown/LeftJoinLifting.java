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
package org.brackit.xquery.compiler.optimizer.walker.topdown;

import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.compiler.optimizer.walker.Walker;
import org.brackit.xquery.util.Cmp;

/**
 * @author Sebastian Baechle
 * 
 */
public class LeftJoinLifting extends Walker {

	@Override
	protected AST visit(AST join) {
		if ((join.getType() != XQ.Join) || (!join.checkProperty("leftJoin"))) {
			return join;
		}

		AST leftIn = join.getChild(0).getChild(0);
		AST rightIn = join.getChild(1).getChild(0);
		if ((rightIn.getType() != XQ.Join)
				|| (join.getProperty("cmp") != Cmp.eq)
				|| (!constTrueJoinKey(findEnd(leftIn).getChild(0)))
				|| (!constTrueJoinKey(findEnd(rightIn.getChild(3)).getChild(0)))) {
			return join;
		}

		// use as new left in a concatenation of the current
		// left input and the converted join; the join
		// is transformed to a left join where the output
		// is simply the "TRUE" join key and where the post-join
		// output is a concatenation of the original post, the
		// original output and the post pipeline of the current join
		AST newLeftIn = new AST(XQ.Start);
		newLeftIn.addChild(leftIn.copyTree());
		AST newLeftInEnd = findEnd(newLeftIn);
		AST ljoin = rightIn.copy();
		ljoin.setProperty("leftJoin", Boolean.TRUE);
		ljoin.addChild(rightIn.getChild(0).copyTree());
		ljoin.addChild(rightIn.getChild(1).copyTree());
		AST outStart = new AST(XQ.Start);
		outStart.addChild(rightIn.getChild(3).copyTree());
		ljoin.addChild(outStart);
		ljoin.addChild(emptyJoinInput(false));
		// concat join
		newLeftInEnd.getParent().replaceChild(newLeftInEnd.getChildIndex(),
				ljoin);
		// replace end and join key from post pipeline
		// with post pipeline from current join
		newLeftInEnd = findEnd(ljoin.getChild(2));
		newLeftInEnd.getParent().replaceChild(newLeftInEnd.getChildIndex(),
				join.getChild(2).getChild(0).copyTree());

		// replace the left input with the assembled join
		// and replace the right input and the post-join
		// with empty pipelines 
		join.replaceChild(0, newLeftIn);
		join.replaceChild(1, emptyJoinInput(true));
		join.getChild(2).replaceChild(0, new AST(XQ.End));

		snapshot();
		return join;
	}

	private AST emptyJoinInput(boolean withStart) {
		AST end = new AST(XQ.End);
		end.addChild(new AST(XQ.Bool, Bool.TRUE));
		
		if (!withStart) {
			return end;
		}
		
		AST in = new AST(XQ.Start);		
		in.addChild(end);
		return in;
	}

	private AST findEnd(AST node) {
		AST tmp = node;
		while (tmp.getType() != XQ.End) {
			tmp = tmp.getLastChild();
		}
		return tmp;
	}

	private boolean constTrueJoinKey(AST joinKey) {
		return ((joinKey.getType() == XQ.Bool) && (((Bool) joinKey.getValue()).bool));
	}
}
