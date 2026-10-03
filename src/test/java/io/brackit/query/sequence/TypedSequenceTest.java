package io.brackit.query.sequence;

import io.brackit.query.ErrorCode;
import io.brackit.query.BrackitQueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Str;
import io.brackit.query.atomic.Una;
import io.brackit.query.expr.ArrayAccessExpr;
import io.brackit.query.expr.SequenceExpr;
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
}
