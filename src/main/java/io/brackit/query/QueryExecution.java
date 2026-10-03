package io.brackit.query;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.expr.PredicateExpr;
import io.brackit.query.expr.Variable;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.sequence.AbstractSequence;

public final class QueryExecution {
  private static final ScopedValue<QueryExecution> CURRENT = ScopedValue.newInstance();

  private final QueryContext context;
  private Map<Variable, DeclarationValue> declarations;

  private static final class DeclarationValue {
    private Sequence value;
    private boolean initialized;

    synchronized Sequence get(Supplier<? extends Sequence> initializer) {
      if (!initialized) {
        value = initializer.get();
        initialized = true;
      }
      return value;
    }
  }

  QueryExecution(QueryContext context) {
    this.context = context;
  }

  public static QueryExecution current() {
    return CURRENT.isBound() ? CURRENT.get() : null;
  }

  public static ScopedValue.Carrier capture() {
    ScopedValue.Carrier scope = PredicateExpr.captureFocus();
    QueryExecution execution = current();
    return execution == null ? scope
        : scope == null ? ScopedValue.where(CURRENT, execution) : scope.where(CURRENT, execution);
  }

  public static Sequence resolveDeclaration(QueryContext context, Variable declaration,
                                            Supplier<? extends Sequence> initializer) {
    QueryExecution execution = current();
    return execution != null && execution.context == context
        ? execution.resolve(declaration, initializer)
        : initializer.get();
  }

  private Sequence resolve(Variable declaration, Supplier<? extends Sequence> initializer) {
    DeclarationValue value;
    synchronized (this) {
      if (declarations == null) {
        declarations = new IdentityHashMap<>(1);
      }
      value = declarations.computeIfAbsent(declaration, ignored -> new DeclarationValue());
    }
    return value.get(initializer);
  }

  public <T> T call(Supplier<T> operation) {
    return ScopedValue.where(CURRENT, this).call(operation::get);
  }

  public void run(Runnable operation) {
    ScopedValue.where(CURRENT, this).run(operation);
  }

  Sequence bind(Sequence sequence) {
    if (sequence == null || sequence instanceof Item) {
      return sequence;
    }
    return new AbstractSequence() {
      @Override
      public boolean booleanValue() {
        return call(sequence::booleanValue);
      }

      @Override
      public IntNumeric size() {
        return call(sequence::size);
      }

      @Override
      public Item get(IntNumeric position) {
        return call(() -> sequence.get(position));
      }

      @Override
      public Iter iterate() {
        return bind(call(sequence::iterate));
      }
    };
  }

  private Iter bind(Iter iterator) {
    return new Iter() {
      @Override
      public Item next() {
        return call(iterator::next);
      }

      @Override
      public void skip(IntNumeric count) {
        run(() -> iterator.skip(count));
      }

      @Override
      public Split split(int min, int max) {
        Split split = call(() -> iterator.split(min, max));
        return new Split(bind(split.head), split.tail == null ? null : bind(split.tail), split.serial);
      }

      @Override
      public void close() {
        run(iterator::close);
      }
    };
  }
}
