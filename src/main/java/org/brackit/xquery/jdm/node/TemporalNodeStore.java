package org.brackit.xquery.jdm.node;

import org.brackit.xquery.node.parser.NodeSubtreeParser;
import org.brackit.xquery.jdm.Stream;
import org.brackit.xquery.jdm.StructuredItemStore;

public interface TemporalNodeStore extends StructuredItemStore {
  @Override
  public TemporalNodeCollection<?> lookup(String name);

  @Override
  public TemporalNodeCollection<?> create(String name);

  public TemporalNodeCollection<?> create(String name, NodeSubtreeParser parser);

  public TemporalNodeCollection<?> create(String name, Stream<NodeSubtreeParser> parsers);

  @Override
  public void drop(String name);

  @Override
  public void makeDir(String path);
}
