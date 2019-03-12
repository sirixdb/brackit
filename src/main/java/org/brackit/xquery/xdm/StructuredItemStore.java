package org.brackit.xquery.xdm;

public interface StructuredItemStore {
  public StructuredItemCollection<?> lookup(String name);

  public StructuredItemCollection<?> create(String name);

  public void drop(String name);

  public void makeDir(String path);
}
