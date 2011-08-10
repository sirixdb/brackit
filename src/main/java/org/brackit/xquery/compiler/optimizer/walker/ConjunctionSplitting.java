package org.brackit.xquery.compiler.optimizer.walker;

import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.parser.XQueryParser;

/**
 * Breaks conjunctions of where clauses into separate where clauses, in order
 * to enable join recognition on multiple sources with a single predicate.
 * 
 * For example:
 * where expr1 and expr2 and expr3 -> where expr1 where expr2 where expr3
 * 
 * @author csauer
 *
 */
public class ConjunctionSplitting extends Walker {

	@Override
	protected AST visit(AST node)
	{
		if (node.getType() != XQueryParser.Selection) {
			return node;
		}
		
		AST currentSelect = node;
		
		while (true)
		{
			AST input = currentSelect.getChild(0);
			AST predicate = currentSelect.getChild(1);
			
			if (predicate.getType() != XQueryParser.AndExpr) {
				break;
			}
			
			AST andLeft = predicate.getChild(0);
			AST andRight = predicate.getChild(1);
			
			AST newSelection = new AST(XQueryParser.Selection, "Selection");
			newSelection.addChild(input);
			newSelection.addChild(andLeft);
			
			currentSelect.replaceChild(0, newSelection);
			currentSelect.replaceChild(1, andRight);
			
			currentSelect = newSelection;
		}
		
		return node;
	}	
	
}
