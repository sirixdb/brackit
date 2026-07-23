/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer;

import io.brackit.query.atomic.QNm;

/**
 * Immutable identity of the document a vectorized scan reads from, lifted from the loop variable's
 * source expression by {@link io.brackit.query.compiler.optimizer.walker.topdown.VectorizedGroupByDetection}
 * and carried on the annotated {@code PipeExpr} via {@link VectorizedScanAnnotation#SOURCE_REF}.
 *
 * <p>{@link VectorizedScanAnnotation#SOURCE_PATH_PREFIX} tells an executor <em>which path</em> inside
 * a document a scan walks, but never <em>which document</em> it dereferences. A {@link VectorizedExecutor}
 * is typically bound to a single physical resource/revision (e.g. SirixDB's
 * {@code SirixVectorizedExecutor}), so a same-shaped query over a <em>different</em> document would be
 * answered with the bound resource's data — wrong results. This ref closes that gap: the executor
 * inspects it in {@link VectorizedExecutor#acceptsSource(SourceRef)} and declines a scan it is not
 * bound to serve, so the translator falls back to the generic (always-correct) pipeline.
 *
 * <p>Four kinds, matching what the detection can prove from the AST:
 * <ul>
 * <li>{@link Kind#DOCUMENT} — the scan opens a concrete {@code jn:doc}/{@code jn:open} with literal
 * database/resource arguments; {@link #databaseName()}, {@link #resourceName()} and {@link #revision()}
 * are populated ({@code revision == } {@link #LATEST_REVISION} when the call names no explicit revision,
 * i.e. it opens the most-recent one).</li>
 * <li>{@link Kind#CONTEXT_ITEM} — the scan ranges over the query's context item (the caller's own bound
 * read transaction); no database/resource is named.</li>
 * <li>{@link Kind#VARIABLE} — the scan ranges over a variable with no resolvable binding inside the
 * query tree (typically {@code declare variable $doc external}, bound at execution time);
 * {@link #variableName()} is populated. Compile-time gates should fail closed, but an executor can
 * verify the actual runtime binding via
 * {@link VectorizedExecutor#acceptsSource(SourceRef, io.brackit.query.QueryContext)}.</li>
 * <li>{@link Kind#UNKNOWN} — the source could not be proven to be a single concrete document (a dynamic
 * {@code jn:doc}, a collection/multi-revision opener, or a non-document source). A resource-bound
 * executor should fail closed on this.</li>
 * </ul>
 */
public final class SourceRef {

  /** Sentinel {@link #revision()} value: the source names no explicit revision (opens the latest). */
  public static final int LATEST_REVISION = -1;

  /** What the optimizer could prove about the scan's source document. */
  public enum Kind {
    DOCUMENT, CONTEXT_ITEM, VARIABLE, UNKNOWN
  }

  private static final SourceRef CONTEXT_ITEM = new SourceRef(Kind.CONTEXT_ITEM, null, null, LATEST_REVISION, null);
  private static final SourceRef UNKNOWN = new SourceRef(Kind.UNKNOWN, null, null, LATEST_REVISION, null);

  private final Kind kind;
  private final String databaseName;
  private final String resourceName;
  private final int revision;
  private final QNm variableName;

  private SourceRef(final Kind kind, final String databaseName, final String resourceName, final int revision,
      final QNm variableName) {
    this.kind = kind;
    this.databaseName = databaseName;
    this.resourceName = resourceName;
    this.revision = revision;
    this.variableName = variableName;
  }

  /**
   * A concrete document scan.
   *
   * @param databaseName the literal database name (must not be {@code null})
   * @param resourceName the literal resource name (must not be {@code null})
   * @param revision     the explicit revision, or {@link #LATEST_REVISION} when the source opens the
   *                     most-recent revision
   */
  public static SourceRef document(final String databaseName, final String resourceName, final int revision) {
    if (databaseName == null || resourceName == null) {
      throw new IllegalArgumentException("databaseName and resourceName must not be null");
    }
    return new SourceRef(Kind.DOCUMENT, databaseName, resourceName, revision, null);
  }

  /** The query's context item — the caller's own bound read transaction. */
  public static SourceRef contextItem() {
    return CONTEXT_ITEM;
  }

  /** An unprovable / non-single-document source; resource-bound executors should fail closed. */
  public static SourceRef unknown() {
    return UNKNOWN;
  }

  /**
   * A scan whose source is a variable the optimizer could not resolve to a binding inside the
   * query tree — typically an external variable bound at execution time via the query context.
   * Unlike {@link Kind#UNKNOWN} this ref is verifiable at RUNTIME: an executor can resolve
   * {@code variableName} through the {@code QueryContext}, inspect the actually-bound item, and
   * accept or decline based on the concrete document it denotes (see
   * {@link VectorizedExecutor#acceptsSource(SourceRef, io.brackit.query.QueryContext)}).
   * Compile-time-only consumers should keep treating it like {@link Kind#UNKNOWN} (fail closed).
   *
   * @param variableName the referenced variable's name (must not be {@code null})
   */
  public static SourceRef variable(final QNm variableName) {
    if (variableName == null) {
      throw new IllegalArgumentException("variableName must not be null");
    }
    return new SourceRef(Kind.VARIABLE, null, null, LATEST_REVISION, variableName);
  }

  public Kind kind() {
    return kind;
  }

  /** {@code true} iff this ref opens a concrete literal document. */
  public boolean isDocument() {
    return kind == Kind.DOCUMENT;
  }

  /** {@code true} iff this ref is the query's context item. */
  public boolean isContextItem() {
    return kind == Kind.CONTEXT_ITEM;
  }

  /** The literal database name for a {@link Kind#DOCUMENT} ref, else {@code null}. */
  public String databaseName() {
    return databaseName;
  }

  /** The literal resource name for a {@link Kind#DOCUMENT} ref, else {@code null}. */
  public String resourceName() {
    return resourceName;
  }

  /** The referenced variable's name for a {@link Kind#VARIABLE} ref, else {@code null}. */
  public QNm variableName() {
    return variableName;
  }

  /**
   * The explicit revision of a {@link Kind#DOCUMENT} ref, or {@link #LATEST_REVISION} when it opens the
   * most-recent revision. Always {@link #LATEST_REVISION} for the other kinds.
   */
  public int revision() {
    return revision;
  }

  /** {@code true} iff a {@link Kind#DOCUMENT} ref names no explicit revision (opens the latest). */
  public boolean opensLatestRevision() {
    return revision == LATEST_REVISION;
  }

  @Override
  public String toString() {
    return switch (kind) {
      case DOCUMENT -> "SourceRef[doc " + databaseName + "/" + resourceName + (revision == LATEST_REVISION
          ? ""
          : "@" + revision) + "]";
      case CONTEXT_ITEM -> "SourceRef[context-item]";
      case VARIABLE -> "SourceRef[variable $" + variableName + "]";
      case UNKNOWN -> "SourceRef[unknown]";
    };
  }
}
