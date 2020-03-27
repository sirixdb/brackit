package org.brackit.xquery.xdm.type;

import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.json.JsonItem;

public final class AnyStructuredItemType extends StructuredItemType {
  public AnyStructuredItemType() {}

  @Override
  public boolean matches(Item item) {
    return item.itemType().isStructuredItem();
  }

  @Override
  public String toString() {
    return "structured-item()";
  }

  @Override
  public boolean equals(Object obj) {
    return ((obj == this) || (obj instanceof StructuredItemType));
  }
}
