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
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.xdm;

import java.math.BigDecimal;

import org.brackit.xquery.atomic.Int;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.Int64;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.xdm.Facets.WS;

/**
 * Type as defined in {@linkplain http://www.w3.org/TR/xpath-datamodel/#types}.
 * 
 * @author Sebastian Baechle
 * 
 */
public final class Type {
	// TODO: Howto add range [#x10000-#xEFFFF]?
	public static final String NAME_START_CHAR_WITHOUT_COLON_PATTERN = "[A-Z]|_|[a-z]|[\u00C0-\u00D6]|[\u00D8-\u00F6]|[\u00F8-\u02FF]|[\u0370-\u037D]|[\u037F-\u1FFF]|[\u200C-\u200D]|[\u2070-\u218F]|[\u2C00-\u2FEF]|[\u3001-\uD7FF]|[\uF900-\uFDCF]|[\uFDF0-\uFFFD]";

	public static final String NAME_CHAR_OTHER_PATTERN = "|-|\\.|[0-9]|\u00B7|[\u0300-\u036F]|[\u203F-\u2040]";

	public static final String NAME_START_CHAR_PATTERN = ":|"
			+ NAME_START_CHAR_WITHOUT_COLON_PATTERN;

	public static final String NAME_CHAR_PATTERN = NAME_START_CHAR_PATTERN
			+ "|" + NAME_CHAR_OTHER_PATTERN;

	public static final String NMTOKEN_PATTERN = "(" + NAME_CHAR_PATTERN + ")+";

	public static final String NAME_PATTERN = "(" + NAME_START_CHAR_PATTERN
			+ ")(" + NAME_CHAR_PATTERN + ")*";

	public static final String NCNAME_PATTERN = "("
			+ NAME_START_CHAR_WITHOUT_COLON_PATTERN + "|"
			+ NAME_CHAR_OTHER_PATTERN + ")+";

	public final static int DATI_CODE = 1;

	public final static int DATE_CODE = 2;

	public final static int TIME_CODE = 3;

	public final static int DUR_CODE = 4;

	public final static int YMD_CODE = 5;

	public final static int DTD_CODE = 6;

	/**
	 * includes xs:float, xs:double, xs:decimal and xs:integer
	 */
	public final static int NUMERIC_CODE = 7;

	public final static int GYM_CODE = 8;

	public final static int GYE_CODE = 9;

	public final static int GMD_CODE = 10;

	public final static int GDAY_CODE = 11;

	public final static int GMON_CODE = 12;

	public final static int BOOL_CODE = 13;

	public final static int B64_CODE = 14;

	public final static int HEX_CODE = 15;

	/**
	 * includes xs:string, xs:anyURI and xs:untypedAtomic
	 */
	public final static int STRING_CODE = 16;

	public final static int QNM_CODE = 17;

	public final static int NOT_CODE = 18;

	/**
	 * xs:anyType
	 */
	public static final Type ANY = new Type("anyType", null, true, -1, false,
			false, false, false);

	/**
	 * xs:untyped
	 */
	public static final Type UN = new Type("untyped", null, true, -1, false,
			false, false, false);

	/**
	 * xs:anySimpleType
	 */
	public static final Type ANS = new Type("anySimpleType", ANY, true, -1,
			false, false, false, false);

	/**
	 * xs:IDREFS
	 */
	public static final Type IDRS = new Type("IDREFS", ANS, true, -1, false,
			false, false, false, new Facets(-1, 1, -1, null, null, null, null,
					null, null, null, -1, -1));

	/**
	 * xs:NMTOKENS
	 */
	public static final Type NMT = new Type("NMTOKENS", ANS, true, -1, false,
			false, false, false, new Facets(-1, 1, -1, null, null, null, null,
					null, null, null, -1, -1));

	/**
	 * xs:ENTITIES
	 */
	public static final Type ENTS = new Type("ENTITIES", ANS, true, -1, false,
			false, false, false, new Facets(-1, 1, -1, null, null, null, null,
					null, null, null, -1, -1));

	/**
	 * xs:anyAtomicType
	 */
	public static final Type ANA = new Type("anyAtomicType", ANS, true, 0,
			false, false, false, false);

	/**
	 * xs:untypedAtomic
	 */
	public static final Type UNA = new Type("untypedAtomic", ANA, true,
			STRING_CODE, true, false, false, false);

