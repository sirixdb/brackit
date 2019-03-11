package org.brackit.xquery.xdm;

import java.time.Instant;

public interface TemporalCollection<E extends Node<E>> extends Collection<E> {

  public E getDocument(int revision);

  public E getDocument(Instant pointInTime);

  public E getDocument(String name, int revision);

  public E getDocument(String name, Instant pointInTime);

  public E getDocument(String name);
}
