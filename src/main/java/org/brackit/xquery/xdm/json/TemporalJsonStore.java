package org.brackit.xquery.xdm.json;

import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.StructuredItemStore;

public interface TemporalJsonStore extends StructuredItemStore {
  @Override
  public TemporalJsonCollection<?> lookup(String name);

  @Override
  public TemporalJsonCollection<?> create(String name);

  public TemporalJsonCollection<?> create(String name, SubtreeParser parser);

  public TemporalJsonCollection<?> create(String name, Stream<SubtreeParser> parsers);

  @Override
  public void drop(String name);

  @Override
  public void makeDir(String path);
}
