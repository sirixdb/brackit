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

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.B64;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.DTD;
import org.brackit.xquery.atomic.Date;
import org.brackit.xquery.atomic.DateTime;
import org.brackit.xquery.atomic.Dbl;
import org.brackit.xquery.atomic.Dec;
import org.brackit.xquery.atomic.Dur;
import org.brackit.xquery.atomic.Flt;
import org.brackit.xquery.atomic.GDay;
import org.brackit.xquery.atomic.GMD;
import org.brackit.xquery.atomic.GMon;
import org.brackit.xquery.atomic.GYE;
import org.brackit.xquery.atomic.GYM;
import org.brackit.xquery.atomic.Hex;
import org.brackit.xquery.atomic.Int;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.Int64;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.atomic.Numeric;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.atomic.Time;
import org.brackit.xquery.atomic.TimeInstant;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.atomic.YMD;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;

/**
 * @author Sebastian Baechle
 */
public class Cast implements Expr {
  private final StaticContext sctx;
  private final Expr expr;
  private final Type target;
  private final boolean allowEmptySequence;

  public Cast(StaticContext sctx, Expr expr, Type targetType, boolean allowEmptySequence) {
    this.sctx = sctx;
    this.expr = expr;
    this.target = targetType;
    this.allowEmptySequence = allowEmptySequence;
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    Item item = expr.evaluateToItem(ctx, tuple);
    return cast(sctx, item, target, allowEmptySequence);
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    return evaluateToItem(ctx, tuple);
  }

  public static Atomic cast(StaticContext sctx, Item item, Type target, boolean allowEmptySequence) {
    // See XQuery 1.0: 3.12.3 Cast
    if (item == null) {
      if (allowEmptySequence) {
        return null;
      } else {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
      }
    }
    if (!target.isAtomic()) {
      throw new QueryException(ErrorCode.ERR_UNKNOWN_ATOMIC_SCHEMA_TYPE, "Cannot cast to non-atomic type %s", target);
    }

    Atomic atomic = item.atomize();
    Type source = atomic.type();

    if ((source == target) || (target == Type.ANA)) {
      // identity cast
      // (diagonal in casting matrix)
      return atomic;
    }

    if (target.isCastPrimitive()) {
      return toPrimitive(sctx, atomic, source, target);
    } else {
      return toDerived(sctx, atomic, source, target);
    }
  }

  public static Atomic cast(StaticContext sctx, Atomic atomic, Type target) {
    // See XQuery 1.0: 3.12.3 Cast
    if (!target.isAtomic()) {
      throw new QueryException(ErrorCode.ERR_UNKNOWN_ATOMIC_SCHEMA_TYPE, "Cannot cast to non-atomic type %s", target);
    }
    Type source = atomic.type();

    if ((source == target) || (target == Type.ANA)) {
      // identity cast
      // (diagonal in casting matrix)
      return atomic;
    }

    if (target.isCastPrimitive()) {
      return toPrimitive(sctx, atomic, source, target);
    } else {
      return toDerived(sctx, atomic, source, target);
    }
  }

  private static Atomic toDerived(StaticContext sctx, Atomic atomic, Type source, Type target) {
    if (source.instanceOf(target)) {
      // source is a real subtype of target
      return atomic.asType(target);
    }

    Type sourceBase = source.getPrimitiveBase();
    Type targetBase = target.getPrimitiveBase();

    if (sourceBase != targetBase) {
      // source and target do not have the same
      // primitive base type
      // See XQuery 1.0: 17.5 Casting across the type hierarchy
      Atomic temp = toPrimitive(sctx, atomic, source, sourceBase);

      if ((sourceBase == Type.STR) || (sourceBase == Type.UNA)) {
        // TODO check upcasted atomic
        // against the pattern facets of target
      }

      atomic = castPrimitiveToPrimitive(sctx, temp, sourceBase, targetBase);
    }

    // See XQuery 1.0: 17.4 Casting within a branch of the type hierarchy
    // Perform the actual downcast

    // TODO check atomic against all facets of target

    // Return a properly typed copy of the base type
    return atomic.asType(target);
  }

