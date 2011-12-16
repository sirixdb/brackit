package org.brackit.xquery.compiler.optimizer.walker;

import static org.brackit.xquery.compiler.XQ.AndExpr;
import static org.brackit.xquery.compiler.XQ.Selection;

import org.brackit.xquery.compiler.AST;

/**
 * Merges sequences of select predicates into a single conjunctions.
 * 
 * @author Sebastian Baechle
 * 
 */
public class PredicateConjunction extends Walker {

	@Override
	protected AST visit(AST select) {
		if (select.getType() != Selection) {
			return select;
		}		
		AST input = select.getChild(0);
		if (input.getType() != Selection) {
			return select;
		}
		while (input.getType() == Selection) {
			AST predicate = select.getChild(1);
			AST tmp = new AST(AndExpr);			
			tmp.addChild(input.getChild(1));
			tmp.addChild(predicate);
			input = input.getChild(0);
			select.replaceChild(1, tmp);
			select.replaceChild(0, input);
		}

		return select;
	}

}
