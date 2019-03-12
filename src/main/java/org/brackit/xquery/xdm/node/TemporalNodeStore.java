package org.brackit.xquery.xdm.node;

import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.StructuredItemStore;

public interface TemporalNodeStore extends StructuredItemStore {
  @Override
  public TemporalNodeCollection<?> lookup(String name);

  @Override
  public TemporalNodeCollection<?> create(String name);

  public TemporalNodeCollection<?> create(String name, SubtreeParser parser);

  public TemporalNodeCollection<?> create(String name, Stream<SubtreeParser> parsers);

  @Override
  public void drop(String name);

  @Override
  public void makeDir(String path);
}
