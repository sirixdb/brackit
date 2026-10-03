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
package io.brackit.query.function;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.brackit.query.ErrorCode;
import io.brackit.query.Query;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Dbl;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jsonitem.array.DArray;
import io.brackit.query.jsonitem.object.ArrayObject;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.jdm.type.AnyItemType;
import io.brackit.query.jdm.type.AtomicType;
import io.brackit.query.jdm.type.Cardinality;
import io.brackit.query.operator.TupleImpl;
import io.brackit.query.module.Functions;
import io.brackit.query.module.StaticContext;
import io.brackit.query.sequence.AbstractSequence;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.ItemSequence;
import io.brackit.query.sequence.LazySequence;
import io.brackit.query.sequence.TypedSequence;
import io.brackit.query.util.ExprUtil;

/** Construction budgets exercise backend work independently of wall-clock timing. */
public class UdfLazyResultTest extends XQueryBaseTest {
  private static final String NS = "declare namespace probe='urn:brackit:repeatable-udf-test'; ";
  private static final ThreadLocal<Probe> PROBE = ThreadLocal.withInitial(Probe::new);

  static {
    Functions.predefine(new Keys());
    Functions.predefine(new UnknownKeys());
  }

  private static final class Probe {
    int length = 64;
    int constructed;
    int opened;
    int closed;
    int badItem;
    int failAfter = -1;
    boolean repeatable = true;
    boolean overflow;
    boolean nodes;
    boolean arrays;
    boolean varyingArrayLengths;
    boolean failOnSize = true;
    Item value;
    Item[] values;
  }

  private static final class Keys extends AbstractFunction {
    Keys() {
      super(new QNm("urn:brackit:repeatable-udf-test", "probe", "keys"),
            new Signature(SequenceType.ITEM_SEQUENCE),
            true);
    }

