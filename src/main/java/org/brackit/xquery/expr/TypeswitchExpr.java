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
package org.brackit.xquery.expr;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.type.AnyItemType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.ItemType;
import org.brackit.xquery.xdm.type.SequenceType;

public class TypeswitchExpr implements Expr {
    private Expr operandExpr;
    private Expr[] caseExprs;
    private SequenceType[] caseTypes;
    private boolean[] varRefs;
    private Expr defaultExpr;
    private boolean updating;
    private boolean vacuous;

    public TypeswitchExpr(Expr operandExpr, Expr[] caseExprs,
            SequenceType[] caseTypes, boolean[] varRefs, Expr defaultExpr,
            boolean updating, boolean vacuous) {
        this.operandExpr = operandExpr;
        this.caseExprs = caseExprs;
        this.caseTypes = caseTypes;
        this.varRefs = varRefs;
        this.defaultExpr = defaultExpr;
        this.updating = updating;
        this.vacuous = vacuous;
    }

    @Override
    public Sequence evaluate(QueryContext ctx, Tuple tuple)
            throws QueryException {
        return evaluate(ctx, tuple, false);
    }

    private Sequence evaluate(QueryContext ctx, Tuple tuple, boolean toItem)
            throws QueryException {
        Sequence operand = operandExpr.evaluate(ctx, tuple);

        for (int i = 0; i < caseExprs.length; i++) {
            ItemType itemType = caseTypes[i].getItemType();
            Cardinality card = caseTypes[i].getCardinality();
            Iter it = operand.iterate();

            // Test first item
            Item item = it.next();
            if (item == null) {
                if (card != Cardinality.One && card != Cardinality.OneOrMany) {
                    if (toItem) {
                        return caseExprs[i].evaluateToItem(ctx,
                                (varRefs[i] ? tuple.concat(operand) : tuple));
                    } else {
                        return caseExprs[i].evaluate(ctx, (varRefs[i] ? tuple
                                .concat(operand) : tuple));
                    }
                } else {
                    continue;
                }
            } else if (card == Cardinality.Zero || !itemType.matches(item)) {
                continue;
            }

            // Test second item
            item = it.next();
            if (item != null
                    && (card == Cardinality.One
                            || card == Cardinality.ZeroOrOne || !itemType
                            .matches(item))) {
                continue;
            }

            if (item != null && itemType != AnyItemType.ANY) {
                // Test following items
                boolean match = true;
                while ((item = it.next()) != null) {
                    if (!itemType.matches(item)) {
                        match = false;
                        break;
                    }
                }

                if (!match) {
                    continue;
                }
            }

            if (toItem) {
                return caseExprs[i].evaluateToItem(ctx, (varRefs[i] ? tuple
                        .concat(operand) : tuple));
            } else {
                return caseExprs[i].evaluate(ctx, (varRefs[i] ? tuple
                        .concat(operand) : tuple));
            }
        }

        if (toItem) {
            return defaultExpr.evaluateToItem(ctx,
                    (varRefs[varRefs.length - 1] ? tuple.concat(operand)
                            : tuple));
        } else {
            return defaultExpr.evaluate(ctx,
                    (varRefs[varRefs.length - 1] ? tuple.concat(operand)
                            : tuple));
        }
    }

    @Override
    public Item evaluateToItem(QueryContext ctx, Tuple tuple)
            throws QueryException {
        return (Item) evaluate(ctx, tuple, true);
    }

    @Override
    public boolean isUpdating() {
        return updating;
    }

    @Override
    public boolean isVacuous() {
        return vacuous;
    }

}