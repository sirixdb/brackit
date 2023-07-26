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
package org.brackit.examples;

import org.brackit.query.BrackitQueryContext;
import org.brackit.query.QueryContext;
import org.brackit.query.QueryException;
import org.brackit.query.Query;
import org.brackit.query.jdm.node.NodeStore;

/**
 * Store existing (dynamic) documents and fragments into
 * the internal storage.
 */
public class StoreAndQuery {

  public static void main(String[] args) {
    try {
      storeDocumentAndQuery();
      System.out.println();
      storeCollectionAndQuery();
    } catch (QueryException e) {
      System.err.print("XQuery error ");
      System.err.print(e.getCode());
      System.err.print(": ");
      System.err.println(e.getMessage());
    }
  }

  private static void storeDocumentAndQuery() throws QueryException {
    // initialize query context and store
    QueryContext ctx = new BrackitQueryContext();
    NodeStore store = ctx.getNodeStore();

    // use XQuery to generate a sample document and store it
    System.out.println("Store document:");
    String query = """
        let $doc :=
        <log tstamp='{current-date()}' severity='critical'>
            <src>192.168.12.31</src>
            <msg>foo bar</msg>
        </log>
        return bit:store('mydoc.xml', $doc)""";
    System.out.println(query);
    new Query(query).evaluate(ctx);

    System.out.println();
    System.out.println("Query stored document:");
    query = "doc('mydoc.xml')/log/@severity/string()";
    System.out.println(query);
    QueryContext ctx2 = new BrackitQueryContext(store); // use same store
    new Query(query).prettyPrint().serialize(ctx2, System.out);
    System.out.println();
  }

  private static void storeCollectionAndQuery() throws QueryException {
    // initialize query context and store
    QueryContext ctx = new BrackitQueryContext();
    NodeStore store = ctx.getNodeStore();

    // use XQuery to generate a sample document and store it
    System.out.println("Store collection:");
    String query = """
        let $docs :=
           for $i in (1 to 10)
           let $sev := if ($i mod 3 = 0) then
                          'low'
                       else if ($i mod 3 = 1) then
                           'high'
                       else 'critical'
           return
              <log tstamp='{current-date()}' severity='{$sev}'>
                <src>192.168.12.{$i}</src>
                <msg>foo bar</msg>
              </log>
        return bit:store('mydocs.col', $docs)""";
    System.out.println(query);
    new Query(query).evaluate(ctx);

    QueryContext ctx2 = new BrackitQueryContext(store);
    System.out.println();
    System.out.println("Query loaded collection:");
    String xq2 = """
        for $log in collection('mydocs.col')/log
        where $log/@severity='critical'
        return
        <message>
          <from>{$log/src/text()}</from>
          <body>{$log/msg/text()}</body>
        </message>""";
    System.out.println(xq2);
    new Query(query).prettyPrint().serialize(ctx2, System.out);
    System.out.println();
  }
}
