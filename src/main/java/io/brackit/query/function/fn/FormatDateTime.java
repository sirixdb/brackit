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
package io.brackit.query.function.fn;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.atomic.TimeInstant;
import io.brackit.query.function.AbstractFunction;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.module.StaticContext;
import io.brackit.query.util.format.DateTimeFormatter;

/**
 * Implements the date formatting functions fn:format-dateTime($value, $picture),
 * fn:format-dateTime($value, $picture, $language, $calendar, $place), and the corresponding
 * two- and five-argument forms of fn:format-date and fn:format-time, as per
 * https://www.w3.org/TR/xpath-functions-31/#func-format-dateTime ff.
 *
 * <p>See {@link DateTimeFormatter} for the supported picture string subset and the documented
 * implementation-defined choices.</p>
 */
public class FormatDateTime extends AbstractFunction {

  private final DateTimeFormatter.Source source;

  public FormatDateTime(QNm name, DateTimeFormatter.Source source, Signature signature) {
    super(name, signature, true);
    this.source = source;
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    if (args[0] == null) {
      return null;
    }
    final TimeInstant value = (TimeInstant) args[0];
    final String picture = ((Atomic) args[1]).stringValue();
    final String language = args.length > 2 && args[2] != null ? ((Atomic) args[2]).stringValue() : null;
    final String calendar = args.length > 3 && args[3] != null ? ((Atomic) args[3]).stringValue() : null;
    // args[4] ($place) is accepted but intentionally unused: without a geographical/timezone
    // database the spec prescribes falling back to the default place for unrecognized values
    return new Str(DateTimeFormatter.format(source, value, picture, language, calendar));
  }
}
