/*
 * [New BSD License]
<<<<<<< HEAD
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
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
package io.brackit.query;

import io.brackit.query.atomic.QNm;

/**
 * @author Sebastian Baechle
 */
public class QueryException extends RuntimeException {
  private final QNm code;

  /** fn:error's user description (raw, NOT run through String.format) — null if not from fn:error. */
  private final String description;

  /** fn:error's error-object value ($err:value) — null if absent. */
  private final transient Object errorValue;

  /**
   * fn:error factory: keeps the description VERBATIM (no String.format — a '%' in user data
   * crashed it) and retains the error value for $err:value. A named factory instead of a
   * constructor: the varargs (QNm, String, Object...) constructor below format-interprets the
   * description, so an overload would be one missed argument away from that hazard.
   */
  public static QueryException fromFnError(QNm code, String description, Object errorValue) {
    return new QueryException(code, description, errorValue, true);
  }

  private QueryException(QNm code, String description, Object errorValue, boolean verbatim) {
    super(code.toString() + ": " + description);
    this.code = code;
    this.description = description;
    this.errorValue = errorValue;
  }

  public QueryException(QNm code) {
    super(String.format(code.toString()));
    this.code = code;
    this.description = null;
    this.errorValue = null;
  }

  public QueryException(QNm code, Object o) {
    super(code.toString() + ": " + o);
    this.code = code;
    this.description = null;
    this.errorValue = null;
  }

  public QueryException(QNm code, String message, Object... args) {
    super(String.format(code.toString() + ": " + message, args));
    this.code = code;
    this.description = null;
    this.errorValue = null;
  }

  public QueryException(Throwable cause, QNm code, String message, Object... args) {
    super(String.format(code.toString() + ": " + message, args), cause);
    this.code = code;
    this.description = null;
    this.errorValue = null;
  }

  public QueryException(Throwable cause, QNm code) {
    super(code.toString(), cause);
    this.code = code;
    this.description = null;
    this.errorValue = null;
  }

  public QueryException(Throwable cause, QNm code, Object o) {
    super(code.toString() + ": " + o, cause);
    this.code = code;
    this.description = null;
    this.errorValue = null;
  }

  public QNm getCode() {
    return code;
  }

  /** The fn:error description ($err:description) — falls back to the full message. */
  public String getDescription() {
    return description != null ? description : getMessage();
  }

  /** The fn:error error-object value ($err:value), or null. */
  public Object getErrorValue() {
    return errorValue;
  }
}
