package org.brackit.query;

import org.brackit.query.atomic.AnyURI;
import org.brackit.query.atomic.DTD;
import org.brackit.query.atomic.Date;
import org.brackit.query.atomic.DateTime;
import org.brackit.query.atomic.QNm;
import org.brackit.query.atomic.Time;
import org.brackit.query.update.UpdateList;
import org.brackit.query.update.op.UpdateOp;
import org.brackit.query.jdm.Item;
import org.brackit.query.jdm.Sequence;
import org.brackit.query.jdm.json.JsonCollection;
import org.brackit.query.jdm.json.JsonStore;
import org.brackit.query.jdm.node.Node;
import org.brackit.query.jdm.node.NodeCollection;
import org.brackit.query.jdm.node.NodeFactory;
import org.brackit.query.jdm.node.NodeStore;
import org.brackit.query.jdm.type.ItemType;

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
