package org.brackit.xquery.xdm.json;

import org.brackit.xquery.xdm.Stream;

/**
 * Adds temporal methods.
 *
 * @param <E> the implementing class
 * @author Johannes Lichtenberger
 */
public interface TemporalJsonItem<E extends TemporalJsonItem<E>> extends JsonItem {
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
  Stream<E> getAllTimes();

  boolean isNextOf(E other);

  boolean isPreviousOf(E other);

  boolean isFutureOf(E other);

  boolean isFutureOrSelfOf(E other);

  boolean isEarlierOf(E other);

  boolean isEarlierOrSelfOf(E other);

  boolean isLastOf(E other);

  boolean isFirstOf(E other);
}
