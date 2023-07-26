package io.brackit.query.atomic;

public interface InternalAtomic {
  int atomicCmpInternal(Atomic atomic);
}
