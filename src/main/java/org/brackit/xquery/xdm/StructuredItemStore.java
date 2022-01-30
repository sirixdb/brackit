package org.brackit.xquery.xdm;

public interface StructuredItemStore {
  StructuredItemCollection<?> lookup(String name);

  StructuredItemCollection<?> create(String name);

  void drop(String name);

  void makeDir(String path);
}
