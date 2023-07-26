package org.brackit.query.jdm.node;

import org.brackit.query.jdm.Stream;

/**
 * Adds temporal methods.
 *
 * @param <E> the implementing class
 * @author Johannes Lichtenberger
 */
public interface TemporalNode<E extends TemporalNode<E>> extends Node<E> {
  /**
   * Get the same node in the next revision.
   *
   * @return same node in next revision
   */
  E getNext();

  /**
   * Get the same node in the previous revision.
   *
   * @return same node in previous revision
   */
  E getPrevious();

  /**
   * Get the same node in the first revision.
   *
   * @return same node in first revision
   */
  E getFirst();

  /**
   * Get the same node in the last revision.
   *
   * @return same node in last revision
   */
  E getLast();

  /**
   * Get a stream of the same node in all earlier revisions.
   *
   * @return stream of the same node in all earlier revisions
   */
  Stream<E> getEarlier(boolean includeSelf);

  /**
   * Get a stream of the same node in all future revisions.
   *
   * @return stream of the same node in all future revisions
   */
  Stream<E> getFuture(boolean includeSelf);

  /**
   * Get a stream of the same node in all revisions.
   *
   * @return stream of the same node in all revisions
   */
  Stream<E> getAllTime();

  boolean isNextOf(TemporalNode<?> other);

  boolean isPreviousOf(TemporalNode<?> other);

  boolean isFutureOf(TemporalNode<?> other);

  boolean isFutureOrSelfOf(TemporalNode<?> other);

  boolean isEarlierOf(TemporalNode<?> other);

  boolean isEarlierOrSelfOf(TemporalNode<?> other);

  boolean isLastOf(TemporalNode<?> other);

  boolean isFirstOf(TemporalNode<?> other);
}
