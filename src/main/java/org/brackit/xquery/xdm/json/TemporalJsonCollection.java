package org.brackit.xquery.xdm.json;

import java.time.Instant;

public interface TemporalJsonCollection<E extends JsonItem> extends JsonCollection<E> {

  public E getDocument(int revision);

  public E getDocument(Instant pointInTime);

  public E getDocument(String name, int revision);

  public E getDocument(String name, Instant pointInTime);

  public E getDocument(String name);
}
