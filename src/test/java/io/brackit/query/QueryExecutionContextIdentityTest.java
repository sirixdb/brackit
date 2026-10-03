package io.brackit.query;

import java.util.concurrent.atomic.AtomicInteger;

import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.block.FJControl;
import io.brackit.query.compiler.Bits;
import io.brackit.query.expr.DefaultCtxItem;
import io.brackit.query.expr.BoundVariable;
import io.brackit.query.expr.FilterExpr;
import io.brackit.query.function.AbstractFunction;
import io.brackit.query.function.DynamicFunctionExpr;
import io.brackit.query.function.FunctionExpr;
import io.brackit.query.jdm.Function;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jdm.SplittableSequence;
import io.brackit.query.jdm.node.NodeStore;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.module.MainModule;
import io.brackit.query.module.Functions;
import io.brackit.query.module.Namespaces;
import io.brackit.query.module.StaticContext;
import io.brackit.query.operator.TupleImpl;
import io.brackit.query.operator.ForBind;
import io.brackit.query.operator.Start;
import io.brackit.query.operator.morsel.MorselPipeExpr;
import io.brackit.query.operator.morsel.SplitAwareExpr;
import io.brackit.query.sequence.ItemSequence;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.LazySequence;
import io.brackit.query.util.forkjoin.Task;
import io.brackit.query.util.ExprUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutionContextIdentityTest extends XQueryBaseTest {
  private static final class BackendContext extends BrackitQueryContext {
    private final int marker = 123;

    BackendContext(NodeStore store) {
      super(store);
    }

    @Override
    public void applyUpdates() {
      assertNotNull(QueryExecution.current());
      super.applyUpdates();
    }
  }

  private Query query(Function function, boolean dynamic) {
    MainModule module = new MainModule();
    module.setExpr(dynamic ? new DynamicFunctionExpr(null, function, null) : new FunctionExpr(null, function));
    return new Query(module);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void extensionConsumersReceiveTheBackendContextDuringEagerAndLazyAccess(boolean dynamic) {
    BackendContext backend = new BackendContext(store);
    AtomicInteger calls = new AtomicInteger();
    Function function = new AbstractFunction(new QNm("probe"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
      @Override
      public Sequence execute(StaticContext sctx, QueryContext received, Sequence[] args) {
        assertSame(backend, received);
        assertEquals(123, ((BackendContext) received).marker);
        return new LazySequence() {
          @Override
          public Iter iterate() {
            assertSame(backend, received);
            return new BaseIter() {
              private int position;

              @Override
              public Item next() {
                assertSame(backend, received);
                assertEquals(123, ((BackendContext) received).marker);
                calls.incrementAndGet();
                return ++position <= 3 ? new Int32(position) : null;
              }

              @Override
              public void close() {
                assertSame(backend, received);
              }
            };
          }
        };
      }
    };
    Query query = query(function, dynamic);
    Sequence result = query.execute(backend);
    assertEquals(0, calls.get());
    assertEquals(Int32.ONE, result.get(Int32.ONE));
    assertEquals(new Int32(3), result.size());
    try (Iter iter = result.iterate()) {
      iter.skip(Int32.ONE);
      Iter.Split split = iter.split(1, 2);
      try (Iter head = split.head) {
        assertEquals(new Int32(2), head.next());
        assertEquals(new Int32(3), head.next());
        assertNull(head.next());
      }
      if (split.tail != null) {
        split.tail.close();
      }
    }
    query.evaluate(backend);
    assertTrue(calls.get() > 3);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void repeatableUdfResultsRetainNativeCapabilitiesWithinTheirExecution(boolean dynamic) {
    BackendContext backend = new BackendContext(store);
    AtomicInteger pulls = new AtomicInteger();
    AtomicInteger sizes = new AtomicInteger();
    AtomicInteger gets = new AtomicInteger();
    AtomicInteger skips = new AtomicInteger();
    Sequence source = new LazySequence() {
      @Override
      public boolean isRepeatable() {
        assertNotNull(QueryExecution.current());
        return true;
      }

      @Override
      public IntNumeric knownSize() {
        assertNotNull(QueryExecution.current());
        return new Int32(3);
      }

      @Override
      public IntNumeric size() {
        assertNotNull(QueryExecution.current());
        sizes.incrementAndGet();
        return new Int32(3);
      }

      @Override
      public Item get(IntNumeric position) {
        assertNotNull(QueryExecution.current());
        gets.incrementAndGet();
        return position.cmp(Int32.ONE) >= 0 && position.cmp(new Int32(3)) <= 0 ? position : null;
      }

      @Override
      public Iter iterate() {
        assertNotNull(QueryExecution.current());
        return new BaseIter() {
          int position;

          @Override
          public Item next() {
            assertNotNull(QueryExecution.current());
            pulls.incrementAndGet();
            return ++position <= 3 ? new Int32(position) : null;
          }

          @Override
          public void skip(IntNumeric count) {
            assertNotNull(QueryExecution.current());
            skips.incrementAndGet();
            position += count.intValue();
          }

          @Override
          public void close() {
            assertNotNull(QueryExecution.current());
          }
        };
      }
    };
    Function producer = new AbstractFunction(new QNm("source"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
      @Override
      public Sequence execute(StaticContext sctx, QueryContext received, Sequence[] args) {
        assertSame(backend, received);
        return source;
      }
    };
    backend.bind(new QNm("source"), producer);
    String call = dynamic ? "(function() as item()* { local:inner() })()" : "local:inner()";
    Sequence result = new Query("declare variable $source external; "
        + "declare function local:inner() as item()* { $source() }; " + call).execute(backend);
    assertEquals(0, pulls.get());
    assertTrue(result.isRepeatable());
    assertEquals(new Int32(3), result.knownSize());
    assertEquals(new Int32(3), result.size());
    assertEquals(new Int32(2), result.get(new Int32(2)));
    assertEquals(1, sizes.get());
    assertEquals(1, gets.get());
    assertEquals(0, pulls.get());
    for (int pass = 0; pass < 2; pass++) {
      try (Iter iter = result.iterate()) {
        iter.skip(Int32.ONE);
        assertEquals(new Int32(2), iter.next());
        assertEquals(new Int32(3), iter.next());
        assertNull(iter.next());
      }
    }
    assertEquals(2, skips.get());
    assertNull(QueryExecution.current());
  }

  @Test
  void forkedTasksAndLazyResultsShareOnlyTheirOwnExecutionDefault() {
    BackendContext backend = new BackendContext(store);
    Query declarationQuery = new Query("declare context item as node() := <n/>; $$");
    DefaultCtxItem declaration = (DefaultCtxItem) declarationQuery.getModule().getVariables().resolve(Bits.FS_DOT);
    Function function = new AbstractFunction(new QNm("parallel-probe"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
      @Override
      public Sequence execute(StaticContext sctx, QueryContext received, Sequence[] args) {
        assertSame(backend, received);
        return new LazySequence() {
          @Override
          public Iter iterate() {
            return new BaseIter() {
              private int position;

              @Override
              public Item next() {
                if (++position > 2) {
                  return null;
                }
                Item[] item = new Item[1];
                Task task = new Task() {
                  @Override
                  protected void doCompute() {
                    assertSame(backend, received);
                    item[0] = declaration.evaluateToItem(received, new TupleImpl());
                  }
                };
                FJControl.submit(task).join();
                assertNull(task.getError());
                return item[0];
              }

              @Override
              public void close() {
              }
            };
          }
        };
      }
    };
    Query query = query(function, false);
    try (Iter first = query.execute(backend).iterate(); Iter second = query.execute(backend).iterate()) {
      Item firstNode = first.next();
      Item secondNode = second.next();
      assertNotSame(firstNode, secondNode);
      assertSame(firstNode, first.next());
      assertSame(secondNode, second.next());
    }
  }

  @Test
  void morselWorkersAndTheirLazyReturnsShareTheExecutionDefault() {
    BackendContext backend = new BackendContext(store);
    DefaultCtxItem declaration = (DefaultCtxItem) new Query("declare context item as node() := <n/>; $$")
        .getModule().getVariables().resolve(Bits.FS_DOT);
    AtomicInteger workers = new AtomicInteger();
    class SplitSource extends ItemSequence implements SplittableSequence {
      SplitSource() {
        super(Int32.ONE, new Int32(2), new Int32(3), new Int32(4));
      }

      @Override
      public int splitCount(int preferred) {
        return Math.min(2, preferred);
      }

      @Override
      public Sequence split(int index, int total) {
        assertEquals("brackit-morsel", Thread.currentThread().getName());
        assertNotNull(QueryExecution.current());
        workers.incrementAndGet();
        return new ItemSequence(items[index * 2], items[index * 2 + 1]);
      }
    }
    Function source = new AbstractFunction(new QNm("split-source"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
      @Override
      public Sequence execute(StaticContext sctx, QueryContext received, Sequence[] args) {
        assertSame(backend, received);
        return new SplitSource();
      }
    };
    Function result = new AbstractFunction(new QNm("morsel-default"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
      @Override
      public Sequence execute(StaticContext sctx, QueryContext received, Sequence[] args) {
        assertSame(backend, received);
        Item first = declaration.evaluateToItem(received, new TupleImpl());
        return new LazySequence() {
          @Override
          public Iter iterate() {
            return new BaseIter() {
              private boolean delivered;

              @Override
              public Item next() {
                assertSame(first, declaration.evaluateToItem(received, new TupleImpl()));
                if (delivered) {
                  return null;
                }
                delivered = true;
                return first;
              }

              @Override
              public void close() {
                assertSame(first, declaration.evaluateToItem(received, new TupleImpl()));
              }
            };
          }
        };
      }
    };
    SplitAwareExpr leaf = new SplitAwareExpr(new FunctionExpr(null, source));
    MainModule module = new MainModule();
    module.setExpr(new MorselPipeExpr(new ForBind(new Start(), leaf, false), new FunctionExpr(null, result), leaf));
    Query query = new Query(module);
    try (Iter first = query.execute(backend).iterate(); Iter second = query.execute(backend).iterate()) {
      Item firstNode = first.next();
      Item secondNode = second.next();
      assertNotSame(firstNode, secondNode);
      for (int row = 1; row < 4; row++) {
        assertSame(firstNode, first.next());
        assertSame(secondNode, second.next());
      }
      assertNull(first.next());
      assertNull(second.next());
    }
    assertEquals(4, workers.get());
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void workerEvaluatedImplicitContextPredicatesRetainTruthiness(boolean morsel) {
    Object binding = new Object();
    BoundVariable focus = new BoundVariable(new QNm("focus"), SequenceType.ITEM, binding);
    focus.setPos(0);
    Function stringLength = new Functions().resolve(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "string-length"), 0);
    Expr call = new DynamicFunctionExpr(null, stringLength, focus);
    Expr predicate;
    if (morsel) {
      class SplitSource extends ItemSequence implements SplittableSequence {
        SplitSource() {
          super(Int32.ONE);
        }

        @Override
        public int splitCount(int preferred) {
          return Math.min(2, preferred);
        }

        @Override
        public Sequence split(int index, int total) {
          return index == 0 ? new ItemSequence(Int32.ONE) : new ItemSequence();
        }
      }
      Function source = new AbstractFunction(new QNm("predicate-source"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
        @Override
        public Sequence execute(StaticContext sctx, QueryContext received, Sequence[] args) {
          return new SplitSource();
        }
      };
      SplitAwareExpr leaf = new SplitAwareExpr(new FunctionExpr(null, source));
      predicate = new MorselPipeExpr(new ForBind(new Start(), leaf, false), call, leaf);
    } else {
      predicate = new Expr() {
        @Override
        public Sequence evaluate(QueryContext received, Tuple tuple) {
          return new LazySequence() {
            @Override
            public Iter iterate() {
              return new BaseIter() {
                private boolean delivered;

                @Override
                public Item next() {
                  if (delivered) {
                    return null;
                  }
                  delivered = true;
                  Item[] result = new Item[1];
                  Task task = new Task() {
                    @Override
                    protected void doCompute() {
                      result[0] = call.evaluateToItem(received, tuple);
                    }
                  };
                  FJControl.submit(task).join();
                  assertNull(task.getError());
                  return result[0];
                }

                @Override
                public void close() {
                }
              };
            }
          };
        }

        @Override
        public Item evaluateToItem(QueryContext received, Tuple tuple) {
          return ExprUtil.asItem(evaluate(received, tuple));
        }

        @Override
        public boolean isUpdating() {
          return false;
        }

        @Override
        public boolean isVacuous() {
          return false;
        }
      };
    }
    ItemSequence expected = new ItemSequence(new Str("aa"), new Str("b"), new Str("ccc"));
    Function input = new AbstractFunction(new QNm("predicate-input"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
      @Override
      public Sequence execute(StaticContext sctx, QueryContext received, Sequence[] args) {
        return expected;
      }
    };
    MainModule module = new MainModule();
    module.setExpr(new FilterExpr(new FunctionExpr(null, input), new Expr[] { predicate }, new boolean[] { true },
        new boolean[] { false }, new boolean[] { false }, new boolean[] { false }, new Object[] { binding }));
    ResultChecker.dCheck(expected, new Query(module).execute(ctx));
  }
}
