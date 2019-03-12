package org.brackit.xquery.xdm.json;

import org.brackit.xquery.xdm.Stream;

/**
 * Adds temporal methods.
 *
 * @author Johannes Lichtenberger
 *
 * @param <E> the implementing class
 */
public interface TemporalJsonItem extends JsonItem {
  /**
   * Get the same node in the next revision.
   *
   * @return same node in next revision
   */
  TemporalJsonItem getNext();

  /**
   * Get the same node in the previous revision.
   *
   * @return same node in previous revision
   */
  TemporalJsonItem getPrevious();

  /**
   * Get the same node in the first revision.
   *
   * @return same node in first revision
   */
  TemporalJsonItem getFirst();

  /**
   * Get the same node in the last revision.
   *
   * @return same node in last revision
   */
  TemporalJsonItem getLast();

  /**
   * Get a stream of the same node in all earlier revisions.
   *
   * @return stream of the same node in all earlier revisions
   */
  Stream<TemporalJsonItem> getEarlier(boolean includeSelf);

  /**
   * Get a stream of the same node in all future revisions.
   *
   * @return stream of the same node in all future revisions
   */
  Stream<TemporalJsonItem> getFuture(boolean includeSelf);

  /**
   * Get a stream of the same node in all revisions.
   *
   * @return stream of the same node in all revisions
   */
  Stream<TemporalJsonItem> getAllTime();

  boolean isNextOf(TemporalJsonItem other);

  boolean isPreviousOf(TemporalJsonItem other);

  boolean isFutureOf(TemporalJsonItem other);

  boolean isFutureOrSelfOf(TemporalJsonItem other);

  boolean isEarlierOf(TemporalJsonItem other);

  boolean isEarlierOrSelfOf(TemporalJsonItem other);

  boolean isLastOf(TemporalJsonItem other);

  boolean isFirstOf(TemporalJsonItem other);
}
