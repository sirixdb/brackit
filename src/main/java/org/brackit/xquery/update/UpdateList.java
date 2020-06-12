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
package org.brackit.xquery.update;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.update.op.OpType;
import org.brackit.xquery.update.op.UpdateOp;
import org.brackit.xquery.util.log.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * @author Sebastian Baechle
 * @author Johannes Lichtenberger
 */
public final class UpdateList {
  private static final Logger log = Logger.getLogger(UpdateList.class);

  private static final EnumSet<OpType> checkOps =
      EnumSet.of(OpType.RENAME, OpType.REPLACE_NODE, OpType.REPLACE_VALUE, OpType.REPLACE_ELEMENT_CONTENT);

  private final List<UpdateOp> ops;

  public UpdateList() {
    ops = new ArrayList<>();
  }

  public void append(UpdateOp op) {
    ops.add(op);
  }

  public void apply() throws QueryException {
    // See XQuery Update Facility 1.0: 3.2.2 upd:applyUpdates
    // First all ops are sorted according to the order of their
    // application which is determined by their type.
    // The resulting list is then checked if some ops
    // are performed twice or more on the same target node
    final var orderByTypeCmp = Comparator.comparing(UpdateOp::getType);
    ops.sort(orderByTypeCmp);

    for (int i = 0, size = ops.size(); i < size; i++) {
      final var firstOperator = ops.get(i);
      if (checkOps.contains(firstOperator.getType())) {
        for (int j = i + 1; j < ops.size(); j++) {
          final var secondOperator = ops.get(j);

          if (secondOperator.getType() != firstOperator.getType()) {
            break;
          }
          checkCompatibility(firstOperator, secondOperator);
        }
      }
    }

    // finally apply all updates
    ops.forEach(op -> {
      if (log.isDebugEnabled()) {
        log.debug(String.format("Applying pending update %s", op));
      }
      op.apply();
    });
  }

  private void checkCompatibility(final UpdateOp op1, final UpdateOp op2) throws QueryException {
    switch (op1.getType()) {
      case RENAME:
        if (op1.getTarget().equals(op2.getTarget())) {
          throw new QueryException(ErrorCode.ERR_UPDATE_DUPLICATE_RENAME_TARGET,
                                   "Node %s is target of more than one replace operation.",
                                   op2.getTarget());
        }
        break;
      case REPLACE_NODE:
        if (op1.getTarget().equals(op2.getTarget())) {
          throw new QueryException(ErrorCode.ERR_UPDATE_DUPLICATE_REPLACE_NODE_TARGET,
                                   "Node %s is target of more than one replace node operation.",
                                   op2.getTarget());
        }
        break;
      case REPLACE_VALUE:
        if (op1.getTarget().equals(op2.getTarget())) {
          throw new QueryException(ErrorCode.ERR_UPDATE_DUPLICATE_REPLACE_VALUE_TARGET,
                                   "Node %s is target of more than one replace value operation.",
                                   op2.getTarget());
        }
        break;
      case REPLACE_ELEMENT_CONTENT:
        if (op1.getTarget().equals(op2.getTarget())) {
          throw new QueryException(ErrorCode.ERR_UPDATE_DUPLICATE_REPLACE_VALUE_TARGET,
                                   "Node %s is target of more than one replace element content operation.",
                                   op2.getTarget());
        }
        break;
      default:
        // Do nothing.
    }
  }

  public List<UpdateOp> list() {
    return ops;
  }
}