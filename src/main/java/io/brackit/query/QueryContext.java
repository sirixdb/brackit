package io.brackit.query;

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
import io.brackit.query.atomic.AnyURI;
import io.brackit.query.atomic.DTD;
import io.brackit.query.atomic.Date;
import io.brackit.query.atomic.DateTime;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Time;

public interface QueryContext {

  void addPendingUpdate(UpdateOp op);

  /**
   * Apply updates.
   *
   * @throws QueryException throws a {@link QueryException} if anything goes wrong
   */
  void applyUpdates();

  UpdateList getUpdateList();

  void setUpdateList(UpdateList updates);

  void bind(QNm name, Sequence sequence);

  Sequence resolve(QNm name);

  boolean isBound(QNm name);

  void setContextItem(Item item);

  Item getContextItem();

  ItemType getItemType();

  Node<?> getDefaultDocument();

  void setDefaultDocument(Node<?> defaultDocument);

  NodeCollection<?> getDefaultNodeCollection();

  JsonCollection<?> getDefaultJsonCollection();

  void setDefaultNodeCollection(NodeCollection<?> defaultNodeCollection);

  void setDefaultJsonCollection(JsonCollection<?> defaultJsonCollection);

  DateTime getDateTime();

  Date getDate();

  Time getTime();

  DTD getImplicitTimezone();

  AnyURI getBaseUri();

  NodeFactory<?> getNodeFactory();

  NodeStore getNodeStore();

  JsonStore getJsonItemStore();

}
