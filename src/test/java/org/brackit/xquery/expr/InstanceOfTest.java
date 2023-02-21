package org.brackit.xquery.expr;

import org.brackit.xquery.ResultChecker;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.XQueryBaseTest;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.jdm.Sequence;
import org.junit.Test;

public final class InstanceOfTest extends XQueryBaseTest {
  @Test
  public void elementInstanceOfElement() {
    Sequence result = new XQuery("<element/> instance of element()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void objectInstanceOfObject() {
    Sequence result = new XQuery("{\"a\":1} instance of object()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyArrayInstanceOfArray() {
    Sequence result = new XQuery("[] instance of array()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyArrayInstanceOfStructuredItem() {
    Sequence result = new XQuery("[] instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyDocumentInstanceOfDocumentNode1() {
    Sequence result = new XQuery("document {()} instance of document-node()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyDocumentInstanceOfDocumentNode2() {
    Sequence result = new XQuery("document {} instance of document-node()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyRecordInstanceOfStructuredItem() {
    Sequence result = new XQuery("{} instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyElementInstanceOfStructuredItem() {
    Sequence result = new XQuery("<element/> instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void attributeInstanceOfStructuredItem() {
    Sequence result = new XQuery("attribute foo {\"bar\"} instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void commentInstanceOfStructuredItem() {
    Sequence result = new XQuery("<!-- foobar --> instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void processingInstructionInstanceOfStructuredItem() {
    Sequence result = new XQuery("<?PITarget PIContent?> instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void textNodeInstanceOfStructuredItem() {
    Sequence result = new XQuery("text {\"Foo\"} instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void stringInstanceOfStructuredItem() {
    Sequence result = new XQuery("'Foo' instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void numberInstanceOfStructuredItem() {
    Sequence result = new XQuery("1 instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void dateInstanceOfStructuredItem() {
    Sequence result = new XQuery("xs:dateTime(\"2018-02-01T00:00:00\") instance of structured-item()").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void emptyArrayInstanceOfJsonItem() {
    Sequence result = new XQuery("[] instance of json-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyRecordInstanceOfJsonItem() {
    Sequence result = new XQuery("{} instance of json-item()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void emptyElementInstanceOfJsonItem() {
    Sequence result = new XQuery("<element/> instance of json-item()").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void arrayInstanceOfArray() {
    Sequence result = new XQuery("[\"foobar\"] instance of array()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void textInstanceOfElement() {
    Sequence result = new XQuery("'Foo' instance of element()").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void stringInstanceOfDecimal() throws Exception {
    Sequence result = new XQuery("'Foo' instance of xs:decimal").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }
}
