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
package io.brackit.query.update.json.op;

import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.json.Object;

import java.util.Objects;

/**
 * Shared identity objects for JSON update operations.
 * <p>
 * These identities are used by {@code UpdateList.apply()} to skip "property updates"
 * (e.g., {@code REPLACE_VALUE}) when the same target is marked for {@code DELETE}.
 * <p>
 * Important: These must be shared types (not private inner classes) so that
 * {@code Set.contains()} can match identities across different operation classes.
 */
record TargetIndexIdentity(Array target, int index) {
  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TargetIndexIdentity that)) {
      return false;
    }
    return target == that.target && index == that.index;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(target) * 31 + index;
  }
}

record TargetFieldIdentity(Object target, QNm field) {
  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TargetFieldIdentity that)) {
      return false;
    }
    return target == that.target && Objects.equals(field, that.field);
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(target) * 31 + Objects.hashCode(field);
  }
}
