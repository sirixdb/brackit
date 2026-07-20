/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.QNm;
import io.brackit.query.compiler.AST;
import io.brackit.query.compiler.XQ;
import io.brackit.query.compiler.optimizer.walker.topdown.VectorizedGroupByDetection;
import io.brackit.query.compiler.translator.SequentialPipelineStrategy;
import io.brackit.query.expr.VectorizedGroupByExpr;
import io.brackit.query.function.json.JSONFun;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Sequence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the source-document identity contract: {@link VectorizedGroupByDetection} lifts the scan's
 * source into a {@link SourceRef} under {@link VectorizedScanAnnotation#SOURCE_REF}, and
 * {@link SequentialPipelineStrategy#tryVectorizedExpr(AST, boolean)} hands it to
 * {@link VectorizedExecutor#acceptsSource(SourceRef)} so a resource-bound executor can decline a scan
 * over a document it is not bound to (falling back to the generic pipeline) rather than answering with
 * the wrong resource's data.
 */
public class VectorizedSourceRefTest {

  private VectorizedGroupByDetection stage;

  @BeforeEach
  void setUp() {
    stage = new VectorizedGroupByDetection();
    SequentialPipelineStrategy.setThreadVectorizedExecutor(null);
  }

  @AfterEach
  void tearDown() {
    SequentialPipelineStrategy.clearThreadVectorizedExecutor();
    SequentialPipelineStrategy.setVectorizedExecutor(null);
  }

  // ==================== AST builders ====================

  private AST varRef(String name) {
    return new AST(XQ.VariableRef, new QNm(name));
  }

  private AST deref(String varName, String fieldName) {
    AST deref = new AST(XQ.DerefExpr);
    deref.addChild(varRef(varName));
    deref.addChild(new AST(XQ.DerefExpr, new QNm(fieldName)));
    return deref;
  }

  private AST strLit(String value) {
    return new AST(XQ.Str, value);
  }

  private AST arrayAccess(AST subject) {
    AST aa = new AST(XQ.ArrayAccess);
    aa.addChild(subject);
    return aa;
  }

  /** {@code jn:doc('db','res')} or {@code jn:doc('db','res',rev)} when {@code rev != null}. */
  private AST jnDoc(String db, String res, Integer rev) {
    AST call = new AST(XQ.FunctionCall, new QNm(JSONFun.JSON_NSURI, JSONFun.JSON_PREFIX, "doc"));
    call.addChild(strLit(db));
    call.addChild(strLit(res));
    if (rev != null) {
      call.addChild(new AST(XQ.Int, new Int32(rev)));
    }
    return call;
  }

  /** A {@code jn:}-namespaced call to an arbitrary function of the given local name. */
  private AST jnCall(String local, AST... args) {
    AST call = new AST(XQ.FunctionCall, new QNm(JSONFun.JSON_NSURI, JSONFun.JSON_PREFIX, local));
    for (AST arg : args) {
      call.addChild(arg);
    }
    return call;
  }

  private AST end(AST returnExpr) {
    AST end = new AST(XQ.End);
    end.addChild(returnExpr);
    return end;
  }

  /** The canonical single-key group-by return: {@code {"city": $c, "count": count($u)}}. */
  private AST groupByReturn(String loopVar, String keyVar, String field) {
    AST obj = new AST(XQ.ObjectConstructor);
    AST keyField = new AST(XQ.KeyValueField);
    keyField.addChild(strLit(field));
    keyField.addChild(varRef(keyVar));
    obj.addChild(keyField);
    AST countField = new AST(XQ.KeyValueField);
    countField.addChild(strLit("count"));
    AST countCall = new AST(XQ.FunctionCall, new QNm("count"));
    countCall.addChild(varRef(loopVar));
    countField.addChild(countCall);
    obj.addChild(countField);
    return end(obj);
  }

  /** GroupBy on {@code keyVar} then the canonical return. */
  private AST groupByChain(String loopVar, String keyVar, String field) {
    AST gb = new AST(XQ.GroupBy);
    AST spec = new AST(XQ.GroupBySpec);
    spec.addChild(varRef(keyVar));
    gb.addChild(spec);
    gb.addChild(new AST(XQ.DftAggregateSpec));
    gb.addChild(groupByReturn(loopVar, keyVar, field));
    return gb;
  }

  /**
   * {@code for $u in <source> let $c := $u.<field> group by $c return {"<field>": $c, "count": count($u)}},
   * optionally wrapped in an outer {@code let $doc := <docBinding>}. Wrapped in PipeExpr → Start.
   */
  private AST groupByPipe(String loopVar, String keyVar, String field, AST source, String outerVar, AST docBinding) {
    AST letC = new AST(XQ.LetBind);
    AST cBinding = new AST(XQ.TypedVariableBinding);
    cBinding.addChild(new AST(XQ.Variable, new QNm(keyVar)));
    letC.addChild(cBinding);
    letC.addChild(deref(loopVar, field));
    letC.addChild(groupByChain(loopVar, keyVar, field));

    AST forBind = new AST(XQ.ForBind);
    AST uBinding = new AST(XQ.TypedVariableBinding);
    uBinding.addChild(new AST(XQ.Variable, new QNm(loopVar)));
    forBind.addChild(uBinding);
    forBind.addChild(source);
    forBind.addChild(letC);

    AST chainRoot = forBind;
    if (outerVar != null) {
      AST letDoc = new AST(XQ.LetBind);
      AST docTvb = new AST(XQ.TypedVariableBinding);
      docTvb.addChild(new AST(XQ.Variable, new QNm(outerVar)));
      letDoc.addChild(docTvb);
      letDoc.addChild(docBinding);
      letDoc.addChild(forBind);
      chainRoot = letDoc;
    }

    AST start = new AST(XQ.Start);
    start.addChild(chainRoot);
    AST pipe = new AST(XQ.PipeExpr);
    pipe.addChild(start);
    return pipe;
  }

  private AST root(AST pipe) {
    AST root = new AST(XQ.Start);
    root.addChild(pipe);
    return root;
  }

  private SourceRef sourceRefOf(AST pipe) {
    return (SourceRef) pipe.getProperty(VectorizedScanAnnotation.SOURCE_REF);
  }

  // ==================== detection: SOURCE_REF extraction ====================

  @Test
  void jnDocThroughLetBindResolvesToDocument() {
    // let $doc := jn:doc('mydb','res') for $u in $doc[] let $c := $u.city group by $c return {...}
    AST pipe = groupByPipe("u", "c", "city", arrayAccess(varRef("doc")), "doc", jnDoc("mydb", "res", null));
    stage.rewrite(null, root(pipe));

    SourceRef ref = sourceRefOf(pipe);
    assertNotNull(ref, "SOURCE_REF must be set on a vectorizable scan");
    assertTrue(ref.isDocument());
    assertEquals("mydb", ref.databaseName());
    assertEquals("res", ref.resourceName());
    assertTrue(ref.opensLatestRevision(), "no explicit revision → opens latest");
    assertEquals(SourceRef.LATEST_REVISION, ref.revision());
    // The claim itself must still fire — SOURCE_REF is additive metadata.
    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
  }

  @Test
  void jnDocDirectSourceResolvesToDocument() {
    // for $u in jn:doc('mydb','res')[] ... — the doc call is the direct for-source root.
    AST pipe = groupByPipe("u", "c", "city", arrayAccess(jnDoc("mydb", "res", null)), null, null);
    stage.rewrite(null, root(pipe));

    SourceRef ref = sourceRefOf(pipe);
    assertNotNull(ref);
    assertTrue(ref.isDocument());
    assertEquals("mydb", ref.databaseName());
    assertEquals("res", ref.resourceName());
  }

  @Test
  void jnDocWithExplicitRevisionCarriesRevision() {
    AST pipe = groupByPipe("u", "c", "city", arrayAccess(jnDoc("mydb", "res", 2)), "doc", jnDoc("mydb", "res", 2));
    stage.rewrite(null, root(pipe));

    SourceRef ref = sourceRefOf(pipe);
    assertTrue(ref.isDocument());
    assertEquals(2, ref.revision());
    assertTrue(!ref.opensLatestRevision());
  }

  @Test
  void dynamicResourceArgYieldsUnknown() {
    // jn:doc('mydb', $r) — the resource is a variable, not a literal: identity unprovable.
    AST dynamicDoc = new AST(XQ.FunctionCall, new QNm(JSONFun.JSON_NSURI, JSONFun.JSON_PREFIX, "doc"));
    dynamicDoc.addChild(strLit("mydb"));
    dynamicDoc.addChild(varRef("r"));
    AST pipe = groupByPipe("u", "c", "city", arrayAccess(dynamicDoc), null, null);
    stage.rewrite(null, root(pipe));

    assertEquals(SourceRef.Kind.UNKNOWN, sourceRefOf(pipe).kind());
  }

  @Test
  void dynamicRevisionArgYieldsUnknown() {
    // jn:doc('mydb','res', $rev) — the revision is dynamic, so the exact revision is unprovable.
    AST dynamicRev = new AST(XQ.FunctionCall, new QNm(JSONFun.JSON_NSURI, JSONFun.JSON_PREFIX, "doc"));
    dynamicRev.addChild(strLit("mydb"));
    dynamicRev.addChild(strLit("res"));
    dynamicRev.addChild(varRef("rev"));
    AST pipe = groupByPipe("u", "c", "city", arrayAccess(dynamicRev), null, null);
    stage.rewrite(null, root(pipe));

    assertEquals(SourceRef.Kind.UNKNOWN, sourceRefOf(pipe).kind());
  }

  @Test
  void collectionOpenerYieldsUnknown() {
    // jn:collection('mydb') spans more than one (resource, revision): not a single document.
    AST pipe = groupByPipe("u", "c", "city", arrayAccess(jnCall("collection", strLit("mydb"))), null, null);
    stage.rewrite(null, root(pipe));

    assertEquals(SourceRef.Kind.UNKNOWN, sourceRefOf(pipe).kind());
  }

  @Test
  void contextItemSourceResolvesToContextItem() {
    // for $u in .[] ... — the query's own context item (the caller's bound transaction).
    AST pipe = groupByPipe("u", "c", "city", arrayAccess(new AST(XQ.ContextItemExpr)), null, null);
    stage.rewrite(null, root(pipe));

    SourceRef ref = sourceRefOf(pipe);
    assertTrue(ref.isContextItem());
    assertEquals(SourceRef.Kind.CONTEXT_ITEM, ref.kind());
  }

  @Test
  void unresolvedVariableSourceYieldsUnknown() {
    // for $u in $mystery[] ... — $mystery is bound nowhere the walker can see.
    AST pipe = groupByPipe("u", "c", "city", arrayAccess(varRef("mystery")), null, null);
    stage.rewrite(null, root(pipe));

    assertEquals(SourceRef.Kind.UNKNOWN, sourceRefOf(pipe).kind());
  }

  // ==================== translate-time gate: acceptsSource ====================

  @Test
  void declinedSourceFallsBackToGenericPipeline() {
    AST pipe = groupByPipe("u", "c", "city", arrayAccess(jnDoc("mydb", "res", null)), null, null);
    stage.rewrite(null, root(pipe));
    assertEquals(Boolean.TRUE, pipe.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));

    RecordingExecutor executor = new RecordingExecutor(false);
    SequentialPipelineStrategy.setThreadVectorizedExecutor(executor);

    Expr result = SequentialPipelineStrategy.tryVectorizedExpr(pipe, false);
    assertNull(result, "a declined source must fall back to the generic pipeline");
    assertNotNull(executor.lastSource, "the executor must be consulted about the source");
    assertTrue(executor.lastSource.isDocument());
    assertEquals("res", executor.lastSource.resourceName());
  }

  @Test
  void acceptedSourceStillServes() {
    AST pipe = groupByPipe("u", "c", "city", arrayAccess(jnDoc("mydb", "res", null)), null, null);
    stage.rewrite(null, root(pipe));

    RecordingExecutor executor = new RecordingExecutor(true);
    SequentialPipelineStrategy.setThreadVectorizedExecutor(executor);

    Expr result = SequentialPipelineStrategy.tryVectorizedExpr(pipe, false);
    assertInstanceOf(VectorizedGroupByExpr.class, result, "an accepted source must serve the vectorized expr");
    assertSame(SourceRef.Kind.DOCUMENT, executor.lastSource.kind());
  }

  @Test
  void absentSourceRefIsNotGated() {
    // A hand-built annotated node with no SOURCE_REF (legacy callers): the gate must be skipped so the
    // executor's default accept-all behaviour is preserved, even for an otherwise-declining executor.
    AST pipe = new AST(XQ.PipeExpr);
    pipe.setProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY, Boolean.TRUE);
    pipe.setProperty(VectorizedScanAnnotation.GROUPBY_FIELD, "city");

    RecordingExecutor executor = new RecordingExecutor(false);
    SequentialPipelineStrategy.setThreadVectorizedExecutor(executor);

    Expr result = SequentialPipelineStrategy.tryVectorizedExpr(pipe, false);
    assertInstanceOf(VectorizedGroupByExpr.class, result, "no SOURCE_REF → gate skipped, executor serves");
    assertNull(executor.lastSource, "acceptsSource must not be consulted when SOURCE_REF is absent");
  }

  @Test
  void defaultAcceptsSourceServesEveryDocument() {
    // The default acceptsSource (accept-all) leaves non-resource-bound executors unaffected.
    AST pipe = groupByPipe("u", "c", "city", arrayAccess(jnDoc("mydb", "res", null)), null, null);
    stage.rewrite(null, root(pipe));

    SequentialPipelineStrategy.setThreadVectorizedExecutor(new DefaultExecutor());

    Expr result = SequentialPipelineStrategy.tryVectorizedExpr(pipe, false);
    assertInstanceOf(VectorizedGroupByExpr.class, result);
  }

  // ==================== stubs ====================

  /** Executor that records the source it was asked about and answers a fixed accept/decline. */
  private static final class RecordingExecutor implements VectorizedExecutor {
    private final boolean accept;
    private SourceRef lastSource;

    RecordingExecutor(boolean accept) {
      this.accept = accept;
    }

    @Override
    public boolean acceptsSource(SourceRef source) {
      this.lastSource = source;
      return accept;
    }

    @Override
    public boolean canExecute(QueryContext ctx) {
      return true;
    }

    @Override
    public Sequence executeGroupByCount(QueryContext ctx, String[] sourcePath, String groupField)
        throws QueryException {
      return null;
    }
  }

  /** Executor relying entirely on the default {@code acceptsSource}. */
  private static final class DefaultExecutor implements VectorizedExecutor {
    @Override
    public boolean canExecute(QueryContext ctx) {
      return true;
    }

    @Override
    public Sequence executeGroupByCount(QueryContext ctx, String[] sourcePath, String groupField)
        throws QueryException {
      return null;
    }
  }
}
