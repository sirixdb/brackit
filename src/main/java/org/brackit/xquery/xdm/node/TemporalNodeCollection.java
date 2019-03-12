package org.brackit.xquery.xdm.node;

import java.time.Instant;

public interface TemporalNodeCollection<E extends Node<E>> extends NodeCollection<E> {

  public E getDocument(int revision);

  public E getDocument(Instant pointInTime);

  public E getDocument(String name, int revision);

  public E getDocument(String name, Instant pointInTime);

  public E getDocument(String name);
}
