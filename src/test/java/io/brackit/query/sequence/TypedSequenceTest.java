package io.brackit.query.sequence;

import io.brackit.query.ErrorCode;
import io.brackit.query.BrackitQueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Query;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Str;
import io.brackit.query.atomic.Una;
import io.brackit.query.expr.ArrayAccessExpr;
import io.brackit.query.expr.SequenceExpr;
import io.brackit.query.expr.Treat;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.type.AnyItemType;
import io.brackit.query.jdm.type.AtomicType;
import io.brackit.query.jdm.type.Cardinality;
import io.brackit.query.jdm.type.NumericType;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.jsonitem.array.DArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypedSequenceTest {
  private static SequenceType type(Cardinality cardinality) {
    return new SequenceType(AtomicType.INR, cardinality);
  }

  private static void typeError(org.junit.jupiter.api.function.Executable action) {
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, assertThrows(QueryException.class, action).getCode());
  }

  @ParameterizedTest
  @EnumSource(value = Cardinality.class, names = { "One", "ZeroOrOne", "Zero" })
  void rejectsExtraItemsBeforeReturningFirst(Cardinality card) {
    Sequence typed = TypedSequence.toTypedSequence(type(card), new ItemSequence(Int32.ONE, new Int32(2)));
    typeError(() -> {
      try (Iter it = typed.iterate()) {
        it.next();
      }
    });
    typeError(() -> typed.get(Int32.ONE));
    typeError(() -> typed.get(new Int32(3)));
  }

  @ParameterizedTest
  @EnumSource(value = Cardinality.class, names = { "One", "ZeroOrOne" })
  void functionConversionRejectsExtraItems(Cardinality card) {
    typeError(() -> FunctionConversionSequence.asTypedSequence(type(card),
                                                               new ItemSequence(Int32.ONE, new Int32(2)),
                                                               false));
    typeError(() -> FunctionConversionSequence.asTypedSequence(new SequenceType(AnyItemType.ANY, card),
                                                               new ItemSequence(Int32.ONE, new Int32(2)),
                                                               false));
  }

  @Test
  void skipStillChecksTypesAndCardinality() {
    Sequence typed = new TypedSequence(type(Cardinality.One), new ItemSequence(Int32.ONE, new Int32(2)));
    typeError(() -> {
      try (Iter it = typed.iterate()) {
        it.skip(Int32.ONE);
      }
    });
    Sequence invalid = new TypedSequence(type(Cardinality.ZeroOrMany), new ItemSequence(new Str("bad"), Int32.ONE));
    typeError(() -> {
      try (Iter it = invalid.iterate()) {
        it.skip(Int32.ONE);
        it.next();
      }
    });
    typeError(() -> invalid.get(new Int32(2)));
  }

  @Test
  void eachTraversalValidatesAgain() {
    Item[] items = { Int32.ONE };
    Sequence typed = new TypedSequence(type(Cardinality.OneOrMany), new ItemSequence(items));
    try (Iter it = typed.iterate()) {
      assertEquals(Int32.ONE, it.next());
      assertNull(it.next());
    }
    items[0] = new Str("bad");
    typeError(() -> {
      try (Iter it = typed.iterate()) {
        it.next();
      }
    });
    typeError(() -> typed.get(Int32.ONE));
  }

  @ParameterizedTest
  @EnumSource(value = Cardinality.class, names = { "One", "ZeroOrOne" })
  void singletonCanBeReadRepeatedlyAndOutOfRangeIsEmpty(Cardinality card) {
    Sequence typed = new TypedSequence(type(card), new ItemSequence(Int32.ONE));
    for (int i = 0; i < 2; i++) {
      try (Iter it = typed.iterate()) {
        assertEquals(Int32.ONE, it.next());
        assertNull(it.next());
        assertNull(it.next());
      }
      assertEquals(Int32.ONE, typed.get(Int32.ONE));
      assertNull(typed.get(new Int32(2)));
      assertNull(typed.get(Int32.ZERO));
    }
  }

  @Test
  void requiredEmptySequenceFailsOnAccess() {
    Sequence typed = new TypedSequence(type(Cardinality.One), new ItemSequence());
    typeError(() -> typed.get(Int32.ONE));
    typeError(() -> {
      try (Iter it = typed.iterate()) {
        it.next();
      }
    });
    assertNull(new TypedSequence(type(Cardinality.ZeroOrOne), new ItemSequence()).get(Int32.ONE));
  }

  @Test
  void emptyTypeRejectsItemsOnAllConversionPaths() {
    Sequence item = Int32.ONE;
    typeError(() -> TypedSequence.toTypedItem(SequenceType.EMPTY_SEQUENCE, item));
    typeError(() -> TypedSequence.toTypedItem(SequenceType.EMPTY_SEQUENCE, Int32.ONE));
    typeError(() -> FunctionConversionSequence.asTypedSequence(SequenceType.EMPTY_SEQUENCE, item, false));
    typeError(() -> FunctionConversionSequence.asTypedSequence(new SequenceType(AtomicType.INR, Cardinality.Zero),
                                                               item,
                                                               true));
    typeError(() -> FunctionConversionSequence.asTypedSequence(new SequenceType(AnyItemType.ANY, Cardinality.Zero),
                                                               item,
                                                               true));
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void getValidatesTheCurrentSourceAfterAnEmptyRead(boolean cacheSize) {
    DArray array = new DArray(List.of());
    Sequence unboxed = new ArrayAccessExpr(array, new SequenceExpr()).evaluate(new BrackitQueryContext(), null);
    Sequence typed = new TypedSequence(type(Cardinality.ZeroOrOne), unboxed);
    if (cacheSize) {
      assertEquals(Int32.ZERO, typed.size());
    } else {
      assertNull(typed.get(Int32.ONE));
    }
    array.append(Int32.ONE);
    assertEquals(Int32.ONE, typed.get(Int32.ONE));
    assertNull(typed.get(new Int32(2)));
    array.replaceAt(0, new Str("bad"));
    typeError(() -> typed.get(Int32.ONE));
    array.replaceAt(0, Int32.ONE);
    array.append(new Int32(2));
    typeError(() -> typed.get(Int32.ONE));
    typeError(() -> typed.get(new Int32(3)));
  }

  @Test
  void getChecksCardinalityAfterASizeReadOfANonemptySource() {
    DArray array = new DArray(List.of(Int32.ONE));
    Sequence unboxed = new ArrayAccessExpr(array, new SequenceExpr()).evaluate(new BrackitQueryContext(), null);
    Sequence typed = new TypedSequence(type(Cardinality.One), unboxed);
    assertEquals(Int32.ONE, typed.size());
    array.append(new Int32(2));
    typeError(() -> typed.get(new Int32(3)));
    typeError(() -> typed.get(Int32.ONE));
  }

  @Test
  void numericConversionUsesTheDeclaredTargetOnEveryRead() {
    for (AtomicType itemType : List.of(AtomicType.INR, AtomicType.INT, AtomicType.DEC, AtomicType.DBL, NumericType.INSTANCE)) {
      for (boolean builtin : List.of(false, true)) {
        SequenceType singleton = new SequenceType(itemType, Cardinality.One);
        Sequence item = FunctionConversionSequence.asTypedSequence(singleton, new Una("1"), builtin);
        Sequence sequence = FunctionConversionSequence.asTypedSequence(singleton, new ItemSequence(new Una("1")), builtin);
        assertEquals(itemType.getType(), assertInstanceOf(Atomic.class, item).type());
        assertEquals(itemType.getType(), assertInstanceOf(Atomic.class, sequence).type());

        Sequence many = FunctionConversionSequence.asTypedSequence(new SequenceType(itemType, Cardinality.ZeroOrMany),
                                                                   new ItemSequence(new Una("1"), new Una("2")),
                                                                   builtin);
        for (int traversal = 0; traversal < 2; traversal++) {
          try (Iter it = many.iterate()) {
            assertEquals(itemType.getType(), assertInstanceOf(Atomic.class, it.next()).type());
            assertEquals(itemType.getType(), assertInstanceOf(Atomic.class, it.next()).type());
            assertNull(it.next());
          }
          assertEquals(itemType.getType(), assertInstanceOf(Atomic.class, many.get(new Int32(2))).type());
          assertNull(many.get(new Int32(3)));
          try (Iter it = many.iterate()) {
            it.skip(Int32.ONE);
            assertEquals(itemType.getType(), assertInstanceOf(Atomic.class, it.next()).type());
          }
        }
      }
    }
    Sequence invalid = FunctionConversionSequence.asTypedSequence(type(Cardinality.ZeroOrMany),
                                                                  new ItemSequence(new Str("bad"), new Una("1")),
                                                                  true);
    typeError(() -> invalid.get(new Int32(2)));
    typeError(() -> {
      try (Iter it = invalid.iterate()) {
        it.skip(Int32.ONE);
      }
    });
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void emptyConversionIsEagerForEverySequenceRepresentation(boolean builtin) {
    for (SequenceType empty : List.of(SequenceType.EMPTY_SEQUENCE, type(Cardinality.Zero))) {
      for (Sequence source : List.of(Int32.ONE, new ItemSequence(Int32.ONE), new NestedSequence(Int32.ONE))) {
        typeError(() -> FunctionConversionSequence.asTypedSequence(empty, source, builtin));
      }
      assertNull(FunctionConversionSequence.asTypedSequence(empty, null, builtin));
      assertNull(FunctionConversionSequence.asTypedSequence(empty, new ItemSequence(), builtin));
      assertNull(FunctionConversionSequence.asTypedSequence(empty, new NestedSequence(), builtin));
    }
  }

  @ParameterizedTest
  @EnumSource(Cardinality.class)
  void aggregateReadsRevalidateMutableSources(Cardinality card) {
    for (boolean conversion : List.of(false, true)) {
      DArray array = new DArray(card == Cardinality.Zero ? List.of() : List.of(Int32.ONE));
      Sequence unboxed = new ArrayAccessExpr(array, new SequenceExpr()).evaluate(new BrackitQueryContext(), null);
      Sequence typed = conversion
          ? new FunctionConversionSequence(type(card), unboxed, false)
          : new TypedSequence(type(card), unboxed);
      assertEquals(card == Cardinality.Zero ? Int32.ZERO : Int32.ONE, typed.size());
      assertEquals(card != Cardinality.Zero, typed.booleanValue());

      if (card == Cardinality.Zero) {
        array.append(Int32.ONE);
      } else {
        array.replaceAt(0, new Str("bad"));
      }
      typeError(typed::size);
      typeError(typed::booleanValue);

      if (card == Cardinality.Zero) {
        array.remove(0);
        assertEquals(Int32.ZERO, typed.size());
        assertFalse(typed.booleanValue());
        continue;
      }
      array.replaceAt(0, Int32.ZERO);
      assertEquals(Int32.ONE, typed.size());
      assertFalse(typed.booleanValue());
      array.append(Int32.ONE);
      if (card.many()) {
        assertEquals(new Int32(2), typed.size());
        QueryException error = assertThrows(QueryException.class, typed::booleanValue);
        assertEquals(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, error.getCode());
      } else {
        typeError(typed::size);
        typeError(typed::booleanValue);
      }
      array.remove(1);
      array.remove(0);
      if (card.moreThanZero()) {
        typeError(typed::size);
        typeError(typed::booleanValue);
      } else {
        assertEquals(Int32.ZERO, typed.size());
        assertFalse(typed.booleanValue());
      }
    }
  }

  @Test
  void optionalAggregatesReflectGrowthAfterAnEmptyRead() {
    for (boolean conversion : List.of(false, true)) {
      DArray array = new DArray(List.of());
      Sequence unboxed = new ArrayAccessExpr(array, new SequenceExpr()).evaluate(new BrackitQueryContext(), null);
      Sequence typed = conversion
          ? new FunctionConversionSequence(type(Cardinality.ZeroOrOne), unboxed, false)
          : new TypedSequence(type(Cardinality.ZeroOrOne), unboxed);
      assertEquals(Int32.ZERO, typed.size());
      assertFalse(typed.booleanValue());
      array.append(Int32.ONE);
      assertEquals(Int32.ONE, typed.size());
      assertTrue(typed.booleanValue());
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cardinality.class, names = { "One", "ZeroOrOne" })
  void nodeBooleanValuesCannotBypassSingletonChecks(Cardinality card) {
    BrackitQueryContext ctx = new BrackitQueryContext();
    Item node = (Item) new Query("<n/>").execute(ctx);
    for (boolean conversion : List.of(false, true)) {
      DArray array = new DArray(List.of(node));
      Sequence unboxed = new ArrayAccessExpr(array, new SequenceExpr()).evaluate(ctx, null);
      SequenceType expected = new SequenceType(AnyItemType.ANY, card);
      Sequence typed = conversion
          ? new FunctionConversionSequence(expected, unboxed, false)
          : new TypedSequence(expected, unboxed);
      assertEquals(Int32.ONE, typed.size());
      assertTrue(typed.booleanValue());
      array.append(node);
      typeError(typed::booleanValue);
      typeError(typed::size);
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cardinality.class, names = { "One", "ZeroOrOne" })
  void convertedSingletonsKeepValidAggregateAndIteratorResults(Cardinality card) {
    for (SequenceType expected : List.of(type(card), new SequenceType(AnyItemType.ANY, card))) {
      Sequence converted = new FunctionConversionSequence(expected, new ItemSequence(Int32.ONE), false);
      for (int traversal = 0; traversal < 2; traversal++) {
        assertEquals(Int32.ONE, converted.size());
        assertTrue(converted.booleanValue());
        try (Iter it = converted.iterate()) {
          assertEquals(Int32.ONE, it.next());
          assertNull(it.next());
          assertNull(it.next());
        }
      }
    }
  }

  @Test
  void treatAggregateReadsKeepTheirErrorCodeAfterMutation() {
    BrackitQueryContext ctx = new BrackitQueryContext();
    DArray array = new DArray(List.of(Int32.ONE));
    Sequence unboxed = new ArrayAccessExpr(array, new SequenceExpr()).evaluate(ctx, null);
    Sequence treated = new Treat(unboxed, type(Cardinality.ZeroOrOne)).evaluate(ctx, null);
    assertEquals(Int32.ONE, treated.size());
    assertTrue(treated.booleanValue());
    array.replaceAt(0, new Str("bad"));
    assertEquals(ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE,
                 assertThrows(QueryException.class, treated::size).getCode());
    assertEquals(ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE,
                 assertThrows(QueryException.class, treated::booleanValue).getCode());
    array.replaceAt(0, Int32.ZERO);
    assertEquals(Int32.ONE, treated.size());
    assertFalse(treated.booleanValue());
  }
}