    @Override
    public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) {
      Probe probe = PROBE.get();
      return new AbstractSequence() {
        private final Item[] nodeItems = new Item[probe.length];

        private Item item(int position) {
          if (probe.nodes) {
            synchronized (nodeItems) {
              if (nodeItems[position - 1] == null) {
                nodeItems[position - 1] = ctx.getNodeFactory().element(new QNm("n"));
                probe.constructed++;
              }
              return nodeItems[position - 1];
            }
          }
          probe.constructed++;
          if (probe.arrays) {
            return new DArray(probe.varyingArrayLengths && position > 1
                ? List.of(new Int32(position * 10), new Int32(position * 10 + 1))
                : List.of(new Int32(position * 10)));
          }
          if (probe.value != null) {
            return probe.value;
          }
          if (probe.values != null) {
            return probe.values[position - 1];
          }
          return position == probe.badItem ? new Str("bad") : new Int32(position);
        }

        private void checkFailure(int position) {
          if (probe.failAfter >= 0 && position >= probe.failAfter) {
            if (probe.overflow) {
              throw new StackOverflowError();
            }
            throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "probe failure");
          }
        }

        @Override
        public boolean isRepeatable() {
          return probe.repeatable;
        }

        @Override
        public IntNumeric size() {
          if (probe.failOnSize) {
            checkFailure(probe.length);
          }
          return new Int32(probe.length);
        }

        @Override
        public boolean booleanValue() {
          return ExprUtil.asItem(this).booleanValue();
        }

        @Override
        public Item get(IntNumeric position) {
          checkFailure(position.intValue() - 1);
          int index = position.intValue();
          return index <= 0 || index > probe.length ? null : item(index);
        }

        @Override
        public Iter iterate() {
          if (!probe.repeatable && probe.opened != 0) {
            throw new IllegalStateException("Single-use source reopened");
          }
          probe.opened++;
          return new BaseIter() {
            int position;
            boolean closed;

            @Override
            public Item next() {
              checkFailure(position);
              return position == probe.length ? null : item(++position);
            }

            @Override
            public void skip(IntNumeric count) {
              if (count.cmp(Int32.ZERO) > 0) {
                position += (int) Math.min(probe.length - position, count.longValue());
                checkFailure(position);
              }
            }

            @Override
            public void close() {
              if (!closed) {
                closed = true;
                probe.closed++;
              }
            }
          };
        }
      };
    }
  }

  private static final class UnknownKeys extends AbstractFunction {
    UnknownKeys() {
      super(new QNm("urn:brackit:repeatable-udf-test", "probe", "unknown-keys"),
            new Signature(SequenceType.ITEM_SEQUENCE),
            true);
    }

    @Override
    public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) {
      Probe probe = PROBE.get();
      return new LazySequence() {
        @Override
        public boolean isRepeatable() {
          return true;
        }

        @Override
        public Iter iterate() {
          probe.opened++;
          return new BaseIter() {
            int position;

            @Override
            public Item next() {
              if (position == probe.length) {
                return null;
              }
              probe.constructed++;
              return new Int32(++position);
            }

            @Override
            public void close() {
              probe.closed++;
            }
          };
        }
      };
    }
  }

  @BeforeEach
  public void resetProbe() {
    PROBE.set(new Probe());
  }

  private Sequence query(String text) {
    return new Query(NS + text).execute(ctx);
  }

  private int integer(String text) {
    return ((IntNumeric) ExprUtil.asItem(query(text))).intValue();
  }

  private Sequence xqueryResult(String text) {
    return new Query("xquery version '3.0'; " + NS + text).execute(ctx);
  }

  private String calls(String type, String body) {
    return "declare function local:f() as " + type + " {" + body + "}; " + "let $f := function() as " + type + " {"
        + body + "} return ";
  }

  private record LookupOutcome(List<Integer> values, QNm error) {
  }

  private LookupOutcome lookupOutcome(String text) {
    List<Integer> values = new ArrayList<>();
    try {
      Sequence result = query(text);
      if (result != null) {
        try (Iter it = result.iterate()) {
          Item item;
          while ((item = it.next()) != null) {
            values.add(((IntNumeric) item).intValue());
          }
        }
      }
      return new LookupOutcome(values, null);
    } catch (QueryException error) {
      return new LookupOutcome(values, error.getCode());
    }
  }

  private Item lookupContainer(boolean object, Sequence member) {
    return object
        ? new ArrayObject(new QNm[] { new QNm("v"), new QNm("empty"), new QNm("tail") },
                          new Sequence[] { member, null, new Int32(30) })
        : new DArray(Arrays.asList(member, null, new Int32(30)));
  }

  private Probe singletonLookupProbe(boolean repeatable, Item value) {
    resetProbe();
    Probe probe = PROBE.get();
    probe.length = 1;
    probe.repeatable = repeatable;
    probe.value = value;
    return probe;
  }

  @ParameterizedTest
  @ValueSource(strings = { "item()*", "item()+" })
  public void countPreservesCheapCardinality(String type) {
    assertEquals(64, integer("count(probe:keys())"));
    assertEquals(0, PROBE.get().constructed);
    assertEquals(64, integer("declare function local:f() as " + type + " { probe:keys() }; count(local:f())"));
    assertEquals(0, PROBE.get().constructed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "exists(%s)", "empty(%s)", "subsequence(%s,1,1)" })
  public void earlyExitMatchesDirectConstruction(String consumer) {
    ExprUtil.asItem(query(consumer.formatted("probe:keys()")));
    int direct = PROBE.get().constructed;
    assertEquals(1, direct);
    resetProbe();
    ExprUtil.asItem(query("declare function local:f() as item()* { probe:keys() }; " + consumer.formatted(
                                                                                                          "local:f()")));
    assertEquals(direct, PROBE.get().constructed);
    assertEquals(PROBE.get().opened, PROBE.get().closed);
  }

  @Test
  public void dynamicAndNestedIdentityCallsPreserveCardinality() {
    assertEquals(64, integer("let $f := function() as item()* { probe:keys() } return count($f())"));
    assertEquals(0, PROBE.get().constructed);
    assertEquals(64,
                 integer("declare function local:id($s as item()*) as item()* { ($s) }; "
                     + "declare function local:f() as item()* { local:id(probe:keys()) }; count(local:f())"));
    assertEquals(0, PROBE.get().constructed);
  }

  @Test
  public void prefixConsumptionIsLazyAndRepeatable() {
    Sequence result = query("declare function local:f() as item()* { probe:keys() }; local:f()");
    assertEquals(0, PROBE.get().constructed);
    for (int pass = 0; pass < 2; pass++) {
      try (Iter it = result.iterate()) {
        for (int i = 1; i <= 3; i++) {
          assertEquals(i, ((IntNumeric) it.next()).intValue());
        }
      }
    }
    assertEquals(6, PROBE.get().constructed);
    assertEquals(PROBE.get().opened, PROBE.get().closed);
  }

  @Test
  public void nonRepeatableResultsKeepEagerMaterializationAndReuse() {
    PROBE.get().repeatable = false;
    assertEquals(128,
                 integer("declare function local:f() as item()* { probe:keys() }; "
                     + "let $s := local:f() return count($s) + count($s)"));
    assertEquals(64, PROBE.get().constructed);
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
  }

  @Test
  public void nonRepeatableErrorIsStillCaughtAtTheCall() {
    PROBE.get().repeatable = false;
    PROBE.get().failAfter = 2;
    assertEquals(99,
                 integer("declare function local:f() as item()* { probe:keys() }; " + "try {local:f()} catch * {99}"));
    assertEquals(PROBE.get().opened, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "item()?", "xs:integer?" })
  public void singletonReturnRejectsMultipleItems(String type) {
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                 assertThrows(QueryException.class,
                              () -> integer("declare function local:f() as " + type
                                  + " { probe:keys() }; count(local:f())")).getCode());
    assertTrue(PROBE.get().constructed <= 2);
  }

  @ParameterizedTest
  @ValueSource(strings = { "item()+", "xs:integer+" })
  public void requiredReturnRejectsEmpty(String type) {
    PROBE.get().length = 0;
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                 assertThrows(QueryException.class,
                              () -> integer("declare function local:f() as " + type
                                  + " { probe:keys() }; count(local:f())")).getCode());
  }

  @ParameterizedTest
  @ValueSource(strings = { "item()?", "item()*", "xs:integer?", "xs:integer*", "empty-sequence()" })
  public void optionalReturnAcceptsEmpty(String type) {
    PROBE.get().length = 0;
    assertEquals(0, integer("declare function local:f() as " + type + " { probe:keys() }; count(local:f())"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "item()", "item()?", "item()+", "xs:integer", "xs:integer?", "xs:integer+" })
  public void constrainedReturnAcceptsOneItem(String type) {
    PROBE.get().length = 1;
    assertEquals(1, integer("declare function local:f() as " + type + " { probe:keys() }; count(local:f())"));
  }

  @Test
  public void emptyReturnRejectsItems() {
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                 assertThrows(QueryException.class,
                              () -> integer("declare function local:f() as empty-sequence() "
                                  + "{ probe:keys() }; count(local:f())")).getCode());
  }

  @Test
  public void typedCountAndSkipValidateTheTail() {
    PROBE.get().badItem = 2;
    String declaration = "declare function local:f() as xs:integer* { probe:keys() }; ";
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                 assertThrows(QueryException.class, () -> integer(declaration + "count(local:f())")).getCode());
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                 assertThrows(QueryException.class,
                              () -> ExprUtil.asItem(query(declaration + "subsequence(local:f(),3,1)"))).getCode());
  }

  @Test
  public void atomicPromotionSurvivesRepeatedReadsAndGet() {
    PROBE.get().length = 2;
    Sequence result = query("declare function local:f() as xs:double* { probe:keys() }; local:f()");
    for (int pass = 0; pass < 2; pass++) {
      try (Iter it = result.iterate()) {
        assertInstanceOf(Dbl.class, it.next());
        assertInstanceOf(Dbl.class, it.next());
        assertNull(it.next());
      }
    }
    assertInstanceOf(Dbl.class, result.get(Int32.ONE));
    assertNull(result.get(new Int32(3)));
  }

  @ParameterizedTest
  @ValueSource(strings = { "count(local:f())", "count($f())" })
  public void deferredOverflowRemainsCatchable(String call) {
    PROBE.get().overflow = true;
    PROBE.get().failAfter = 2;
    String declarations = "declare function local:f() as item()* { probe:keys() }; "
        + "let $f := function() as item()* { probe:keys() } return ";
    assertEquals(ErrorCode.BIT_DYN_RT_STACK_OVERFLOW,
                 assertThrows(QueryException.class, () -> integer(declarations + call)).getCode());
    assertEquals(99, integer(declarations + "try {" + call + "} catch * {99}"));
  }

  @Test
  public void exactOneSignatureChecksBothBounds() {
    // Master's parser gives a missing occurrence indicator the * cardinality.
    // Exercise the public function signature directly without changing that parser.
    for (int length : new int[] { 0, 1, 64 }) {
      resetProbe();
      PROBE.get().length = length;
      UDF function = new UDF(new QNm("urn:test", "test", "one"),
                             new Signature(new SequenceType(AtomicType.INR, Cardinality.One)),
                             false);
      function.setExpr(new Keys().execute(null, ctx, new Sequence[0]));
      FunctionExpr expression = new FunctionExpr(null, function);
      if (length == 1) {
        assertEquals(1, ((IntNumeric) expression.evaluate(ctx, new TupleImpl())).intValue());
      } else {
        assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                     assertThrows(QueryException.class, () -> expression.evaluate(ctx, new TupleImpl())).getCode());
        assertTrue(PROBE.get().constructed <= 2);
      }
    }
  }

  @Test
  public void indexedAccessAndNativeSkipStayCheap() {
    Sequence result = query("declare function local:f() as item()* { probe:keys() }; local:f()");
    assertEquals(60, ((IntNumeric) result.get(new Int32(60))).intValue());
    assertEquals(1, PROBE.get().constructed);
    resetProbe();
    assertEquals(5,
                 integer("declare function local:f() as item()* { probe:keys() }; "
                     + "count(subsequence(local:f(),60,5))"));
    assertEquals(5, PROBE.get().constructed);
  }

  @Test
  public void errorsFromRepeatableIterationRemainInsideConsumingTry() {
    PROBE.get().badItem = 2;
    assertEquals(99,
                 integer("declare function local:f() as xs:integer* { probe:keys() }; "
                     + "try {count(local:f())} catch * {99}"));
  }

  @Test
  public void overflowGuardCoversIndexedAndIteratorOperations() {
    PROBE.get().overflow = true;
    PROBE.get().failAfter = 0;
    Sequence result = query("declare function local:f() as item()* { probe:keys() }; local:f()");
    assertEquals(ErrorCode.BIT_DYN_RT_STACK_OVERFLOW, assertThrows(QueryException.class, result::size).getCode());
    assertEquals(ErrorCode.BIT_DYN_RT_STACK_OVERFLOW,
                 assertThrows(QueryException.class, () -> result.get(Int32.ONE)).getCode());
    try (Iter it = result.iterate()) {
      assertEquals(ErrorCode.BIT_DYN_RT_STACK_OVERFLOW, assertThrows(QueryException.class, it::next).getCode());
    }
    try (Iter it = result.iterate()) {
      assertEquals(ErrorCode.BIT_DYN_RT_STACK_OVERFLOW,
                   assertThrows(QueryException.class, () -> it.skip(Int32.ONE)).getCode());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "item()*", "item()+" })
  public void prefixOfUnknownCardinalityDoesNotCountTheTail(String type) {
    assertEquals(3,
                 integer("declare function local:f() as " + type
                     + " {probe:unknown-keys()}; subsequence(local:f(),3,1)"));
    assertEquals(3, PROBE.get().constructed);
    assertEquals(PROBE.get().opened, PROBE.get().closed);
  }

  @Test
  public void requiredManyReturnPreservesNativeSkip() {
    assertEquals(5,
                 integer("declare function local:f() as item()+ {probe:keys()}; "
                     + "count(subsequence(local:f(),60,5))"));
    assertEquals(6, PROBE.get().constructed);
  }

  @Test
  public void sharedRepeatableNodesRetainIdentity() {
    PROBE.get().nodes = true;
    assertTrue(ExprUtil.asItem(query("declare function local:f() as node()* {probe:keys()}; "
        + "let $s := local:f() return subsequence($s,1,1) is subsequence($s,1,1)")).booleanValue());
    assertEquals(1, PROBE.get().constructed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "node()*", "xs:string*", "xs:boolean*" })
  public void constrainedItemTypeRejectsWrongItems(String type) {
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                 assertThrows(QueryException.class,
                              () -> integer("declare function local:f() as " + type
                                  + " {probe:keys()}; count(local:f())")).getCode());
  }

  @Test
  public void unmarkedRecursiveResultKeepsMasterReuse() {
    assertEquals(12,
                 integer("declare function local:f($n) as item()* { "
                     + "if ($n eq 0) then () else ($n,local:f($n - 1)) }; "
                     + "let $s := local:f(3) return sum($s) + sum($s)"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void numericPredicatesKeepRemainingFilters(String call) {
    PROBE.get().length = 1;
    for (Bool keep : new Bool[] { Bool.FALSE, Bool.TRUE }) {
      ctx.bind(new QNm("keep"), keep);
      String declarations = "declare variable $keep external; " + calls("item()*", "probe:keys()");
      assertEquals(keep.booleanValue() ? 1 : 0,
                   ((IntNumeric) ExprUtil.asItem(xqueryResult(declarations + "count(<r><a/></r>/a[" + call
                       + "][$keep])"))).intValue());
      assertEquals(keep.booleanValue() ? 1 : 0,
                   ((IntNumeric) ExprUtil.asItem(xqueryResult(declarations + "count((10,20)[" + call + "][$keep])")))
                                                                                                                     .intValue());
    }
    assertEquals(PROBE.get().opened, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void typedViewsOfLazyNumericPredicatesKeepPositionAndRemainingFilters(String call) {
    PROBE.get().length = 1;
    String declarations = "declare variable $keep external; "
        + "declare function local:select($s as xs:integer*) as item()* {(10,20)[$s][$keep]}; " + calls("item()*",
                                                                                                       "probe:keys()");
    for (String type : new String[] { "item()*", "'http://www.w3.org/2001/XMLSchema':integer*" }) {
      String binding = declarations + "let $s as " + type + " := " + call + " return ";
      ctx.bind(new QNm("keep"), Bool.FALSE);
      assertEquals(0,
                   ((IntNumeric) ExprUtil.asItem(xqueryResult(binding + "count(<r><a/></r>/a[$s][$keep])")))
                                                                                                            .intValue());
      assertEquals(0, ((IntNumeric) ExprUtil.asItem(xqueryResult(binding + "count((10,20)[$s][$keep])"))).intValue());
      PROBE.get().value = new Int32(2);
      assertNull(ExprUtil.asItem(xqueryResult(binding + "(10[$s]) + 1")));
      PROBE.get().value = null;
    }
    assertEquals(0,
                 ((IntNumeric) ExprUtil.asItem(xqueryResult(declarations + "count(local:select(" + call + "))")))
                                                                                                                 .intValue());
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void numericPredicatesMatchScalarAndDependentPositions(String call) {
    PROBE.get().length = 1;
    String declarations = calls("item()*", "probe:keys()");
    assertEquals(11, ((IntNumeric) ExprUtil.asItem(xqueryResult(declarations + "(10[" + call + "]) + 1"))).intValue());
    PROBE.get().value = new Int32(2);
    assertNull(ExprUtil.asItem(xqueryResult(declarations + "(10[" + call + "]) + 1")));
    assertEquals(20, ((IntNumeric) ExprUtil.asItem(xqueryResult(declarations + "(10,20)[" + call + "]"))).intValue());
    String dependent = "if (position() gt 0) then " + call + " else ()";
    assertNull(ExprUtil.asItem(xqueryResult(declarations + "(10[" + dependent + "]) + 1")));
    assertEquals(20,
                 ((IntNumeric) ExprUtil.asItem(xqueryResult(declarations + "(10,20)[" + dependent + "]"))).intValue());
    PROBE.get().value = new Dbl(1.5);
    assertNull(ExprUtil.asItem(xqueryResult(declarations + "(10[" + call + "]) + 1")));
    assertNull(ExprUtil.asItem(xqueryResult(declarations + "(10,20)[" + call + "]")));
    assertEquals(PROBE.get().opened, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void reverseAxisNumericPredicatesKeepAxisOrderAndRemainingFilters(String call) {
    PROBE.get().length = 1;
    String path = "<r><a/><b/><c/></r>/c/preceding-sibling::*[" + call + "][$keep]";
    String declarations = "declare variable $keep external; " + calls("item()*", "probe:keys()");
    ctx.bind(new QNm("keep"), Bool.TRUE);
    assertEquals("b", ExprUtil.asItem(xqueryResult(declarations + "name(" + path + ")")).atomize().stringValue());
    PROBE.get().value = new Int32(2);
    assertEquals("a", ExprUtil.asItem(xqueryResult(declarations + "name(" + path + ")")).atomize().stringValue());
    ctx.bind(new QNm("keep"), Bool.FALSE);
    assertNull(ExprUtil.asItem(xqueryResult(declarations + path)));
    ctx.bind(new QNm("keep"), Bool.TRUE);
    PROBE.get().value = new Int32(3);
    assertNull(ExprUtil.asItem(xqueryResult(declarations + path)));
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void jsoniqNumericPredicatesRetainContextTruthiness(String call) {
    PROBE.get().length = 1;
    PROBE.get().value = new Int32(2);
    String declarations = calls("item()*", "probe:keys()");
    String predicate = "[?if ($$ gt 0) then " + call + " else ()]";
    assertEquals(30, integer(declarations + "sum((10,20)" + predicate + ")"));
    assertEquals(11, integer(declarations + "(10" + predicate + ") + 1"));
    PROBE.get().value = Int32.ZERO;
    assertEquals(0, integer(declarations + "count((10,20)" + predicate + ")"));
    assertNull(ExprUtil.asItem(query(declarations + "(10" + predicate + ") + 1")));
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void lazyPredicatesRetainEmptyAndNonNumericBooleanBehavior(String call) {
    String declarations = calls("item()*", "probe:keys()");
    PROBE.get().length = 0;
    assertNull(ExprUtil.asItem(xqueryResult(declarations + "(10[" + call + "]) + 1")));
    PROBE.get().length = 1;
    PROBE.get().value = Bool.TRUE;
    assertEquals(11, ((IntNumeric) ExprUtil.asItem(xqueryResult(declarations + "(10[" + call + "]) + 1"))).intValue());
    PROBE.get().value = Bool.FALSE;
    assertNull(ExprUtil.asItem(xqueryResult(declarations + "(10[" + call + "]) + 1")));
    PROBE.get().value = null;
    PROBE.get().length = 2;
    assertEquals(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
                 assertThrows(QueryException.class,
                              () -> ExprUtil.asItem(xqueryResult(declarations + "(10[" + call + "]) + 1"))).getCode());
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void unmarkedScalarPredicatesRetainMasterBehavior(String call) {
    PROBE.get().repeatable = false;
    PROBE.get().length = 1;
    PROBE.get().value = new Dbl(1.5);
    assertEquals(11,
                 ((IntNumeric) ExprUtil.asItem(xqueryResult(calls("item()*", "probe:keys()") + "(10[" + call
                     + "]) + 1"))).intValue());
    assertEquals(1, PROBE.get().constructed);
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void multiArrayResultsAndTypedIdentityCallsMapEachArray(String call) {
    PROBE.get().length = 2;
    PROBE.get().arrays = true;
    for (String access : new String[] { "[0]", "[-1]", "[]" }) {
      for (String body : new String[] { "probe:keys()", "local:id(probe:keys())" }) {
        String declarations = "declare function local:id($s as item()*) as item()* {($s)}; " + calls("item()*", body);
        Sequence result = query(declarations + call + access);
        try (Iter it = result.iterate()) {
          assertEquals(10, ((IntNumeric) it.next()).intValue());
          assertEquals(20, ((IntNumeric) it.next()).intValue());
          assertNull(it.next());
        }
      }
      Sequence typed = query("let $s as item()* := probe:keys() return $s" + access);
      try (Iter it = typed.iterate()) {
        assertEquals(10, ((IntNumeric) it.next()).intValue());
        assertEquals(20, ((IntNumeric) it.next()).intValue());
        assertNull(it.next());
      }
    }
  }

  @Test
  public void typedIdentityViewPreservesCardinalityGetAndNativeSkip() {
    Sequence source = new Keys().execute(null, ctx, new Sequence[0]);
    Sequence typed = TypedSequence.toTypedSequence(SequenceType.ITEM_SEQUENCE, source);
    assertInstanceOf(LazySequence.class, typed);
    assertTrue(typed.isRepeatable());
    assertEquals(64, typed.size().intValue());
    assertEquals(0, PROBE.get().constructed);
    assertEquals(60, ((IntNumeric) typed.get(new Int32(60))).intValue());
    assertEquals(1, PROBE.get().constructed);
    try (Iter it = typed.iterate()) {
      it.skip(new Int32(59));
      assertEquals(60, ((IntNumeric) it.next()).intValue());
    }
    assertEquals(2, PROBE.get().constructed);
    assertEquals(PROBE.get().opened, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void unknownCardinalityTypedIdentityCallsKeepPrefixWorkBounded(String call) {
    String declarations = "declare function local:id($s as item()*) as item()* {($s)}; " + calls("item()*",
                                                                                                 "local:id(probe:unknown-keys())");
    Sequence result = query(declarations + call);
    assertEquals(0, PROBE.get().constructed);
    for (int pass = 0; pass < 2; pass++) {
      try (Iter it = result.iterate()) {
        assertEquals(1, ((IntNumeric) it.next()).intValue());
      }
    }
    assertEquals(2, PROBE.get().constructed);
    assertEquals(PROBE.get().opened, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void typedCountsReuseOnlySuccessfullyValidatedCardinality(String call) {
    String declarations = calls("xs:integer*", "probe:keys()");
    assertEquals(128, integer(declarations + "let $s as item()* := " + call + " return count($s)+count($s)"));
    assertEquals(64, PROBE.get().constructed);
    assertEquals(1, PROBE.get().opened);
    assertEquals(PROBE.get().opened, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void partialReadsAndCachedCountsNeverBypassLaterConversions(String call) {
    Sequence result = query(calls("xs:double*", "probe:keys()") + call);
    try (Iter it = result.iterate()) {
      assertInstanceOf(Dbl.class, it.next());
    }
    assertEquals(1, PROBE.get().constructed);
    assertEquals(64, result.size().intValue());
    assertEquals(65, PROBE.get().constructed);
    assertEquals(64, result.size().intValue());
    assertEquals(65, PROBE.get().constructed);
    assertInstanceOf(Dbl.class, result.get(Int32.ONE));
    try (Iter it = result.iterate()) {
      it.skip(new Int32(2));
      assertInstanceOf(Dbl.class, it.next());
    }
    assertEquals(69, PROBE.get().constructed);
    assertEquals(PROBE.get().opened, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void invalidTailCountsFailOnEveryRead(String call) {
    PROBE.get().badItem = 64;
    Sequence result = query(calls("xs:integer*", "probe:keys()") + call);
    for (int pass = 1; pass <= 2; pass++) {
      assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, assertThrows(QueryException.class, result::size).getCode());
      assertEquals(64 * pass, PROBE.get().constructed);
    }
    assertEquals(PROBE.get().opened, PROBE.get().closed);
    try (Iter it = result.iterate()) {
      assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                   assertThrows(QueryException.class, () -> it.skip(new Int32(64))).getCode());
    }
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                 assertThrows(QueryException.class, () -> result.get(new Int32(64))).getCode());
  }

  @Test
  public void concurrentTypedCountsShareOneSuccessfulValidation() throws Exception {
    Sequence result = query("declare function local:f() as xs:integer* {probe:keys()}; local:f()");
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> {
        start.await();
        return result.size();
      });
      var second = executor.submit(() -> {
        start.await();
        return result.size();
      });
      start.countDown();
      assertEquals(64, first.get(10, TimeUnit.SECONDS).intValue());
      assertEquals(64, second.get(10, TimeUnit.SECONDS).intValue());
    }
    assertEquals(64, PROBE.get().constructed);
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void lazyArrayBoundsPreserveFullWidthAndSingletonBehavior(String call) {
    for (String body : new String[] { "probe:keys()", "local:id(probe:keys())" }) {
      for (String access : new String[] { "[4294967296]", "[1]", "[0]", "[-1]", "[-4294967296]", "[-2]", "[2147483648]",
          "[-2147483648]", "[9223372036854775807]", "[-9223372036854775808]", "[1.0]", "['bad']", "[true()]",
          "[()]" }) {
        String text = "declare function local:id($s as item()*) as item()* {($s)}; " + calls("item()*", body) + call
            + access;
        Item array = new DArray(List.of(new Int32(10)));
        Probe baselineProbe = singletonLookupProbe(false, array);
        LookupOutcome baseline = lookupOutcome(text);
        assertEquals(1, baselineProbe.opened, text);
        assertEquals(1, baselineProbe.closed, text);
        Probe lazyProbe = singletonLookupProbe(true, array);
        assertEquals(baseline, lookupOutcome(text), text);
        assertEquals(1, lazyProbe.constructed, text);
        assertEquals(1, lazyProbe.opened, text);
        assertEquals(1, lazyProbe.closed, text);
      }
    }
    resetProbe();
    PROBE.get().length = 1;
    PROBE.get().arrays = true;
    assertEquals(0, integer("let $s as item()* := probe:keys() return count($s[4294967296])"));
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void sequenceValuedAndEmptyLookupsMatchMaterializedSingletons(String call) {
    for (boolean object : new boolean[] { false, true }) {
      for (Sequence member : new Sequence[] { new ItemSequence(new Int32(10), new Int32(20)), null, new ItemSequence(),
          new Int32(10) }) {
        for (String body : new String[] { "probe:keys()", "local:id(probe:keys())" }) {
          for (String access : object
              ? new String[] { ".v", ".(0)", ".('v')", ".empty", ".(1)", ".tail", ".missing" }
              : new String[] { "[0]", "[-3]", "[1]", "[-2]", "[2]", "[-1]", "[3]", "[-4]", "[4294967296]",
                  "[-4294967296]" }) {
            String text = "declare function local:id($s as item()*) as item()* {($s)}; " + calls("item()*", body) + call
                + access;
            Probe baselineProbe = singletonLookupProbe(false, lookupContainer(object, member));
            LookupOutcome baseline = lookupOutcome(text);
            if (access.equals("[0]") || access.equals("[-3]") || access.equals(".v") || access.equals(".(0)") || access
                                                                                                                       .equals(".('v')")) {
              List<Integer> expected = member == null || member.size().intValue() == 0
                  ? List.of()
                  : member instanceof Item ? List.of(10) : List.of(10, 20);
              assertEquals(new LookupOutcome(expected, null), baseline, text);
            }
            assertEquals(1, baselineProbe.opened, text);
            assertEquals(1, baselineProbe.closed, text);
            Probe lazyProbe = singletonLookupProbe(true, lookupContainer(object, member));
            assertEquals(baseline, lookupOutcome(text), text);
            assertEquals(1, lazyProbe.constructed, text);
            assertEquals(1, lazyProbe.opened, text);
            assertEquals(1, lazyProbe.closed, text);
          }
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void emptySelectedSequencesDoNotHideLaterLookupResults(String call) {
    for (boolean object : new boolean[] { false, true }) {
      for (String body : new String[] { "probe:keys()", "local:id(probe:keys())" }) {
        resetProbe();
        Probe probe = PROBE.get();
        probe.length = 3;
        probe.values = new Item[] { lookupContainer(object, null), lookupContainer(object, new ItemSequence()),
            lookupContainer(object, new ItemSequence(new Int32(10), new Int32(20))) };
        String text = "declare function local:id($s as item()*) as item()* {($s)}; " + calls("item()*", body) + call
            + (object ? ".v" : "[0]");
        assertEquals(new LookupOutcome(List.of(10, 20), null), lookupOutcome(text));
        assertEquals(3, probe.constructed);
        assertEquals(1, probe.opened);
        assertEquals(1, probe.closed);
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void selectedLookupReadersCloseAfterFullEmptyAndPrefixConsumption(String call) {
    for (boolean object : new boolean[] { false, true }) {
      for (String body : new String[] { "probe:keys()", "local:id(probe:keys())" }) {
        for (int length : new int[] { 0, 3, 64 }) {
          resetProbe();
          Probe nestedProbe = PROBE.get();
          nestedProbe.length = length;
          Sequence member = new UnknownKeys().execute(null, ctx, new Sequence[0]);
          Probe outerProbe = singletonLookupProbe(true, lookupContainer(object, member));
          String operand = call + (object ? ".v" : "[-3]");
          String text = "declare function local:id($s as item()*) as item()* {($s)}; " + calls("item()*", body)
              + (length == 64 ? "subsequence(" + operand + ",1,1)" : operand);
          Sequence result = query(text);
          assertEquals(0, outerProbe.constructed);
          assertEquals(0, nestedProbe.constructed);
          int count = 0;
          try (Iter it = result.iterate()) {
            Item item;
            while ((item = it.next()) != null) {
              assertEquals(++count, ((IntNumeric) item).intValue());
            }
          }
          assertEquals(length == 64 ? 1 : length, count);
          assertEquals(1, outerProbe.constructed);
          assertEquals(count, nestedProbe.constructed);
          assertEquals(1, outerProbe.opened);
          assertEquals(1, outerProbe.closed);
          assertEquals(1, nestedProbe.opened);
          assertEquals(1, nestedProbe.closed);
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void selectedLookupReadersCloseOnNestedAndOuterFailures(String call) {
    for (boolean object : new boolean[] { false, true }) {
      for (boolean nestedFailure : new boolean[] { false, true }) {
        resetProbe();
        Probe nestedProbe = PROBE.get();
        nestedProbe.length = 1;
        nestedProbe.failAfter = nestedFailure ? 1 : -1;
        Sequence member = new Keys().execute(null, ctx, new Sequence[0]);
        Probe outerProbe = singletonLookupProbe(true, lookupContainer(object, member));
        if (!nestedFailure) {
          outerProbe.length = 2;
          outerProbe.failAfter = 1;
        }
        Sequence result = query(calls("item()*", "probe:keys()") + call + (object ? ".v" : "[0]"));
        try (Iter it = result.iterate()) {
          assertEquals(1, ((IntNumeric) it.next()).intValue());
          assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, assertThrows(QueryException.class, it::next).getCode());
          assertEquals(1, outerProbe.opened);
          assertEquals(1, outerProbe.closed);
          assertEquals(1, nestedProbe.opened);
          assertEquals(1, nestedProbe.closed);
          assertNull(it.next());
        }
        assertEquals(1, outerProbe.closed);
        assertEquals(1, nestedProbe.closed);
      }
      Probe probe = singletonLookupProbe(true, lookupContainer(object, new Int32(10)));
      Sequence result = query(calls("item()*", "probe:keys()") + call + (object ? ".([1])" : "['bad']"));
      try (Iter it = result.iterate()) {
        assertThrows(QueryException.class, it::next);
        assertEquals(1, probe.opened);
        assertEquals(1, probe.closed);
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void outOfRangeArrayMembersDoNotHideLaterMatches(String call) {
    PROBE.get().length = 2;
    PROBE.get().arrays = true;
    PROBE.get().varyingArrayLengths = true;
    assertEquals(21, integer(calls("item()*", "probe:keys()") + call + "[1]"));
    assertEquals(2, PROBE.get().constructed);
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void arrayConsumersCloseReadersAfterFullAndPrefixConsumption(String call) {
    for (String access : new String[] { "[0]", "[-1]", "[]" }) {
      for (boolean prefix : new boolean[] { false, true }) {
        resetProbe();
        PROBE.get().arrays = true;
        PROBE.get().length = 2;
        PROBE.get().varyingArrayLengths = true;
        String operand = call + access;
        String consumer = prefix ? "subsequence(" + operand + ",1,1)" : operand;
        assertEquals(prefix ? 1 : access.equals("[]") ? 3 : 2,
                     integer(calls("item()*", "probe:keys()") + "count(" + consumer + ")"));
        assertEquals(prefix ? 1 : 2, PROBE.get().constructed);
        assertEquals(1, PROBE.get().opened);
        assertEquals(1, PROBE.get().closed);
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void arrayConsumersCloseReadersOnIndexAndSourceFailures(String call) {
    PROBE.get().arrays = true;
    Sequence result = query(calls("item()*", "probe:keys()") + call + "['bad']");
    try (Iter it = result.iterate()) {
      assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, assertThrows(QueryException.class, it::next).getCode());
      assertEquals(1, PROBE.get().opened);
      assertEquals(1, PROBE.get().closed);
    }
    resetProbe();
    PROBE.get().arrays = true;
    PROBE.get().failAfter = 1;
    result = query(calls("item()*", "probe:keys()") + call + "[]");
    try (Iter it = result.iterate()) {
      assertEquals(10, ((IntNumeric) it.next()).intValue());
      assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, assertThrows(QueryException.class, it::next).getCode());
      assertEquals(1, PROBE.get().opened);
      assertEquals(1, PROBE.get().closed);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void typeswitchClosesReadersOnMatchRejectionAndDefault(String call) {
    for (String body : new String[] { "typeswitch(" + call + ") case item()* return 42 default return 0", "(typeswitch("
        + call + ") case item()* return 42 default return 0) + 1", "typeswitch(" + call
            + ") case xs:string* return 0 case item()* return 42 default return 0", "typeswitch(" + call
                + ") case xs:integer? return 0 case item()* return 42 default return 0", "typeswitch(" + call
                    + ") case xs:string* return 0 default return 42", "typeswitch(" + call
                        + ") case $s as item()* return count($s) default return 0" }) {
      resetProbe();
      PROBE.get().length = 3;
      int expected = body.endsWith("+ 1") ? 43 : body.contains("count($s)") ? 3 : 42;
      assertEquals(expected, integer(calls("item()*", "probe:keys()") + body));
      assertTrue(PROBE.get().opened > 0);
      assertEquals(PROBE.get().opened, PROBE.get().closed);
    }
    resetProbe();
    PROBE.get().length = 3;
    PROBE.get().badItem = 3;
    assertEquals(42,
                 integer(calls("item()*", "probe:keys()") + "typeswitch(" + call
                     + ") case xs:integer* return 0 default return 42"));
    assertEquals(3, PROBE.get().constructed);
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
    resetProbe();
    PROBE.get().length = 0;
    assertEquals(42,
                 integer(calls("item()*", "probe:keys()") + "typeswitch(" + call
                     + ") case empty-sequence() return 42 default return 0"));
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void typeswitchClosesReadersOnIterationAndCaseBodyFailures(String call) {
    PROBE.get().failAfter = 1;
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                 assertThrows(QueryException.class,
                              () -> integer(calls("item()*", "probe:keys()") + "typeswitch(" + call
                                  + ") case item()* return 42 default return 0")).getCode());
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
    resetProbe();
    assertThrows(QueryException.class,
                 () -> integer(calls("item()*", "probe:keys()") + "typeswitch(" + call
                     + ") case item()* return error() default return 0"));
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void reverseClosesReadersOnSuccessAndIterationFailure(String call) {
    PROBE.get().length = 3;
    Sequence result = query(calls("item()*", "probe:keys()") + "reverse(" + call + ")");
    try (Iter it = result.iterate()) {
      for (int expected = 3; expected > 0; expected--) {
        assertEquals(expected, ((IntNumeric) it.next()).intValue());
      }
      assertNull(it.next());
    }
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
    resetProbe();
    PROBE.get().length = 3;
    assertEquals(3, integer(calls("item()*", "probe:keys()") + "subsequence(reverse(" + call + "),1,1)"));
    assertEquals(3, PROBE.get().constructed);
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
    resetProbe();
    PROBE.get().failAfter = 2;
    PROBE.get().failOnSize = false;
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                 assertThrows(QueryException.class,
                              () -> ExprUtil.asItem(query(calls("item()*", "probe:keys()") + "reverse(" + call + ")")))
                                                                                                                       .getCode());
    assertEquals(2, PROBE.get().constructed);
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
  }

  @ParameterizedTest
  @ValueSource(strings = { "local:f()", "$f()" })
  public void codepointConversionClosesReadersOnSuccessEmptyAndFailure(String call) {
    PROBE.get().length = 3;
    PROBE.get().value = new Int32(65);
    assertEquals("AAA",
                 ExprUtil.asItem(query(calls("item()*", "probe:keys()") + "codepoints-to-string(" + call + ")"))
                         .atomize()
                         .stringValue());
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
    resetProbe();
    PROBE.get().length = 0;
    assertEquals("",
                 ExprUtil.asItem(query(calls("item()*", "probe:keys()") + "codepoints-to-string(" + call + ")"))
                         .atomize()
                         .stringValue());
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
    resetProbe();
    PROBE.get().value = Int32.ZERO;
    assertEquals(ErrorCode.ERR_CODE_POINT_NOT_VALID,
                 assertThrows(QueryException.class,
                              () -> ExprUtil.asItem(query(calls("item()*", "probe:keys()") + "codepoints-to-string("
                                  + call + ")"))).getCode());
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
    resetProbe();
    PROBE.get().value = new Int32(65);
    PROBE.get().failAfter = 1;
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                 assertThrows(QueryException.class,
                              () -> ExprUtil.asItem(query(calls("item()*", "probe:keys()") + "codepoints-to-string("
                                  + call + ")"))).getCode());
    assertEquals(1, PROBE.get().opened);
    assertEquals(1, PROBE.get().closed);
  }
}
