package io.brackit.query.function.fn;

import io.brackit.query.ErrorCode;
import io.brackit.query.Query;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.AnyURI;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Type;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for fn:resolve-QName (XQuery F&amp;O 3.1, section 10.2.2): prefix resolution against the
 * in-scope namespaces of an element, default-namespace handling for unprefixed names (unlike the
 * xs:QName constructor), and the err:FOCA0002/err:FONS0004 error conditions.
 */
public class ResolveQNameTest extends XQueryBaseTest {

  private void check(Sequence expected, String query) {
    ResultChecker.dCheck(expected, new Query(query).execute(ctx));
  }

  private static Atomic ncName(String name) {
    return new Str(name).asType(Type.NCN);
  }

  @Test
  public void emptyArgumentYieldsEmptySequence() {
    check(Bool.TRUE, "empty(resolve-QName((), <a/>))");
  }

  @Test
  public void unprefixedNameWithoutDefaultNamespace() {
    // spec example: returns a QName with local name "hello" that is in no namespace
    check(ncName("hello"), "local-name-from-QName(resolve-QName('hello', <a/>))");
    check(new AnyURI(""), "namespace-uri-from-QName(resolve-QName('hello', <a/>))");
    check(Bool.TRUE, "empty(prefix-from-QName(resolve-QName('hello', <a/>)))");
  }

  @Test
  public void unprefixedNameUsesDefaultNamespace() {
    // unlike the xs:QName constructor, resolve-QName DOES consult the default element namespace
    check(new AnyURI("http://example.org/default"),
          "namespace-uri-from-QName(resolve-QName('hello', <a xmlns='http://example.org/default'/>))");
    check(ncName("hello"), "local-name-from-QName(resolve-QName('hello', <a xmlns='http://example.org/default'/>))");
  }

  @Test
  public void prefixedNameResolvesAgainstInScopeNamespaces() {
    // spec example: the namespace URI is taken from the binding of the prefix "eg"
    check(new AnyURI("http://example.org/myFunctions"),
          "namespace-uri-from-QName(resolve-QName('eg:myFunc', <a xmlns:eg='http://example.org/myFunctions'/>))");
    check(ncName("myFunc"),
          "local-name-from-QName(resolve-QName('eg:myFunc', <a xmlns:eg='http://example.org/myFunctions'/>))");
    // the prefix is retained in the returned expanded-QName
    check(ncName("eg"),
          "prefix-from-QName(resolve-QName('eg:myFunc', <a xmlns:eg='http://example.org/myFunctions'/>))");
  }

  @Test
  public void namespacesAreInheritedFromAncestors() {
    check(new AnyURI("urn:outer"),
          "namespace-uri-from-QName(resolve-QName('x:leaf', (<a xmlns:x='urn:outer'><b/></a>)/*:b))");
  }

  @Test
  public void xmlPrefixIsAlwaysInScope() {
    check(new AnyURI("http://www.w3.org/XML/1998/namespace"),
          "namespace-uri-from-QName(resolve-QName('xml:lang', <a/>))");
  }

  @Test
  public void whitespaceIsCollapsed() {
    // the lexical space of xs:QName permits surrounding whitespace (whitespace facet: collapse)
    check(ncName("hello"), "local-name-from-QName(resolve-QName('  hello  ', <a/>))");
  }

  @Test
  public void invalidLexicalQNameRaisesFOCA0002() {
    for (final String lexical : new String[] { "1:b", "a:b:c", ":b", "a:", "a b", "" }) {
      final QueryException ex = assertThrows(QueryException.class,
                                             () -> new Query("resolve-QName('" + lexical + "', <a/>)").execute(ctx),
                                             lexical);
      assertEquals(ErrorCode.ERR_INVALID_LEXICAL_VALUE, ex.getCode(), lexical);
    }
  }

  @Test
  public void unboundPrefixRaisesFONS0004() {
    final QueryException ex = assertThrows(QueryException.class,
                                           () -> new Query("resolve-QName('unknown:x', <a/>)").execute(ctx));
    assertEquals(ErrorCode.ERR_NO_NAMESPACE_FOR_PREFIX, ex.getCode());

    // a prefix bound on an unrelated sibling scope is not in scope either
    final QueryException ex2 = assertThrows(QueryException.class,
                                            () -> new Query("resolve-QName('eg:x', (<r><a xmlns:eg='urn:x'/><b/></r>)/*:b)").execute(ctx));
    assertEquals(ErrorCode.ERR_NO_NAMESPACE_FOR_PREFIX, ex2.getCode());
  }
}