  private static Atomic toPrimitive(StaticContext sctx, Atomic atomic, Type source, Type target) {
    if (source.isCastPrimitive()) {
      return castPrimitiveToPrimitive(sctx, atomic, source, target);
    } else if (source.instanceOf(target)) {
      // source is a real subtype of target
      return atomic.asType(target);
    } else {
      // See XQuery 1.0: 17.5 Casting across the type hierarchy
      Type sourceBase = source.getPrimitiveBase();

      if (sourceBase == null) {
        throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                                 "Tried to cast %s to %s but did not find primitive base type",
                                 source,
                                 target);
      }

      Atomic temp = toPrimitive(sctx, atomic, source, sourceBase);
      // target is primitive and has not pattern facets.
      // No need to check any
      return castPrimitiveToPrimitive(sctx, temp, sourceBase, target);
    }
  }

  /*
   * // See XQuery 1.0: 17.1 Casting from primitive types to primitive types
   */
  private static Atomic castPrimitiveToPrimitive(StaticContext sctx, Atomic atomic, Type source, Type target) {
    // Compare with columns in cast
    if (target == source) {
      return atomic;
    } else if (target == Type.UNA) {
      return new Una(atomic.stringValue());
    } else if (target == Type.STR) {
      return new Str(atomic.stringValue());
    } else if (target.isNumeric()) {
      // use numeric indicator to short-circuit
      // if-else cascade
      if (target == Type.DBL) {
        return primitiveToDbl(atomic, source, target);
      } else if (target == Type.INR) {
        return primitiveToInt(atomic, source, target);
      } else if (target == Type.DEC) {
        return primitiveToDec(atomic, source, target);
      } else if (target == Type.FLO) {
        return primitiveToFlt(atomic, source, target);
      } else {
        throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                                 "Unexpected numeric primitive target type: %s",
                                 target);
      }
    } else if (target.isDuration()) {
      // use duration indicator to
      // short-circuit if-else cascade
      if (target == Type.DUR) {
        return primitiveToDur(atomic, source, target);
      } else if (target == Type.DTD) {
        return primitiveToDTDur(atomic, source, target);
      } else if (target == Type.YMD) {
        return primitiveToYMDur(atomic, source, target);
      } else {
        throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                                 "Unexpected primitive duration target type: %s",
                                 target);
      }
    } else if (target.isTimeInstance()) {
      // use dateTime indicator to
      // short-circuit if-else cascade
      if (target == Type.DATI) {
        return primitiveToDateTime(atomic, source, target);
      } else if (target == Type.DATE) {
        return primitiveToDate(atomic, source, target);
      } else if (target == Type.TIME) {
        return primitiveToTime(atomic, source, target);
      }
      if (target == Type.GDAY) {
        return primitiveToGDay(atomic, source, target);
      } else if (target == Type.GMD) {
        return primitiveToGMD(atomic, source, target);
      } else if (target == Type.GMON) {
        return primitiveToGMon(atomic, source, target);
      } else if (target == Type.GYE) {
        return primitiveToGYE(atomic, source, target);
      } else if (target == Type.GYM) {
        return primitiveToGYM(atomic, source, target);
      } else {
        throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                                 "Unexpected primitive date/time target type: %s",
                                 target);
      }
    } else if (target == Type.BOOL) {
      return primitiveToBool(atomic, source, target);
    } else if (target == Type.B64) {
      return primitiveToB64(atomic, source, target);
    } else if (target == Type.HEX) {
      return primitiveToHex(atomic, source, target);
    } else if (target == Type.AURI) {
      return primitiveToAnyURI(atomic, source, target);
    } else if (target == Type.QNM) {
      // Implementation note:
      // According to
      // "XQuery 1.0: 17.1.1 Casting from xs:string and xs:untypedAtomic"
      // it is legal to cast an xs:string _literal_ to a xs:QName. This is
      // not reflected here
      // and must be reflected by a direct compilation to an xs:QName
      // literal.
      if (source.instanceOf(Type.QNM)) {
        return atomic;
      } else if (source.instanceOf(Type.NOT)) {
        return new QNm(atomic.stringValue());
      } else {
        QNm qnm = new QNm(atomic.stringValue());
        if (qnm.getPrefix() != null) {
          String uri = sctx.getNamespaces().resolve(qnm.getPrefix());
          if (uri == null) {
            throw new QueryException(ErrorCode.ERR_UNKNOWN_NS_PREFIX_IN_COMP_CONSTR,
                                     "Statically unkown namespace prefix: '%s'",
                                     qnm.getPrefix());
          }
          return new QNm(uri, null, qnm.getLocalName());
        } else {
          String uri = sctx.getNamespaces().getDefaultElementNamespace();
          if ((uri == null) || (uri.isEmpty())) {
            return qnm;
          } else {
            return new QNm(uri, null, qnm.getLocalName());
          }
        }
      }
    } else if ((target == Type.NOT) || (target == Type.ANA)) {
      throw new QueryException(ErrorCode.ERR_ILLEGAL_CAST_TARGET_TYPE, "Cast to %s is not allowed", target);
    } else {
      throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                               "Unexpected primitive target type: %s",
                               target);
    }
  }

  private static Atomic primitiveToGDay(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new GDay(atomic.stringValue());
    }
    if ((source == Type.DATI) || (source == Type.DATE)) {
      TimeInstant ti = (TimeInstant) atomic;
      return new GDay(ti.getDay(), ti.getTimezone());
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToGMD(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new GMD(atomic.stringValue());
    }
    if ((source == Type.DATI) || (source == Type.DATE)) {
      TimeInstant ti = (TimeInstant) atomic;
      return new GMD(ti.getMonth(), ti.getDay(), ti.getTimezone());
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToGMon(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new GMon(atomic.stringValue());
    }
    if ((source == Type.DATI) || (source == Type.DATE)) {
      TimeInstant ti = (TimeInstant) atomic;
      return new GMon(ti.getMonth(), ti.getTimezone());
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToGYM(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new GYM(atomic.stringValue());
    }
    if ((source == Type.DATI) || (source == Type.DATE)) {
      TimeInstant ti = (TimeInstant) atomic;
      return new GYM(ti.getYear(), ti.getMonth(), ti.getTimezone());
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToGYE(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new GYE(atomic.stringValue());
    }
    if ((source == Type.DATI) || (source == Type.DATE)) {
      TimeInstant ti = (TimeInstant) atomic;
      return new GYE(ti.getYear(), ti.getTimezone());
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToDateTime(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new DateTime(atomic.stringValue());
    }
    if (source == Type.DATE) {
      Date d = (Date) atomic;
      return new DateTime(d.getYear(), d.getMonth(), d.getDay(), (byte) 0, (byte) 0, (byte) 0, d.getTimezone());
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToDate(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new Date(atomic.stringValue());
    }
    if (source == Type.DATI) {
      return new Date((DateTime) atomic);
    }

    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToTime(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new Time(atomic.stringValue());
    }
    if (source == Type.DATI) {
      return new Time((DateTime) atomic);
    }

    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToDur(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new Dur(atomic.stringValue());
    } else if (source == Type.YMD) {
      YMD ymd = (YMD) atomic;
      return new Dur(ymd.isNegative(), ymd.getYears(), ymd.getMonths(), (short) 0, (byte) 0, (byte) 0, 0);
    } else if (source == Type.DTD) {
      DTD dtd = (DTD) atomic;
      return new Dur(dtd.isNegative(),
                     (short) 0,
                     (byte) 0,
                     dtd.getDays(),
                     dtd.getHours(),
                     dtd.getMinutes(),
                     dtd.getMicros());
    }

    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToYMDur(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new YMD(atomic.stringValue());
    } else if (source == Type.DUR) {
      Dur dur = (Dur) atomic;
      return new YMD(dur.isNegative(), dur.getYears(), dur.getMonths());
    } else if (source == Type.DTD) {
      DTD dtd = (DTD) atomic;
      return new YMD(dtd.isNegative(), (short) 0, (byte) 0);
    }

    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToDTDur(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new DTD(atomic.stringValue());
    }
    if (source == Type.DUR) {
      Dur dur = (Dur) atomic;
      return new DTD(dur.isNegative(), dur.getDays(), dur.getHours(), dur.getMinutes(), dur.getMicros());
    } else if (source == Type.YMD) {
      YMD ymd = (YMD) atomic;
      return new DTD(ymd.isNegative(), (short) 0, (byte) 0, (byte) 0, 0);
    }

    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToDbl(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR) || (source == Type.DEC) || (source == Type.INR)) {
      return Dbl.parse(atomic.stringValue());
    }
    if (source == Type.FLO) {
      return new Dbl(((Numeric) atomic).floatValue());
    }
    if (source == Type.BOOL) {
      return new Dbl(((Bool) atomic).bool ? 1d : 0d);
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToFlt(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR) || (source == Type.DEC) || (source == Type.INR)) {
      return Flt.parse(atomic.stringValue());
    }
    if (source == Type.DBL) {
      return new Flt(((Numeric) atomic).floatValue());
    }
    if (source == Type.BOOL) {
      return new Flt(((Bool) atomic).bool ? 1f : 0f);
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToDec(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new Dec(atomic.stringValue());
    }
    if (source == Type.DBL) {
      double dv = ((Numeric) atomic).doubleValue();
      if ((Double.isNaN(dv)) || (Double.isInfinite(dv))) {
        throw new QueryException(ErrorCode.ERR_INVALID_LEXICAL_VALUE);
      }
      return new Dec(new BigDecimal(dv));
    }
    if (source == Type.FLO) {
      float fv = ((Numeric) atomic).floatValue();
      if ((Float.isNaN(fv)) || (Float.isInfinite(fv))) {
        throw new QueryException(ErrorCode.ERR_INVALID_LEXICAL_VALUE);
      }
      return new Dec(new BigDecimal(fv));
    }
    if (source == Type.INR) {
      return atomic.asType(Type.DEC);
    }
    if (source == Type.BOOL) {
      return new Dec(((Bool) atomic).bool ? BigDecimal.ONE : BigDecimal.ZERO);
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToInt(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return Int32.parse(atomic.stringValue());
    }
    if ((source == Type.DBL) || (source == Type.FLO)) {
      double d = ((Numeric) atomic).doubleValue();
      if ((Double.isNaN(d)) || (Double.isInfinite(d))) {
        throw new QueryException(ErrorCode.ERR_INVALID_LEXICAL_VALUE);
      }
      return asInteger(d);
    }
    if (source == Type.DEC) {
      return asInt(((Numeric) atomic).decimalValue());
    }
    if (source == Type.BOOL) {
      return (((Bool) atomic).bool) ? Int32.ONE : Int32.ZERO;
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToBool(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      String s = Whitespace.collapseTrimOnly(atomic.stringValue());
      if (("true".equals(s)) || ("1".equals(s))) {
        return Bool.TRUE;
      } else if (("false".equals(s)) || ("0".equals(s))) {
        return Bool.FALSE;
      } else {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Illegal cast from %s to %s", source, target);
      }
    }
    if (source.isNumeric()) {
      double d = ((Numeric) atomic).doubleValue();
      return new Bool(((d == Double.NaN) || (d == 0)) ? false : true);
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToAnyURI(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new AnyURI(atomic.stringValue());
    }

    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToHex(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new Hex(atomic.stringValue());
    }
    if (source == Type.B64) {
      return new Hex(((B64) atomic).getBytes());
    }

    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  private static Atomic primitiveToB64(Atomic atomic, Type source, Type target) {
    if ((source == Type.UNA) || (source == Type.STR)) {
      return new B64(atomic.stringValue());
    }
    if (source == Type.HEX) {
      return new B64(((Hex) atomic).getBytes());
    }

    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Illegal cast from %s to %s", source, target);
  }

  public static IntNumeric asInt(BigDecimal d) {
    if (d.scale() != 0) {
      d = d.setScale(0, RoundingMode.DOWN);
    }
    if ((d.compareTo(Int32.MAX_VALUE_AS_DECIMAL) <= 0) && (d.compareTo(Int32.MIN_VALUE_AS_DECIMAL) >= 0)) {
      return new Int32(d.intValue());
    } else if ((d.compareTo(Int64.MAX_VALUE_AS_DECIMAL) <= 0) && (d.compareTo(Int64.MIN_VALUE_AS_DECIMAL) >= 0)) {
      return new Int64(d.longValue());
    } else {
      return new Int(d);
    }
  }

  public static IntNumeric asInteger(double d) {
    if ((d == Double.NaN) || (d == Double.POSITIVE_INFINITY) || (d == Double.NEGATIVE_INFINITY)) {
      throw new QueryException(ErrorCode.ERR_INVALID_LEXICAL_VALUE);
    }
    if ((d <= Integer.MAX_VALUE) && (d >= Integer.MIN_VALUE)) {
      return new Int32((int) d);
    }
    if ((d <= Long.MAX_VALUE) || (d >= Long.MIN_VALUE)) {
      return new Int64((long) d);
    }
    return new Int(d);
  }

  @Override
  public boolean isUpdating() {
    return (expr.isUpdating());
  }

  @Override
  public boolean isVacuous() {
    return false;
  }
}
