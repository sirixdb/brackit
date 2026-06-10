package io.brackit.query;

import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression tests for the pass-4 brackit audit: quantifier-over-empty, missing-field delete,
 * fn:error semantics, xs:integer from large doubles / NaN casts, instance-of empty-sequence,
 * array-slice step + view bounds, regex error mapping, and castable-QName.
 */
public class PassFourCorrectnessTest extends XQueryBaseTest {

  private String eval(String query) {
    var out = new java.io.ByteArrayOutputStream();
    new Query(query).serialize(ctx, new java.io.PrintStream(out));
    return out.toString().trim();
  }

  // B1: `every` over the empty sequence is vacuously true.
  @Test
  public void everyOverEmptyIsTrue() {
    assertEquals("true", eval("every $x in () satisfies false()"));
    assertEquals("true", eval("every $x in () satisfies $x gt 100"));
    assertEquals("false", eval("some $x in () satisfies true()")); // some stays false
  }

  // B3: ArrayObject.remove(QNm) on a MISSING field is a no-op (it deleted the first field before
  // the fix). Tested at the API level — a missing-field deref is empty at query level, so the
  // (DeleteRecordFieldOp -> remove) path isn't reachable from a simple query.
  @Test
  public void removeMissingFieldIsNoOp() {
    var fields = new io.brackit.query.atomic.QNm[] { new io.brackit.query.atomic.QNm("a"),
        new io.brackit.query.atomic.QNm("b") };
    var vals = new io.brackit.query.jdm.Sequence[] { new Int32(1), new Int32(2) };
    var obj = new io.brackit.query.jsonitem.object.ArrayObject(fields, vals);
    obj.remove(new io.brackit.query.atomic.QNm("zzz")); // missing — must NOT drop "a"
    assertEquals(2, obj.len());
    assertEquals("a", obj.name(0).stringValue());
    assertEquals("b", obj.name(1).stringValue());
    obj.remove(new io.brackit.query.atomic.QNm("a")); // present — drops it
    assertEquals(1, obj.len());
    assertEquals("b", obj.name(0).stringValue());
  }

  // B4: fn:error keeps the description verbatim (no String.format crash) and carries the value.
  @Test
  public void fnErrorDescriptionAndValue() {
    // a '%' in the description must not crash with UnknownFormatConversionException
    assertEquals("caught:50% failed",
                 eval("try { fn:error(xs:QName('err:FOER0000'), '50% failed') } catch * { concat('caught:', $err:description) }"));
    // $err:value carries the 3rd argument
    assertEquals("42", eval("try { fn:error(xs:QName('err:FOER0000'), 'd', 42) } catch * { $err:value }"));
  }

  // B5: xs:integer from a large double does NOT saturate at Long.MAX; NaN casts are handled.
  @Test
  public void integerFromLargeDoubleAndNaNCasts() {
    assertEquals("1000000000000000019884624838656", eval("xs:integer(xs:double('1e30'))"));
    assertEquals("false", eval("string(xs:double('NaN') cast as xs:boolean)"));
  }

  // B6: a single item is not an instance of empty-sequence(); ? and * still accept it.
  @Test
  public void instanceOfEmptySequence() {
    assertEquals("false", eval("1 instance of empty-sequence()"));
    assertEquals("true", eval("() instance of empty-sequence()"));
    assertEquals("true", eval("1 instance of item()?"));
    assertEquals("true", eval("1 instance of item()*"));
  }

  // B8: array-slice step counts from the lower bound.
  @Test
  public void arraySliceStep() {
    // $a[1:5:2] keeps the lower bound and every 2nd member from there → members at index 1,3 = 2,4
    assertEquals("[2,4]", eval("let $a := [1,2,3,4,5] return $a[1:5:2]"));
  }

  // B9: invalid regex / replacement are mapped to FORX errors, not raw Java exceptions.
  @Test
  public void regexErrorMapping() {
    QueryException tok = assertThrows(QueryException.class, () -> new Query("fn:tokenize('a','[')").execute(ctx));
    assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, tok.getCode());
    QueryException rep = assertThrows(QueryException.class, () -> new Query("fn:replace('abc','b','x$')").execute(ctx));
    assertEquals(ErrorCode.ERR_INVALID_REPLACEMENT_STRING, rep.getCode());
  }

  // B10: `castable as xs:QName` with an unbound prefix is false, not an error.
  @Test
  public void castableQNameUnboundPrefix() {
    assertEquals("false", eval("'nosuchprefix:y' castable as xs:QName"));
  }
}
