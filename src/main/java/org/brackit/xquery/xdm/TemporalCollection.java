package org.brackit.xquery.xdm;

import java.time.Instant;

public interface TemporalCollection<E extends Node<E>> extends Collection<E> {
  public E getDocument(int revision) throws DocumentException;

  public E getDocument(Instant pointInTime) throws DocumentException;

  public E getDocument(int revision, boolean updatable) throws DocumentException;

  public E getDocument(Instant pointInTime, boolean updatable) throws DocumentException;

  public E getDocument(int revision, String name) throws DocumentException;

  public E getDocument(Instant pointInTime, String name) throws DocumentException;

  public E getDocument(int revision, String name, boolean updatable) throws DocumentException;

  public E getDocument(Instant pointInTime, String name, boolean updatable)
      throws DocumentException;

  public E getDocument(String name) throws DocumentException;
}
