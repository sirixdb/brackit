package io.brackit.query.expr;

import java.util.ArrayList;
import java.util.List;

import io.brackit.query.Query;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DirectElementNamespaceTest extends XQueryBaseTest {

  private void check(String query, String... expected) {
    List<String> actual = new ArrayList<>();
    try (Iter items = new Query(query).execute(ctx).iterate()) {
      Item item;
      while ((item = items.next()) != null) {
        actual.add(((Atomic) item).stringValue());
      }
    }
    assertEquals(List.of(expected), actual);
  }

  private void checkElements(String constructor, String... expected) {
    checkElementNames("", constructor, expected);
  }

  private void checkElementNames(String prolog, String constructor, String... expected) {
    check(prolog + "let $roots := (" + constructor + ") for $e in $roots/descendant-or-self::* return "
        + "(string(namespace-uri($e)), string(prefix-from-QName(node-name($e))), local-name($e))", expected);
  }

  @Test
  public void defaultNamespace() {
    checkElements("<root xmlns='urn:default'/>", "urn:default", "", "root");
    check("namespace-uri-from-QName(resolve-QName('probe', <root xmlns='urn:default'/>))", "urn:default");
  }

  @Test
  public void prefixedNamespace() {
    checkElements("<p:root xmlns:p='urn:prefixed'/>", "urn:prefixed", "p", "root");
    check("namespace-uri-from-QName(resolve-QName('p:probe', <p:root xmlns:p='urn:prefixed'/>))", "urn:prefixed");
  }

  @Test
  public void defaultNamespaceScopesOverrideInheritAndRestore() {
    String constructor = "<root xmlns='urn:outer'><inherited/><inner xmlns='urn:inner'><leaf/></inner>"
        + "<reset xmlns=''><plain/></reset><sibling/></root>";
    checkElements(constructor,
                  "urn:outer",
                  "",
                  "root",
                  "urn:outer",
                  "",
                  "inherited",
                  "urn:inner",
                  "",
                  "inner",
                  "urn:inner",
                  "",
                  "leaf",
                  "",
                  "",
                  "reset",
                  "",
                  "",
                  "plain",
                  "urn:outer",
                  "",
                  "sibling");
    check("let $root := " + constructor + " for $e in $root/descendant-or-self::* return "
        + "string(namespace-uri-from-QName(resolve-QName('probe', $e)))",
          "urn:outer",
          "urn:outer",
          "urn:inner",
          "urn:inner",
          "",
          "",
          "urn:outer");
  }

  @Test
  public void prefixedNamespaceScopesOverrideInheritAndRestore() {
    String constructor = "<p:root xmlns:p='urn:outer'><p:inherited/>"
        + "<p:inner xmlns:p='urn:inner'><p:leaf/></p:inner><p:sibling/></p:root>";
    checkElements(constructor,
                  "urn:outer",
                  "p",
                  "root",
                  "urn:outer",
                  "p",
                  "inherited",
                  "urn:inner",
                  "p",
                  "inner",
                  "urn:inner",
                  "p",
                  "leaf",
                  "urn:outer",
                  "p",
                  "sibling");
    check("let $root := " + constructor + " for $e in $root/descendant-or-self::* return "
        + "namespace-uri-from-QName(resolve-QName('p:probe', $e))",
          "urn:outer",
          "urn:outer",
          "urn:inner",
          "urn:inner",
          "urn:outer");
  }

  @Test
  public void siblingDeclarationsDoNotLeak() {
    checkElements("<root><item xmlns='urn:a'>a</item><item xmlns='urn:b'>b</item>"
        + "<p:item xmlns:p='urn:a'>alias</p:item><item>plain</item></root>",
                  "",
                  "",
                  "root",
                  "urn:a",
                  "",
                  "item",
                  "urn:b",
                  "",
                  "item",
                  "urn:a",
                  "p",
                  "item",
                  "",
                  "",
                  "item");
  }

  @Test
  public void severalDeclarationsPreserveNamesAndBindings() {
    String constructor = "<p:root xmlns='urn:default' xmlns:p='urn:p' xmlns:q='urn:q'><child/><q:child/></p:root>";
    checkElements(constructor, "urn:p", "p", "root", "urn:default", "", "child", "urn:q", "q", "child");
    check("let $root := " + constructor + " for $e in $root/descendant-or-self::* return "
        + "(namespace-uri-from-QName(resolve-QName('probe', $e)), "
        + "namespace-uri-from-QName(resolve-QName('p:probe', $e)), "
        + "namespace-uri-from-QName(resolve-QName('q:probe', $e)))",
          "urn:default",
          "urn:p",
          "urn:q",
          "urn:default",
          "urn:p",
          "urn:q",
          "urn:default",
          "urn:p",
          "urn:q");
  }

  @Test
  public void directAndComputedConstructorsAgree() {
    checkElementNames("declare default element namespace 'urn:default'; declare namespace p='urn:p'; ",
                      "(<root xmlns='urn:default'/>, element root {}, element { QName('urn:default', 'root') } {}, "
                          + "<p:root xmlns:p='urn:p'/>, element p:root {}, element { QName('urn:p', 'p:root') } {})",
                      "urn:default",
                      "",
                      "root",
                      "urn:default",
                      "",
                      "root",
                      "urn:default",
                      "",
                      "root",
                      "urn:p",
                      "p",
                      "root",
                      "urn:p",
                      "p",
                      "root",
                      "urn:p",
                      "p",
                      "root");
  }

  @Test
  public void directNamespacesInsideComputedConstructors() {
    checkElementNames("declare namespace p='urn:outer'; ",
                      "document { element p:root { <p:inner xmlns:p='urn:inner'><p:leaf/></p:inner> } }",
                      "urn:outer",
                      "p",
                      "root",
                      "urn:inner",
                      "p",
                      "inner",
                      "urn:inner",
                      "p",
                      "leaf");
  }

  @Test
  public void attributeNamespacesRemainIndependentOfDefaultNamespace() {
    check("let $root := <root xmlns='urn:default' xmlns:p='urn:p' p:attr='value' plain='value'/> "
        + "for $a in $root/@* return "
        + "(string(namespace-uri($a)), string(prefix-from-QName(node-name($a))), local-name($a))",
          "urn:p",
          "p",
          "attr",
          "",
          "",
          "plain");
    check("declare namespace p='urn:p'; let $root := element root { attribute p:attr { 'value' } } "
        + "for $a in $root/@* return "
        + "(string(namespace-uri($a)), string(prefix-from-QName(node-name($a))), local-name($a))",
          "urn:p",
          "p",
          "attr");
  }
}
