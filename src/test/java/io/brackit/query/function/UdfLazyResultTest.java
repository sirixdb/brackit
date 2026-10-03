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
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.jdm.type.AnyItemType;
import io.brackit.query.jdm.type.AtomicType;
import io.brackit.query.jdm.type.Cardinality;
import io.brackit.query.operator.TupleImpl;
import io.brackit.query.module.Functions;
import io.brackit.query.module.StaticContext;
import io.brackit.query.sequence.AbstractSequence;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.LazySequence;
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
          checkFailure(probe.length);
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
}
