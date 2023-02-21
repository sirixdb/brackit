/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.jdm.type;

/**
 * Type as defined in {@linkplain http://www.w3.org/TR/xquery/#dt-sequence-type}
 *
 * @author Sebastian Baechle
 */
public final class SequenceType {
  public static SequenceType EMPTY_SEQUENCE = new SequenceType(new AnyItemType(), Cardinality.Zero);

  public static SequenceType ITEM_SEQUENCE = new SequenceType(new AnyItemType(), Cardinality.ZeroOrMany);

  public static SequenceType JSON_ITEM_SEQUENCE =
      new SequenceType(AnyJsonItemType.ANY_JSON_ITEM, Cardinality.ZeroOrMany);

  public static SequenceType ITEM = new SequenceType(new AnyItemType(), Cardinality.One);

  public static SequenceType JSON_ITEM = new SequenceType(AnyItemType.ANY, Cardinality.One);

  public static SequenceType NODE = new SequenceType(AnyItemType.ANY, Cardinality.One);

  public static SequenceType INTEGER = new SequenceType(AtomicType.INR, Cardinality.One);

  public static SequenceType STRING = new SequenceType(AtomicType.STR, Cardinality.One);

  private final ItemType itemType;

  private final Cardinality cardinality;

  public SequenceType(ItemType itemType, Cardinality cardinality) {
    this.itemType = itemType;
    this.cardinality = cardinality;
  }

  public ItemType getItemType() {
    return itemType;
  }

  public Cardinality getCardinality() {
    return cardinality;
  }

  private String cardinalityString() {
    switch (cardinality) {
      case OneOrMany:
        return "+";
      case ZeroOrMany:
        return "*";
      case ZeroOrOne:
        return "?";
      case One:
      case Zero:
        return "";
      default:
        throw new RuntimeException();
    }
  }

  @Override
  public String toString() {
    return (cardinality == Cardinality.Zero)
        ? "empty-sequence()"
        : String.format("%s%s", itemType, cardinalityString());
  }

  @Override
  public boolean equals(Object obj) {
    return ((obj == this) || ((obj instanceof SequenceType) && (((SequenceType) obj).itemType.equals(itemType))
        && (((SequenceType) obj).cardinality.equals(cardinality))));
  }
}
