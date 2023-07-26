package org.brackit.query.jdm;

public interface StructuredItemStore {
  StructuredItemCollection<?> lookup(String name);

  StructuredItemCollection<?> create(String name);

  void drop(String name);

  void makeDir(String path);
}
