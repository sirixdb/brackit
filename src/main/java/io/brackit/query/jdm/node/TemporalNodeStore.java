package io.brackit.query.jdm.node;

import io.brackit.query.node.parser.NodeSubtreeParser;
import io.brackit.query.jdm.Stream;
import io.brackit.query.jdm.StructuredItemStore;

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
