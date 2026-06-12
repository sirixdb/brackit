package io.brackit.query.function.fn;

import io.brackit.query.Query;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the QName component accessors fn:local-name-from-QName, fn:namespace-uri-from-QName,
 * and fn:prefix-from-QName (XQuery F&amp;O 11), which were previously not implemented (XPST0017).
 */
public class QNameComponentTest extends XQueryBaseTest {

  @Test
  public void localNameFromQName() {
    ResultChecker.dCheck(Bool.TRUE,
                         new Query("local-name-from-QName(fn:QName('urn:x', 'p:foo')) eq xs:NCName('foo')").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE,
                         new Query("local-name-from-QName(fn:QName('urn:x', 'p:foo')) instance of xs:NCName").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE,
                         new Query("local-name-from-QName(xs:QName('bar')) eq xs:NCName('bar')").execute(ctx));
  }

  @Test
  public void namespaceUriFromQName() {
    ResultChecker.dCheck(Bool.TRUE,
                         new Query("string(namespace-uri-from-QName(fn:QName('urn:x', 'p:foo'))) eq 'urn:x'").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE,
                         new Query("namespace-uri-from-QName(fn:QName('urn:x', 'p:foo')) instance of xs:anyURI").execute(ctx));
    // A QName in no namespace yields the zero-length xs:anyURI, not the empty sequence.
    ResultChecker.dCheck(Bool.TRUE,
                         new Query("string(namespace-uri-from-QName(fn:QName('', 'foo'))) eq ''").execute(ctx));
    ResultChecker.dCheck(new Int32(1), new Query("count(namespace-uri-from-QName(fn:QName('', 'foo')))").execute(ctx));
  }

  @Test
  public void prefixFromQName() {
    ResultChecker.dCheck(Bool.TRUE,
                         new Query("prefix-from-QName(fn:QName('urn:x', 'p:foo')) eq xs:NCName('p')").execute(ctx));
    // No prefix: empty sequence.
    ResultChecker.dCheck(new Int32(0), new Query("count(prefix-from-QName(fn:QName('urn:x', 'foo')))").execute(ctx));
  }

  @Test
  public void emptySequenceArgumentYieldsEmpty() {
    ResultChecker.dCheck(new Int32(0), new Query("count(local-name-from-QName(()))").execute(ctx));
    ResultChecker.dCheck(new Int32(0), new Query("count(namespace-uri-from-QName(()))").execute(ctx));
    ResultChecker.dCheck(new Int32(0), new Query("count(prefix-from-QName(()))").execute(ctx));
  }

  @Test
  public void nonQNameArgumentRejected() {
    assertThrows(QueryException.class, () -> new Query("local-name-from-QName('notaqname')").execute(ctx));
  }
}
