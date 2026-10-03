package io.brackit.query.expr;

import java.util.ArrayList;
import java.util.List;

import io.brackit.query.ErrorCode;
import io.brackit.query.Query;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.QNm;
import io.brackit.query.function.AbstractFunction;
import io.brackit.query.function.FunctionExpr;
import io.brackit.query.jdm.Axis;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jdm.Stream;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.jdm.type.AnyNodeType;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.module.Functions;
import io.brackit.query.module.MainModule;
import io.brackit.query.module.Namespaces;
import io.brackit.query.module.StaticContext;
import io.brackit.query.operator.TupleImpl;
import io.brackit.query.sequence.AbstractSequence;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.ItemSequence;
import io.brackit.query.util.ExprUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PredicateReaderTest extends XQueryBaseTest {
  private enum Route {
    FILTER_INDEPENDENT, FILTER_SHARED, SCALAR_INDEPENDENT, SCALAR_SHARED,
    SPATIAL_INDEPENDENT, SPATIAL_SHARED, TEMPORAL_INDEPENDENT, TEMPORAL_SHARED
  }

  private static final class ReaderProbe extends AbstractSequence {
    int opens;
    int pulls;
    int closes;
    final Iter cursor;

    ReaderProbe(List<Item> values, boolean failAfterFirst) {
      cursor = new BaseIter() {
        int position;

        @Override
        public Item next() {
          pulls++;
          if (failAfterFirst && position > 0) {
            throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Predicate tail was read");
          }
          return position < values.size() ? values.get(position++) : null;
        }

        @Override
        public void close() {
          closes++;
        }
      };
    }

    @Override
    public Iter iterate() {
      opens++;
      assertEquals(1, opens, "Single-pass predicate reader reopened");
      return cursor;
    }
  }

  private Expr predicate(List<Item> values, boolean failAfterFirst, List<ReaderProbe> readers) {
    return new FunctionExpr(null, new AbstractFunction(new QNm("probe"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
      @Override
      public Sequence execute(StaticContext sctx, QueryContext context, Sequence[] args) {
        ReaderProbe reader = new ReaderProbe(values, failAfterFirst);
        readers.add(reader);
        return reader;
      }
    });
  }

  private Sequence evaluate(Route route, Expr predicate, boolean ebv) {
    boolean dependent = route.name().endsWith("SHARED");
    Expr expression;
    switch (route) {
      case FILTER_INDEPENDENT, FILTER_SHARED, SCALAR_INDEPENDENT, SCALAR_SHARED -> {
        Sequence input = route.name().startsWith("SCALAR") ? new Int32(10) : new ItemSequence(new Int32(10), new Int32(20));
        expression = new FilterExpr(input, new Expr[] { predicate }, new boolean[] { dependent },
                                    new boolean[] { false }, new boolean[] { false }, new boolean[] { ebv });
      }
      default -> {
        Node<?> first = ctx.getNodeFactory().element(new QNm("first"));
        Node<?> second = ctx.getNodeFactory().element(new QNm("second"));
        Axis axis = route.name().startsWith("SPATIAL") ? Axis.PRECEDING_SIBLING : Axis.PAST;
        Accessor accessor = new Accessor(axis) {
          @Override
          public Stream<? extends Node<?>> performStep(Node<?> node) {
            return new Stream<Node<?>>() {
              int position;

              @Override
              public Node<?> next() {
                return switch (position++) {
                  case 0 -> first;
                  case 1 -> second;
                  default -> null;
                };
              }

              @Override
              public void close() {
              }
            };
          }
        };
        expression = new StepExpr(accessor, AnyNodeType.ANY_NODE, first, new Expr[] { predicate },
                                  new boolean[] { dependent }, new boolean[] { false }, new boolean[] { false },
                                  new boolean[] { ebv }, new Object[1]);
      }
    }
    if (route.name().startsWith("SCALAR")) {
      return expression.evaluateToItem(ctx, TupleImpl.EMPTY_TUPLE);
    }
    MainModule module = new MainModule();
    module.setExpr(expression);
    return new Query(module).execute(ctx);
  }

  private List<String> values(Sequence sequence) {
    List<String> values = new ArrayList<>();
    if (sequence != null) {
      try (Iter iter = sequence.iterate()) {
        Item item;
        while ((item = iter.next()) != null) {
          values.add(item instanceof Node<?> node ? node.getName().stringValue() : item.atomize().stringValue());
        }
      }
    }
    return values;
  }

  @ParameterizedTest
  @EnumSource(Route.class)
  void oneCursorBooleanResultsAreNotReopened(Route route) {
    List<ReaderProbe> readers = new ArrayList<>();
    List<String> expected = route.name().startsWith("SCALAR") ? List.of("10")
        : route.name().contains("FILTER") ? List.of("10", "20") : List.of("first", "second");
    assertEquals(expected, values(evaluate(route, predicate(List.of(Bool.TRUE), false, readers), false)));
    assertFalse(readers.isEmpty());
    for (ReaderProbe reader : readers) {
      assertEquals(1, reader.opens);
      assertEquals(2, reader.pulls);
      assertEquals(1, reader.closes);
    }
  }

  @ParameterizedTest
  @EnumSource(Route.class)
  void nodeFirstValuesCloseAfterOnePullWithoutReadingTheTail(Route route) {
    Item node = ctx.getNodeFactory().element(new QNm("predicate-node"));
    List<String> expected = route.name().startsWith("SCALAR") ? List.of("10")
        : route.name().contains("FILTER") ? List.of("10", "20") : List.of("first", "second");
    for (boolean ebv : new boolean[] { false, true }) {
      List<ReaderProbe> readers = new ArrayList<>();
      assertEquals(expected, values(evaluate(route, predicate(List.of(node), true, readers), ebv)));
      for (ReaderProbe reader : readers) {
        assertEquals(1, reader.opens);
        assertEquals(1, reader.pulls);
        assertEquals(1, reader.closes);
      }
    }
  }

  @ParameterizedTest
  @EnumSource(Route.class)
  void multipleAtomicValuesRetainForg0006AndCloseTheReader(Route route) {
    List<ReaderProbe> readers = new ArrayList<>();
    QueryException error = assertThrows(QueryException.class,
        () -> values(evaluate(route, predicate(List.of(Bool.TRUE, Bool.FALSE), false, readers), false)));
    assertEquals(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, error.getCode());
    assertEquals(1, readers.size());
    assertEquals(2, readers.getFirst().pulls);
    assertEquals(1, readers.getFirst().opens);
    assertEquals(1, readers.getFirst().closes);
  }

  @ParameterizedTest
  @EnumSource(Route.class)
  void singletonNumericSequencesRetainPositionalRanking(Route route) {
    List<ReaderProbe> readers = new ArrayList<>();
    List<String> expected = route.name().startsWith("SCALAR") ? List.of()
        : route.name().contains("FILTER") ? List.of("20")
        : route.name().startsWith("SPATIAL") ? List.of("first") : List.of("second");
    assertEquals(expected, values(evaluate(route, predicate(List.of(new Int32(2)), false, readers), false)));
    for (ReaderProbe reader : readers) {
      assertEquals(1, reader.opens);
      assertEquals(2, reader.pulls);
      assertEquals(1, reader.closes);
    }
    readers.clear();
    assertEquals(List.of(), values(evaluate(route, predicate(List.of(new Int32(99)), false, readers), false)));
  }

  @ParameterizedTest
  @ValueSource(strings = { "<n/>", "(<a/>,<b/>)", "<r><a/><b/></r>/child::*", "<r><a/><b/><c/></r>/c/preceding-sibling::*" })
  void actualRootFunctionReturnsAFirstNodeBeforeTheLazyError(String input) {
    ctx.bind(new QNm("f"), new Functions().resolve(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "root"), 0));
    String filter = input + "[?(for $i in (1,2) return if ($i eq 1) then $f() else error())]";
    ResultChecker.dCheck(Bool.TRUE, new Query("declare variable $f external; exists(" + filter + ")").execute(ctx));
    if (input.equals("<n/>")) {
      assertEquals(new QNm("n"), ((Node<?>) new Query("declare variable $f external; " + filter)
          .getModule().getBody().evaluateToItem(ctx, TupleImpl.EMPTY_TUPLE)).getName());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "item()", "item()?" })
  void writtenSingletonTypesStillValidateBeforeExposingTheFirstNode(String type) {
    ctx.bind(new QNm("f"), new AbstractFunction(new QNm("nodes"), new Signature(SequenceType.ITEM_SEQUENCE), true) {
      @Override
      public Sequence execute(StaticContext sctx, QueryContext context, Sequence[] args) {
        return new ItemSequence(ctx.getNodeFactory().element(new QNm("a")), ctx.getNodeFactory().element(new QNm("b")));
      }
    });
    for (String input : List.of("1", "(1,2)", "<r><a/><b/></r>/child::*", "<r><a/><b/><c/></r>/c/preceding-sibling::*")) {
      QueryException error = assertThrows(QueryException.class, () -> ExprUtil.asItem(new Query(
          "declare variable $f external; exists(" + input + "[?($f() treat as " + type + ")])").execute(ctx)));
      assertEquals(ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE, error.getCode());
    }
  }

  @Test
  void independentNumericSequencePredicatesDoNotSkipLaterFilters() {
    List<ReaderProbe> readers = new ArrayList<>();
    Expr predicate = predicate(List.of(new Int32(2)), false, readers);
    FilterExpr filter = new FilterExpr(new ItemSequence(new Int32(10), new Int32(20)),
        new Expr[] { predicate, Bool.FALSE }, new boolean[2], new boolean[2], new boolean[2]);
    assertNull(filter.evaluate(ctx, TupleImpl.EMPTY_TUPLE));
    Node<?> node = ctx.getNodeFactory().element(new QNm("n"));
    StepExpr step = new StepExpr(Accessor.ANCESTOR_OR_SELF, AnyNodeType.ANY_NODE, node,
        new Expr[] { predicate(List.of(Int32.ONE), false, readers), Bool.FALSE },
        new boolean[2], new boolean[2], new boolean[2]);
    assertNull(step.evaluate(ctx, TupleImpl.EMPTY_TUPLE));
  }
}
