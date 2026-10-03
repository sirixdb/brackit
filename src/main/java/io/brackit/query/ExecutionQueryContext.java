package io.brackit.query;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.brackit.query.atomic.AnyURI;
import io.brackit.query.atomic.DTD;
import io.brackit.query.atomic.Date;
import io.brackit.query.atomic.DateTime;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Time;
import io.brackit.query.expr.DefaultCtxItem;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.json.JsonCollection;
import io.brackit.query.jdm.json.JsonStore;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.jdm.node.NodeCollection;
import io.brackit.query.jdm.node.NodeFactory;
import io.brackit.query.jdm.node.NodeStore;
import io.brackit.query.jdm.type.ItemType;
import io.brackit.query.update.UpdateList;
import io.brackit.query.update.op.UpdateOp;

final class ExecutionQueryContext implements QueryContext {
  private final QueryContext delegate;
  private Map<DefaultCtxItem, Item> contextItems;

  ExecutionQueryContext(QueryContext delegate) {
    this.delegate = delegate;
  }

  @Override
  public synchronized Item resolveDefaultContextItem(DefaultCtxItem declaration, Supplier<Item> initializer) {
    if (contextItems == null) {
      contextItems = new IdentityHashMap<>(1);
    }
    return contextItems.computeIfAbsent(declaration, ignored -> initializer.get());
  }

  @Override
  public void addPendingUpdate(UpdateOp op) {
    delegate.addPendingUpdate(op);
  }

  @Override
  public void applyUpdates() {
    delegate.applyUpdates();
  }

  @Override
  public UpdateList getUpdateList() {
    return delegate.getUpdateList();
  }

  @Override
  public void setUpdateList(UpdateList updates) {
    delegate.setUpdateList(updates);
  }

  @Override
  public void bind(QNm name, Sequence sequence) {
    delegate.bind(name, sequence);
  }

  @Override
  public Sequence resolve(QNm name) {
    return delegate.resolve(name);
  }

  @Override
  public boolean isBound(QNm name) {
    return delegate.isBound(name);
  }

  @Override
  public void setContextItem(Item item) {
    delegate.setContextItem(item);
  }

  @Override
  public Item getContextItem() {
    return delegate.getContextItem();
  }

  @Override
  public ItemType getItemType() {
    return delegate.getItemType();
  }

  @Override
  public Node<?> getDefaultDocument() {
    return delegate.getDefaultDocument();
  }

  @Override
  public void setDefaultDocument(Node<?> defaultDocument) {
    delegate.setDefaultDocument(defaultDocument);
  }

  @Override
  public NodeCollection<?> getDefaultNodeCollection() {
    return delegate.getDefaultNodeCollection();
  }

  @Override
  public JsonCollection<?> getDefaultJsonCollection() {
    return delegate.getDefaultJsonCollection();
  }

  @Override
  public void setDefaultNodeCollection(NodeCollection<?> defaultNodeCollection) {
    delegate.setDefaultNodeCollection(defaultNodeCollection);
  }

  @Override
  public void setDefaultJsonCollection(JsonCollection<?> defaultJsonCollection) {
    delegate.setDefaultJsonCollection(defaultJsonCollection);
  }

  @Override
  public DateTime getDateTime() {
    return delegate.getDateTime();
  }

  @Override
  public Date getDate() {
    return delegate.getDate();
  }

  @Override
  public Time getTime() {
    return delegate.getTime();
  }

  @Override
  public DTD getImplicitTimezone() {
    return delegate.getImplicitTimezone();
  }

  @Override
  public AnyURI getBaseUri() {
    return delegate.getBaseUri();
  }

  @Override
  public NodeFactory<?> getNodeFactory() {
    return delegate.getNodeFactory();
  }

  @Override
  public NodeStore getNodeStore() {
    return delegate.getNodeStore();
  }

  @Override
  public JsonStore getJsonItemStore() {
    return delegate.getJsonItemStore();
  }
}
