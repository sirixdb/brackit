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
package org.brackit.xquery.jdm;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.jdm.node.Node;
import org.brackit.xquery.jdm.node.TemporalNode;

/**
 * Covers the document axes as defined in XQuery 1.0: 3.2.1.1 Axes
 *
 * <p>
 * Note that we sometimes have to check a node's kind because attributes are
 * often not in in the defined path axes.
 * </p>
 *
 * @author Sebastian Baechle
 * @author Johannes Lichtenberger
 */
public enum Axis {
  PARENT(true) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return node.isParentOf(other);
    }
  },
  CHILD(true) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return node.isChildOf(other);
    }
  },
  ANCESTOR(false) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return node.isAncestorOf(other);
    }
  },
  DESCENDANT(true) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return node.isDescendantOf(other);
    }
  },
  ANCESTOR_OR_SELF(false) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return node.isAncestorOrSelfOf(other);
    }
  },
  DESCENDANT_OR_SELF(true) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return node.isDescendantOrSelfOf(other);
    }
  },
  ATTRIBUTE(true) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return node.isAttributeOf(other);
    }
  },
  SELF(true) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return node.isSelfOf(other);
    }
  },
  FOLLOWING(true) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return node.isFollowingOf(other);
    }
  },
  FOLLOWING_SIBLING(true) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return node.isFollowingSiblingOf(other);
    }
  },
  PRECEDING(false) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return node.isPrecedingOf(other);
    }
  },
  PRECEDING_SIBLING(false) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return node.isPrecedingSiblingOf(other);
    }
  },
  NEXT(true) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return ((TemporalNode<?>) node).isNextOf((TemporalNode<?>) other);
    }
  },
  PREVIOUS(false) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return ((TemporalNode<?>) node).isPreviousOf((TemporalNode<?>) other);
    }
  },
  FUTURE(true) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return ((TemporalNode<?>) node).isFutureOf((TemporalNode<?>) other);
    }
  },
  FUTURE_OR_SELF(true) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return ((TemporalNode<?>) node).isFutureOrSelfOf((TemporalNode<?>) other);
    }
  },
  PAST(false) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return ((TemporalNode<?>) node).isEarlierOf((TemporalNode<?>) other);
    }
  },
  PAST_OR_SELF(false) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return ((TemporalNode<?>) node).isEarlierOrSelfOf((TemporalNode<?>) other);
    }
  },
  LAST(false) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return ((TemporalNode<?>) node).isLastOf((TemporalNode<?>) other);
    }
  },
  FIRST(true) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      return ((TemporalNode<?>) node).isFirstOf((TemporalNode<?>) other);
    }
  },
  ALL_TIME(true) {
    @Override
    public boolean check(Node<?> node, Node<?> other) throws QueryException {
      // TODO
      return true;
    }
  };

  private final boolean forward;

  private Axis(boolean forward) {
    this.forward = forward;
  }

  public abstract boolean check(Node<?> node, Node<?> other) throws QueryException;

  public boolean isForward() {
    return forward;
  }
}
