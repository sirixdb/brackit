package org.brackit.xquery.jdm.json;

import org.brackit.xquery.node.parser.NodeSubtreeParser;
import org.brackit.xquery.jdm.Stream;
import org.brackit.xquery.jdm.StructuredItemStore;

public interface TemporalJsonStore extends StructuredItemStore {
  @Override
  public TemporalJsonCollection<?> lookup(String name);

  @Override
  public TemporalJsonCollection<?> create(String name);

  public TemporalJsonCollection<?> create(String name, NodeSubtreeParser parser);

  public TemporalJsonCollection<?> create(String name, Stream<NodeSubtreeParser> parsers);

  @Override
  public void drop(String name);

  @Override
  public void makeDir(String path);
}
