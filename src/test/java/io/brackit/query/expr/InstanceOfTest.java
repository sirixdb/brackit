package io.brackit.query.expr;

import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Bool;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.ResultChecker;
import io.brackit.query.Query;
import org.junit.Test;

public final class InstanceOfTest extends XQueryBaseTest {
  @Test
  public void elementInstanceOfElement() {
    Sequence result = new Query("<element/> instance of element()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void objectInstanceOfObject() {
    Sequence result = new Query("{\"a\":1} instance of object()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyArrayInstanceOfArray() {
    Sequence result = new Query("[] instance of array()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyArrayInstanceOfStructuredItem() {
    Sequence result = new Query("[] instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyDocumentInstanceOfDocumentNode1() {
    Sequence result = new Query("document {()} instance of document-node()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyDocumentInstanceOfDocumentNode2() {
    Sequence result = new Query("document {} instance of document-node()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyRecordInstanceOfStructuredItem() {
    Sequence result = new Query("{} instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyElementInstanceOfStructuredItem() {
    Sequence result = new Query("<element/> instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void attributeInstanceOfStructuredItem() {
    Sequence result = new Query("attribute foo {\"bar\"} instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void commentInstanceOfStructuredItem() {
    Sequence result = new Query("<!-- foobar --> instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void processingInstructionInstanceOfStructuredItem() {
    Sequence result = new Query("<?PITarget PIContent?> instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void textNodeInstanceOfStructuredItem() {
    Sequence result = new Query("text {\"Foo\"} instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void stringInstanceOfStructuredItem() {
    Sequence result = new Query("'Foo' instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void numberInstanceOfStructuredItem() {
    Sequence result = new Query("1 instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void dateInstanceOfStructuredItem() {
    Sequence result = new Query("xs:dateTime(\"2018-02-01T00:00:00\") instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void emptyArrayInstanceOfJsonItem() {
    Sequence result = new Query("[] instance of json-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyRecordInstanceOfJsonItem() {
    Sequence result = new Query("{} instance of json-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyElementInstanceOfJsonItem() {
    Sequence result = new Query("<element/> instance of json-item()").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void arrayInstanceOfArray() {
    Sequence result = new Query("[\"foobar\"] instance of array()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void textInstanceOfElement() {
    Sequence result = new Query("'Foo' instance of element()").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void stringInstanceOfDecimal() throws Exception {
    Sequence result = new Query("'Foo' instance of xs:decimal").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }
}
