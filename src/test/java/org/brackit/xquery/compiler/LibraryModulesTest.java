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
package org.brackit.xquery.compiler;

import org.brackit.xquery.*;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Sebastian Baechle
 */
public class LibraryModulesTest extends XQueryBaseTest {

  private static final String RIGHT =
      "module namespace right='right'; import module namespace left='left'; declare function right:foo() {2};";

  private static final String LEFT =
      "module namespace left='left'; import module namespace right='right'; declare function left:foo() {1};";

  private static final String FOO =
      "module namespace foo=\"http://brackit.org/lib/foo\"; " + "declare function foo:echo($s as item()*) as item()* "
          + "{ ($s, $s) };";

  private static final String FOO2 =
      "module namespace foo=\"http://brackit.org/lib/foo\"; " + "declare function foo:echo2($s as item()*) as item()* "
          + "{ ($s, $s) };";

  private static final String BAR =
      "module namespace bar=\"http://brackit.org/lib/bar\"; " + "declare function bar:echo2($s as item()*) as item()* "
          + "{ ($s, $s) };";

  private static final String IMPORT_FOO = "import module namespace foo=\"http://brackit.org/lib/foo\"; ";

  private static final String IMPORT_BAR = "import module namespace bar=\"http://brackit.org/lib/bar\"; ";

  @Test
  public void defineModule() {
    XQuery xq = new XQuery(FOO);
    xq.getModule();
    // simply rest if no error happens
  }

  @Test
  public void importModule() throws Exception {
    final BaseResolver res = new BaseResolver();
    CompileChain chain = new CompileChain() {
      private final ModuleResolver resolver = res;

      @Override
      protected ModuleResolver getModuleResolver() {
        return resolver;
      }
    };
    new XQuery(chain, FOO);
    XQuery xq = new XQuery(chain, IMPORT_FOO + "foo:echo('y')");
    QueryContext ctx = createContext();
    Sequence result = xq.execute(ctx);
    ResultChecker.check(new ItemSequence(new Str("y"), new Str("y")), result);
  }

  @Test
  public void importModulesInSameTargetNS() throws Exception {
    final BaseResolver res = new BaseResolver();
    CompileChain chain = new CompileChain() {
      private final ModuleResolver resolver = res;

      @Override
      protected ModuleResolver getModuleResolver() {
        return resolver;
      }
    };
    new XQuery(chain, FOO);
    new XQuery(chain, FOO2);
    XQuery xq = new XQuery(chain, IMPORT_FOO + "(foo:echo('y'), foo:echo2('y'))");
    QueryContext ctx = createContext();
    Sequence result = xq.execute(ctx);
    ResultChecker.check(new ItemSequence(new Str("y"), new Str("y"), new Str("y"), new Str("y")), result);
  }

  @Test
  public void importModulesInDifferentTargetNS() throws Exception {
    final BaseResolver res = new BaseResolver();
    CompileChain chain = new CompileChain() {
      private final ModuleResolver resolver = res;

      @Override
      protected ModuleResolver getModuleResolver() {
        return resolver;
      }
    };
    new XQuery(chain, FOO);
    new XQuery(chain, BAR);
    XQuery xq = new XQuery(chain, IMPORT_FOO + IMPORT_BAR + "(foo:echo('y'), bar:echo2('y'))");
    QueryContext ctx = createContext();
    Sequence result = xq.execute(ctx);
    ResultChecker.check(new ItemSequence(new Str("y"), new Str("y"), new Str("y"), new Str("y")), result);
  }

  @Test
  public void importModulesInSameTargetNSWithConflict() {
    final BaseResolver res = new BaseResolver();
    CompileChain chain = new CompileChain() {
      private final ModuleResolver resolver = res;

      @Override
      protected ModuleResolver getModuleResolver() {
        return resolver;
      }
    };
    new XQuery(chain, FOO);
    new XQuery(chain, FOO);
    try {
      new XQuery(chain, IMPORT_FOO + "foo:echo('y')");
      fail("double definition of function not detected");
    } catch (QueryException e) {
      assertEquals("XQST0034", e.getCode().getLocalName());
    }
  }

  @Test
  public void importDirectCyclicModule() {
    final BaseResolver res = new BaseResolver() {
      @Override
      public List<String> load(String uri, String[] locations) {
        if (!uri.equals("right")) {
          return null;
        }
        var list = new ArrayList<String>(1);
        list.add(RIGHT);
        return list;
      }

    };
    CompileChain chain = new CompileChain() {
      private final ModuleResolver resolver = res;

      @Override
      protected ModuleResolver getModuleResolver() {
        return resolver;
      }
    };
    new XQuery(chain, LEFT);
  }

  @Test
  public void importDirectCyclicModule2() {
    final BaseResolver res = new BaseResolver() {
      @Override
      public List<String> load(String uri, String[] locations) {
        if (uri.equals("right")) {
          var l = new ArrayList<String>(1);
          l.add(RIGHT);
          return l;
        }
        if (uri.equals("left")) {
          var l = new ArrayList<String>(1);
          l.add(LEFT);
          return l;
        }
        return null;
      }

    };

    CompileChain chain = new CompileChain() {
      private final ModuleResolver resolver = res;

      @Override
      protected ModuleResolver getModuleResolver() {
        return resolver;
      }
    };

    final XQuery xq = new XQuery(chain,
                                 "" + "import module namespace left='left'; "
                                     + "import module namespace right='right'; " + "right:foo() + left:foo()");
    Sequence result = xq.execute(ctx);
    ResultChecker.check(new Int32(3), result);
  }
}
