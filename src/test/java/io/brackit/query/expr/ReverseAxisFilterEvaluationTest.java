package io.brackit.query.expr;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.brackit.query.Query;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import io.brackit.query.function.AbstractFunction;
import io.brackit.query.QueryContext;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.module.StaticContext;
import io.brackit.query.sequence.ItemSequence;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.compiler.AST;
import io.brackit.query.compiler.Bits;
import io.brackit.query.compiler.CompileChain;
import io.brackit.query.compiler.translator.TopDownTranslator;
import io.brackit.query.compiler.translator.Translator;
import io.brackit.query.jdm.Stream;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Axis;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.jdm.type.NodeType;
import io.brackit.query.jdm.type.AnyNodeType;
import io.brackit.query.jdm.type.AttributeType;
import io.brackit.query.operator.TupleImpl;
import io.brackit.query.util.ExprUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class ReverseAxisFilterEvaluationTest extends XQueryBaseTest {
  private CompileChain observedAxes(AtomicInteger pulls, List<Node<?>> candidates) {
    return new CompileChain() {
      @Override
      protected Translator getTranslator(Map<QNm, Str> options) {
        return new TopDownTranslator(options) {
          @Override
          protected Accessor axis(AST ast) {
            Accessor delegate = super.axis(ast);
            if (delegate.getAxis().isForward()) {
              return delegate;
            }
            return new Accessor(delegate.getAxis()) {
              @Override
              public Stream<? extends Node<?>> performStep(Node<?> node) {
                return performStep(node, null);
              }

              @Override
              public Stream<? extends Node<?>> performStep(Node<?> node, NodeType test) {
                Stream<? extends Node<?>> stream = candidates == null
                    ? delegate.performStep(node, test)
                    : new Stream<Node<?>>() {
                      int position;

                      @Override
                      public Node<?> next() {
                        return position < candidates.size() ? candidates.get(position++) : null;
                      }

                      @Override
                      public void close() {
                      }
                    };
                return new Stream<Node<?>>() {
                  @Override
                  public Node<?> next() {
                    pulls.incrementAndGet();
                    return stream.next();
                  }

                  @Override
                  public void close() {
                    stream.close();
                  }
                };
              }
            };
          }
        };
      }
    };
  }

  @Test
  void existsStopsAtFirstMatchingPrecedingSibling() {
    Node<?> context = (Node<?>) ExprUtil.asItem(new Query("<r><a flag='yes'/>" + "<b/>".repeat(1000) + "<c/></r>/c")
                                                                                                                    .execute(ctx));
    ctx.setContextItem(context);
    AtomicInteger pulls = new AtomicInteger();
    Query query = new Query(observedAxes(pulls, null), "exists(preceding-sibling::*[?@flag])");
    ResultChecker.dCheck(Bool.TRUE, query.execute(ctx));
    assertEquals(1, pulls.get());
  }

  @ParameterizedTest
  @ValueSource(strings = { "preceding", "preceding-sibling", "ancestor", "ancestor-or-self", "past" })
  void guaranteedEbvFiltersReadOnlyTheFirstBackendAxisCandidate(String axis) {
    List<Node<?>> nodes = List.of((Node<?>) new Query("<a flag='yes'/>").execute(ctx),
                                  (Node<?>) new Query("<b/>").execute(ctx),
                                  (Node<?>) new Query("<c/>").execute(ctx));
    ctx.setContextItem(nodes.get(2));
    AtomicInteger pulls = new AtomicInteger();
    Query query = new Query(observedAxes(pulls, nodes), "exists(" + axis + "::*[?@flag])");
    ResultChecker.dCheck(Bool.TRUE, query.execute(ctx));
    assertEquals(1, pulls.get());
  }

  @ParameterizedTest
  @EnumSource(value = Axis.class, names = { "PAST_OR_SELF", "PREVIOUS", "LAST" })
  void temporalReverseStepApiPreservesEarlyExit(Axis axis) {
    Node<?> first = (Node<?>) new Query("<a flag='yes'/>").execute(ctx);
    AtomicInteger pulls = new AtomicInteger();
    Accessor cursor = new Accessor(axis) {
      @Override
      public Stream<? extends Node<?>> performStep(Node<?> node) {
        return new Stream<Node<?>>() {
          boolean delivered;

          @Override
          public Node<?> next() {
            pulls.incrementAndGet();
            if (delivered) {
              return null;
            }
            delivered = true;
            return first;
          }

          @Override
          public void close() {
          }
        };
      }
    };
    Expr predicate = new StepExpr(Accessor.ATTRIBUTE,
                                  new AttributeType(new QNm("flag")),
                                  new BoundVariable(Bits.FS_DOT, 0),
                                  new Expr[0],
                                  new boolean[0],
                                  new boolean[0],
                                  new boolean[0]);
    Expr step = new StepExpr(cursor,
                             AnyNodeType.ANY_NODE,
                             first,
                             new Expr[] { predicate },
                             new boolean[] { true },
                             new boolean[] { false },
                             new boolean[] { false },
                             new boolean[] { true },
                             new Object[1]);
    assertTrue(step.evaluate(ctx, TupleImpl.EMPTY_TUPLE).booleanValue());
    assertEquals(1, pulls.get());
  }

  @ParameterizedTest
  @ValueSource(strings = { "past", "past-or-self" })
  void temporalPredicatesRankNewestFirstCursorCandidates(String axis) {
    List<Node<?>> nodes = List.of((Node<?>) new Query("<r4 v='1'/>").execute(ctx),
                                  (Node<?>) new Query("<r3 v='1'/>").execute(ctx),
                                  (Node<?>) new Query("<r2 v='1'/>").execute(ctx),
                                  (Node<?>) new Query("<r1 v='1'/>").execute(ctx));
    boolean self = axis.equals("past-or-self");
    List<Node<?>> candidates = self ? nodes : nodes.subList(1, 4);
    ctx.setContextItem(nodes.getFirst());
    ctx.bind(new QNm("rank"), new AbstractFunction(new QNm("rank"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
      @Override
      public Sequence execute(StaticContext sctx, QueryContext context, Sequence[] args) {
        return new ItemSequence(new Int32(2));
      }
    });
    List<String> predicates = List.of("[1]",
                                      "[2]",
                                      "[$rank()]",
                                      "[@v + 0]",
                                      "[@v + 1]",
                                      "[position() le 2]",
                                      "[true()]");
    for (String predicate : predicates) {
      AtomicInteger pulls = new AtomicInteger();
      Query query = new Query(observedAxes(pulls, candidates),
                              "xquery version \"3.0\"; declare variable $rank external; " + axis + "::*" + predicate);
      List<Node<?>> expected = predicate.equals("[true()]")
          ? candidates
          : predicate.equals("[position() le 2]")
              ? candidates.subList(0, 2)
              : List.of(candidates.get(predicate.equals("[2]") || predicate.equals("[$rank()]") || predicate.equals(
                                                                                                                    "[@v + 1]")
                                                                                                                        ? 1
                                                                                                                        : 0));
      try (Iter result = query.execute(ctx).iterate()) {
        for (Node<?> node : expected) {
          assertSame(node, result.next(), axis + predicate);
        }
        assertNull(result.next(), axis + predicate);
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "?@flag and position() eq 1", "?@flag and last() eq 2", "?position() eq 1", "?$f()" })
  void positionalPredicatesKeepNearestSiblingOrder(String predicate) {
    Node<?> context = (Node<?>) ExprUtil.asItem(new Query("<r><a flag='yes'/><b flag='yes'/><c/></r>/c").execute(ctx));
    ctx.setContextItem(context);
    AtomicInteger pulls = new AtomicInteger();
    Query query = new Query(observedAxes(pulls, null),
                            "declare variable $f := function() { 1 }; preceding-sibling::*[" + predicate + "]");
    if (predicate.contains("last()")) {
      try (Iter result = query.execute(ctx).iterate()) {
        assertEquals(new QNm("a"), ((Node<?>) result.next()).getName());
        assertEquals(new QNm("b"), ((Node<?>) result.next()).getName());
        assertNull(result.next());
      }
    } else {
      Node<?> result = (Node<?>) ExprUtil.asItem(query.execute(ctx));
      assertEquals(new QNm("b"), result.getName());
    }
    assertTrue(pulls.get() >= 2);
  }
}