	/**
	 * xs:dateTime
	 */
	public static final Type DATI = new Type("dateTime", ANA, true, DATI_CODE,
			true, false, false, true, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:date
	 */
	public static final Type DATE = new Type("date", ANA, true, DATE_CODE,
			true, false, false, true, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:time
	 */
	public static final Type TIME = new Type("time", ANA, true, TIME_CODE,
			true, false, false, true, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:duration
	 */
	public static final Type DUR = new Type("duration", ANA, true, DUR_CODE,
			true, false, true, false, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:yearMonthDuration
	 */
	public static final Type YMD = new Type("yearMonthDuration", DUR, true,
			YMD_CODE, true, false, true, false, new Facets(-1, -1, -1, null,
					null, WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:dayTimeDuration
	 */
	public static final Type DTD = new Type("dayTimeDuration", DUR, true,
			DTD_CODE, true, false, true, false, new Facets(-1, -1, -1, null,
					null, WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:float
	 */
	public static final Type FLO = new Type("float", ANA, true, NUMERIC_CODE,
			true, true, false, false, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:double
	 */
	public static final Type DBL = new Type("double", ANA, true, NUMERIC_CODE,
			true, true, false, false, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:decimal
	 */
	public static final Type DEC = new Type("decimal", ANA, true, NUMERIC_CODE,
			true, true, false, false, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:integer
	 */
	public static final Type INR = new Type("integer", DEC, true, NUMERIC_CODE,
			true, true, false, false, new Facets(-1, -1, -1, "[-+]?[0-9]+",
					null, WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:nonPositiveInteger
	 */
	public static final Type NPI = new Type("nonPositiveInteger", INR, true,
			NUMERIC_CODE, false, true, false, false, new Facets(-1, -1, -1,
					"[-+]?[0-9]+", null, WS.COLLAPSE, Int32.ZERO, null, null,
					null, -1, -1));

	/**
	 * xs:negativeInteger
	 */
	public static final Type NINR = new Type("negativeInteger", NPI, true,
			NUMERIC_CODE, false, true, false, false, new Facets(-1, -1, -1,
					"[-+]?[0-9]+", null, WS.COLLAPSE, Int32.N_ONE, null, null,
					null, -1, -1));

	/**
	 * xs:long
	 */
	public static final Type LON = new Type("long", INR, true, NUMERIC_CODE,
			false, true, false, false, new Facets(-1, -1, -1, "[-+]?[0-9]+",
					null, WS.COLLAPSE, Int64.MAX_VALUE, Int64.MIN_VALUE, null,
					null, -1, -1));

	/**
	 * xs:int
	 */
	public static final Type INT = new Type("int", LON, true, NUMERIC_CODE,
			false, true, false, false, new Facets(-1, -1, -1, "[-+]?[0-9]+",
					null, WS.COLLAPSE, Int32.MAX_VALUE, Int32.MIN_VALUE, null,
					null, -1, -1));

	/**
	 * xs:short
	 */
	public static final Type SHO = new Type("short", INT, true, NUMERIC_CODE,
			false, true, false, false, new Facets(-1, -1, -1, "[-+]?[0-9]+",
					null, WS.COLLAPSE, new Int32(Short.MAX_VALUE), new Int32(
							Short.MIN_VALUE), null, null, -1, -1));

	/**
	 * xs:byte
	 */
	public static final Type BYT = new Type("byte", INT, true, NUMERIC_CODE,
			false, true, false, false, new Facets(-1, -1, -1, "[-+]?[0-9]+",
					null, WS.COLLAPSE, new Int32(Byte.MAX_VALUE), new Int32(
							Byte.MIN_VALUE), null, null, -1, -1));

	/**
	 * xs:nonNegativeInteger
	 */
	public static final Type NNI = new Type("nonNegativeInteger", INR, true,
			NUMERIC_CODE, false, true, false, false, new Facets(-1, -1, -1,
					"[-+]?[0-9]+", null, WS.COLLAPSE, null, Int32.ZERO, null,
					null, -1, -1));

	/**
	 * xs:unsignedLong
	 */
	public static final Type ULON = new Type("unsignedLong", NNI, true,
			NUMERIC_CODE, false, true, false, false, new Facets(-1, -1, -1,
					"[-+]?[0-9]+", null, WS.COLLAPSE, new Int(new BigDecimal(
							"18446744073709551615")), Int32.ZERO, null, null,
					-1, -1));

	/**
	 * xs:unsignedInt
	 */
	public static final Type UINT = new Type("unsignedInt", ULON, true,
			NUMERIC_CODE, false, true, false, false, new Facets(-1, -1, -1,
					"[-+]?[0-9]+", null, WS.COLLAPSE, new Int64(4294967295l),
					Int32.ZERO, null, null, -1, -1));

	/**
	 * xs:unsignedShort
	 */
	public static final Type USHO = new Type("unsignedShort", UINT, true,
			NUMERIC_CODE, false, true, false, false, new Facets(-1, -1, -1,
					"[-+]?[0-9]+", null, WS.COLLAPSE, new Int32(65535),
					Int32.ZERO, null, null, -1, -1));

	/**
	 * xs:unsignedByte
	 */
	public static final Type UBYT = new Type("unsignedByte", USHO, true,
			NUMERIC_CODE, false, true, false, false, new Facets(-1, -1, -1,
					"[-+]?[0-9]+", null, WS.COLLAPSE, new Int32(255),
					Int32.ZERO, null, null, -1, -1));

	/**
	 * xs:positiveInteger
	 */
	public static final Type PINR = new Type("positiveInteger", NNI, true,
			NUMERIC_CODE, false, true, false, false, new Facets(-1, -1, -1,
					"[-+]?[0-9]+", null, WS.COLLAPSE, null, Int32.ONE, null,
					null, -1, -1));

	/**
	 * xs:string
	 */
	public static final Type STR = new Type("string", ANA, true, STRING_CODE,
			true, false, false, false, new Facets(-1, -1, -1, null, null,
					WS.PRESERVE, null, null, null, null, -1, -1));

	/**
	 * xs:normalizedString
	 */
	public static final Type NSTR = new Type("normalizedString", STR, true,
			STRING_CODE, false, false, false, false, new Facets(-1, -1, -1,
					null, null, WS.REPLACE, null, null, null, null, -1, -1));

	/**
	 * xs:token
	 */
	public static final Type TOK = new Type("token", NSTR, true, STRING_CODE,
			false, false, false, false, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:language
	 */
	public static final Type LAN = new Type("language", TOK, true, STRING_CODE,
			false, false, false, false, new Facets(-1, -1, -1,
					"[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*", null, WS.COLLAPSE,
					null, null, null, null, -1, -1));

	/**
	 * xs:NMTOKEN
	 */
	public static final Type NMTS = new Type("NMTOKEN", TOK, true, STRING_CODE,
			false, false, false, false, new Facets(-1, -1, -1, NMTOKEN_PATTERN,
					null, WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:Name
	 */
	public static final Type NAM = new Type("Name", TOK, true, STRING_CODE,
			false, false, false, false, new Facets(-1, -1, -1, NAME_PATTERN,
					null, WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:NCName
	 */
	public static final Type NCN = new Type("NCName", NAM, true, STRING_CODE,
			false, false, false, false, new Facets(-1, -1, -1, NCNAME_PATTERN,
					null, WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:ID
	 */
	public static final Type ID = new Type("ID", NCN, true, STRING_CODE, false,
			false, false, false);

	/**
	 * xs:IDREF
	 */
	public static final Type IDR = new Type("IDREF", NCN, true, STRING_CODE,
			false, false, false, false);

	/**
	 * xs:ENTITY
	 */
	public static final Type ENT = new Type("ENTITY", NCN, true, STRING_CODE,
			false, false, false, false);

	/**
	 * xs:gYearMonth
	 */
	public static final Type GYM = new Type("gYearMonth", ANA, true, GYM_CODE,
			true, false, false, true, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:gYear
	 */
	public static final Type GYE = new Type("gYear", ANA, true, GYE_CODE, true,
			false, false, true, new Facets(-1, -1, -1, null, null, WS.COLLAPSE,
					null, null, null, null, -1, -1));

	/**
	 * xs:gMonthDay
	 */
	public static final Type GMD = new Type("gMonthDay", ANA, true, GMD_CODE,
			true, false, false, true, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:gDay
	 */
	public static final Type GDAY = new Type("gDay", ANA, true, GDAY_CODE,
			true, false, false, true, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:gMonth
	 */
	public static final Type GMON = new Type("gMonth", ANA, true, GMON_CODE,
			true, false, false, true, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:boolean
	 */
	public static final Type BOOL = new Type("boolean", ANA, true, BOOL_CODE,
			true, false, false, false, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:base64Binary
	 */
	public static final Type B64 = new Type("base64Binary", ANA, true,
			B64_CODE, true, false, false, false, new Facets(-1, -1, -1, null,
					null, WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:hexBinary
	 */
	public static final Type HEX = new Type("hexBinary", ANA, true, HEX_CODE,
			true, false, false, false, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:anyURI
	 */
	public static final Type AURI = new Type("anyURI", ANA, true, STRING_CODE,
			true, false, false, false, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:QName
	 */
	public static final Type QNM = new Type("QName", ANA, true, QNM_CODE, true,
			false, false, false, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	/**
	 * xs:NOTATION
	 */
	public static final Type NOT = new Type("NOTATION", ANA, true, NOT_CODE,
			true, false, false, false, new Facets(-1, -1, -1, null, null,
					WS.COLLAPSE, null, null, null, null, -1, -1));

	public static final Type[] builtInTypes;

	static {
		builtInTypes = new Type[] { ANY, UN, ANS, IDRS, NMT, ENTS, ANA, UNA,
				DATI, DATE, TIME, DUR, YMD, DTD, FLO, DBL, DEC, INR, NPI, NINR,
				LON, INT, SHO, BYT, NNI, ULON, UINT, USHO, UBYT, PINR, STR,
				NSTR, TOK, LAN, NMTS, NAM, NCN, ID, IDR, ENT, GYM, GYE, GMD, GDAY,
				GMON, BOOL, B64, HEX, AURI, QNM, NOT };
	}

	private final QNm name;

	private final Type parent;

	private final Type primitiveBase;

	private final boolean builtIn;

	private final boolean castPrimitive;

	private final boolean atomic;

	private final int atomicCode;

	private final boolean numeric;

	private final boolean duration;

	private final boolean timeInstance;

	private final Facets facets;

	public Type(QNm name, Type parent, Facets facets) {
		this.name = name;
		this.parent = parent;
		this.builtIn = false;
		this.castPrimitive = false;
		this.atomic = instanceOf(ANA);
		this.numeric = parent.numeric;
		this.duration = parent.duration;
		this.timeInstance = parent.timeInstance;
		if (atomic) {
			Type t = this;
			while ((t != null) && (!t.castPrimitive))
				t = t.parent;
			primitiveBase = t;
			atomicCode = primitiveBase.atomicCode;
		} else {
			primitiveBase = null;
			atomicCode = -1;
		}
		this.facets = (facets != null) ? facets : (parent != null) ? parent
				.getFacets() : null;
	}

	private Type(String name, Type parent, boolean builtIn, int atomicCode,
			boolean castPrimitive, boolean numeric, boolean duration,
			boolean timeInstant) {
		this(name, parent, builtIn, atomicCode, castPrimitive, numeric,
				duration, timeInstant, null);
	}

	private Type(String name, Type parent, boolean builtIn, int atomicCode,
			boolean castPrimitive, boolean numeric, boolean duration,
			boolean timeInstant, Facets facets) {
		this.name = new QNm(Namespaces.XS_NSURI, Namespaces.XS_PREFIX, name);
		this.parent = parent;
		this.builtIn = builtIn;
		this.castPrimitive = castPrimitive;
		this.atomic = (atomicCode >= 0);
		this.numeric = numeric;
		this.duration = duration;
		this.timeInstance = timeInstant;
		this.atomicCode = atomicCode;
		if (atomic) {
			Type t = this;
			while ((t != null) && (!t.castPrimitive))
				t = t.parent;
			primitiveBase = t;
		} else {
			primitiveBase = null;
		}
		this.facets = (facets != null) ? facets : (parent != null) ? parent
				.getFacets() : null;
	}

	public boolean instanceOf(Type type) {
		Type t = this;
		do {
			if (t == type)
				return true;
		} while ((t = t.parent) != null);
		return false;
	}

	public QNm getName() {
		return name;
	}

	public Type getParent() {
		return parent;
	}

	public boolean isBuiltin() {
		return builtIn;
	}

	public boolean isCastPrimitive() {
		return castPrimitive;
	}

	public boolean isAtomic() {
		return atomic;
	}

	public String toString() {
		return name.toString();
	}

	public boolean isNumeric() {
		return numeric;
	}

	public boolean isDuration() {
		return duration;
	}

	public boolean isTimeInstance() {
		return timeInstance;
	}

	public Type getPrimitiveBase() {
		return primitiveBase;
	}

	public int atomicCode() {
		return atomicCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Type)) {
			return false;
		}
		Type other = (Type) obj;

		return other.name.equals(name);
	}

	public Facets getFacets() {
		return facets;
	}
}