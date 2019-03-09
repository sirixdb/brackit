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
package org.brackit.xquery.atomic;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Type;

/**
 * An {@link Atomic} defines the common interface of an atomic value in the
 * system.
 *
 * <p>
 * The API is designed to follow the general rules of the <b>XQuery and XPath
 * Data Model 3.0</b> and <b>XQuery 3.0</b>.
 * </p>
 * <p>
 * Implementors have to ensure to override {@link Object#equals(Object)} and
 * {@link Object#hashCode()} appropriately. The methods
 * {@link Object#equals(Object)} and {@link Comparable#compareTo(Object)} have
 * to be consistent with {@link Atomic#atomicCmp(Atomic)}.
 * </p>
 *
 * @see http://www.w3.org/TR/xpath-datamodel-30/
 * @see http://www.w3.org/TR/xquery-30/
 *
 * @author Sebastian Baechle
 *
 */
public interface Atomic extends Item, Comparable<Atomic> {

	/**
	 * Returns the {@link Type} of this value.
	 *
	 * @return the {@link Type} of this value
	 */
	public Type type();

	/**
	 * Compares this atomic with the given one. Numeric types are expected to
	 * perform numeric type promotion as xs:anyURI is expected to be promoted to
	 * xs:string.
	 *
	 * This method backs up the operations lt, le, eq, ge, gt on atomic types as
	 * defined in XQuery 3.0: B.2 Operator Mapping.
	 *
	 * The result is defined as in {@link Comparable#compareTo(Object)}.
	 *
	 * If two Atomics are of a different, incomparable type, an exception with
	 * error code {@link ErrorCode#ERR_TYPE_INAPPROPRIATE_TYPE} must be thrown.
	 */
	public int cmp(Atomic atomic) throws QueryException;

	/**
	 * Compares this atomic with the given one. Numeric types are expected to
	 * perform numeric type promotion as xs:anyURI is expected to be promoted to
	 * xs:string.
	 *
	 * This method backs up only the operation eq on atomic types as defined in
	 * XQuery 3.0: B.2 Operator Mapping.
	 *
	 * The result is defined as in {@link Comparable#compareTo(Object)}.
	 *
	 * If two Atomics are of a different, incomparable type, an exception with
	 * error code {@link ErrorCode#ERR_TYPE_INAPPROPRIATE_TYPE} must be thrown.
	 */
	public boolean eq(Atomic atomic) throws QueryException;

	/**
	 * Compares this atomic with the given one. This order is defined to be
	 * global and total for all atomic types.
	 *
	 * CAVEAT: The total order is implementation defined and does not strictly
	 * obey the definitions of the <code>eq</code> definition. In general, the
	 * order relation as defined by <code>eq</code> is used. Type promotion is
	 * performed as usual for numeric types and URI types. To enforce a total
	 * ordering, a stable total order is guaranteed by the implementation also
	 * if <code>eq</code> is not defined (e.g. xs:duration) or if
	 * <code>eq</code> only defines partial (e.g. xs:dateTime) ordering.
	 *
	 * This method backs up the functions fn:distinct and fn:index-of, which
	 * require tests for identity and duplicate elimination. According to their
	 * definition, types that cannot be compared, i.e., the <code>eq</code>
	 * operator is not defined for their types, are considered to be distinct.
	 * Also, values of type xs:untypedAtomic are compared as xs:string
	 *
	 * The result is defined as in {@link Comparable#compareTo(Object)}.
	 */
	public int atomicCmp(Atomic atomic);

	/**
	 * Shortcut that returns the atomic code {@link Type#atomicCode()} of this
	 * value.
	 */
	public int atomicCode();

	/**
	 * Returns the string value of this item.
	 *
	 * @see http://www.w3.org/TR/xquery-operators/#func-string
	 */
	public String stringValue();

	/**
	 * Returns the string value of this item.
	 *
	 * @see http://www.w3.org/TR/xquery-operators/#func-string
	 */
	public Str asStr();

	/**
	 * Returns the string value of this item as untyped atomic.
	 *
	 * @see http://www.w3.org/TR/xquery-operators/#func-string
	 */
	public Una asUna();

	/**
	 * Returns a copy of this value of the given type. The value must be in the
	 * value space of the target type and must not violate facets of the given
	 * type if any.
	 */
	public Atomic asType(Type type) throws QueryException;
}
