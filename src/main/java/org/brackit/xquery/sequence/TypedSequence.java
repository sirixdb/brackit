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
package org.brackit.xquery.sequence;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntegerNumeric;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.sequence.type.AtomicType;
import org.brackit.xquery.sequence.type.Cardinality;
import org.brackit.xquery.sequence.type.ItemType;
import org.brackit.xquery.sequence.type.SequenceType;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class TypedSequence implements Sequence {
	private final QueryContext ctx;

	private final SequenceType type;

	private final Sequence arg;

	private final boolean applyFunctionConversion;

	private final boolean enforceDouble;

	private Boolean booleanValue;

	private IntegerNumeric size;

	public TypedSequence(QueryContext ctx, SequenceType type, Sequence arg) {
		this.ctx = ctx;
		this.type = type;
		this.arg = arg;
		this.applyFunctionConversion = false;
		this.enforceDouble = false;
	}

	public TypedSequence(QueryContext ctx, SequenceType type, Sequence arg,
			boolean applyFunctionConversion, boolean enforceDouble) {
		this.ctx = ctx;
		this.type = type;
		this.arg = arg;
		this.applyFunctionConversion = applyFunctionConversion;
		this.enforceDouble = enforceDouble;
	}

	@Override
	public boolean booleanValue(QueryContext ctx) throws QueryException {
		// Remember if expression is checked several times
		if (booleanValue != null) {
			return booleanValue;
		}

		booleanValue = false;
		Iter s = iterate();
		try {
			Item n;
			booleanValue = ((n = s.next()) != null) ? (n instanceof Node<?>) ? true
					: n.booleanValue(ctx)
					: false;

			// TODO: Must typed sequences be checked in full once accessed?
			while (s.next() != null)
				;
		} finally {
			s.close();
		}

		return booleanValue;
	}

	@Override
	public Iter iterate() {
		return new Iter() {
			Cardinality cardinality = type.getCardinality();
			ItemType itemType = type.getItemType();
			boolean expectAtomicType = (itemType instanceof AtomicType);
			Type expectedAtomicType = (expectAtomicType) ? ((AtomicType) itemType).type
					: null;
			int pos = 0;
			Iter s;

			@Override
			public Item next() throws QueryException {
				if (s == null) {
					s = arg.iterate();
				}

				Item next = s.next();

				if (next != null) {
					pos++;

					if ((cardinality == Cardinality.Zero)
							|| ((pos == 2) && (cardinality.atMostOne()))) {
						throw new QueryException(
								ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
								"Invalid cardinality of typed sequence (expected %s): >= %s",
								cardinality, pos);
					}

					// See XQuery 3.1.5 Function Calls
					if (expectAtomicType) {
						Atomic atomic = next.atomize();
						Type type = atomic.type();

						if ((type == Type.UNA)
								&& (expectedAtomicType != Type.UNA)
								&& (applyFunctionConversion)) {
							if (enforceDouble) {
								atomic = Cast
										.cast(ctx, atomic, Type.DBL, false);
							} else {
								atomic = Cast.cast(ctx, atomic,
										expectedAtomicType, false);
							}
						} else if (!itemType.matches(atomic)) {
							if ((applyFunctionConversion)
									&& (expectedAtomicType.isNumeric())
									&& (type.isNumeric())) {
								atomic = Cast.cast(ctx, atomic,
										expectedAtomicType, false);
							} else if ((applyFunctionConversion)
									&& (expectedAtomicType.instanceOf(Type.STR))
									&& (type.instanceOf(Type.AURI))) {
								atomic = Cast.cast(ctx, atomic,
										expectedAtomicType, false);
							} else {
								throw new QueryException(
										ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
										"Item of invalid atomic type in typed sequence (expected %s): %s",
										itemType, atomic);
							}
						}

						return atomic;
					} else if (!itemType.matches(next)) {
						throw new QueryException(
								ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
								"Item of invalid type in typed sequence (expected %s): %s",
								itemType, next);
					}
				} else if ((pos == 0) && (cardinality.moreThanZero())) {
					throw new QueryException(
							ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
							"Invalid empty typed sequence (expected %s)",
							cardinality);
				}

				return next;
			}

			@Override
			public void close() {
				if (s != null) {
					s.close();
				}
			}
		};
	}

	@Override
	public IntegerNumeric size(QueryContext ctx) throws QueryException {
		// Remember if expression is checked several times
		if (size != null) {
			return size;
		}

		IntegerNumeric count = Int32.ZERO;
		Iter s = iterate();
		try {
			while (s.next() != null) {
				count = count.inc();
			}
		} finally {
			s.close();
		}

		size = count;
		return size;
	}

	public static Sequence toTypedSequence(QueryContext ctx,
			SequenceType sequenceType, Sequence s) throws QueryException {
		if (s == null) {
			if (sequenceType.getCardinality().moreThanZero()) {
				throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
						"Invalid empty-sequence()");
			}
			return null;
		} else if (s instanceof Item) {
			// short-circuit wrapping of single item parameter
			ItemType itemType = sequenceType.getItemType();

			if (!itemType.matches((Item) s)) {
				throw new QueryException(
						ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
						"Item of invalid type in typed sequence (expected %s): %s",
						itemType, s);
			}

			return s;
		} else {
			return new TypedSequence(ctx, sequenceType, s);
		}
	}

	public static Item toTypedItem(QueryContext ctx, SequenceType sequenceType,
			Item item) throws QueryException {
		if (item == null) {
			if (sequenceType.getCardinality().moreThanZero()) {
				throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
						"Invalid empty-sequence()");
			}
			return null;
		} else {
			// short-circuit wrapping of single item parameter
			ItemType itemType = sequenceType.getItemType();

			if (!itemType.matches(item)) {
				throw new QueryException(
						ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
						"Item of invalid type in typed sequence (expected %s): %s",
						itemType, item);
			}

			return item;
		}
	}

	public static Item toTypedItem(QueryContext ctx, SequenceType sequenceType,
			Sequence sequence) throws QueryException {
		if (sequence == null) {
			if (sequenceType.getCardinality().moreThanZero()) {
				throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
						"Invalid empty-sequence()");
			}
			return null;
		} else if (sequence instanceof Item) {
			// short-circuit wrapping of single item parameter
			ItemType itemType = sequenceType.getItemType();
			Item item = (Item) sequence;

			if (!itemType.matches(item)) {
				throw new QueryException(
						ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
						"Item of invalid type in typed sequence (expected %s): %s",
						itemType, item);
			}

			return item;
		} else {
			Iter it = sequence.iterate();
			try {
				Item item = it.next();
				if (it.next() != null) {
					throw new QueryException(
							ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
							"Cannot convert %s typed sequence %s to single item",
							sequenceType, sequence);
				}
				return toTypedItem(ctx, sequenceType, item);
			} finally {
				it.close();
			}
		}
	}
}