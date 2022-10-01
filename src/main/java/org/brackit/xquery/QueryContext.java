package org.brackit.xquery;

import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.DTD;
import org.brackit.xquery.atomic.Date;
import org.brackit.xquery.atomic.DateTime;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Time;
import org.brackit.xquery.update.UpdateList;
import org.brackit.xquery.update.op.UpdateOp;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.json.JsonCollection;
import org.brackit.xquery.xdm.json.JsonStore;
import org.brackit.xquery.xdm.node.Node;
import org.brackit.xquery.xdm.node.NodeCollection;
import org.brackit.xquery.xdm.node.NodeFactory;
import org.brackit.xquery.xdm.node.NodeStore;
import org.brackit.xquery.xdm.type.ItemType;

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
