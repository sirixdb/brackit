/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.xqts;

import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import static org.brackit.xquery.xqts.XMLComparator.compareAsXML;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.XQueryBaseTest;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.node.parser.StreamSubtreeProcessor;
import org.brackit.xquery.util.Cfg;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Stream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

enum SCENARIO { STANDARD, PARSE_ERROR, RUNTIME_ERROR, TRIVIAL }

enum COMPARATOR { XML, FRAGMENT, TEXT, IGNORE, INSPECT }

enum ROLE { PRINCIPAL_DATA, SUPPLEMENTAL_DATA, SCHEMA, DTD, PRINCIPAL, CONSOLE_LOG }

/**
 * 
 * @author Max Bechtold
 *
 */
public class XQTSCase extends XQueryBaseTest
{
	private Logger log = Logger.getLogger(XQTSCase.class);
	
	/** Fields related to navigating test catalog */
	protected String xqtsDir = "testdata/xqts/";
	protected static XPath xpath = XPathFactory.newInstance().newXPath();
	protected Document catalog;
	protected String queryPath;
	protected String xqueryFileExt;
	protected String resultPath;
	protected String sourcePath;

	private static Thread shutdowner = null;
	protected static DocumentBuilder builder;
	private static ProgressLogger pLog; 
	
	enum ExceptionType
	{
		UNEXP_AST_EXPR_NODE("Unexpected AST expr node '([^']*)' of type: (\\d*)"),
		
		UNKNOWN_FN("Unknown function: '([^']*)'"),

		UNASS_CTX("Dynamic context variable ([^']*) is not assigned a value");		
		;
		
		private Pattern pattern;
		
		ExceptionType(String patt)
		{
			this.pattern = Pattern.compile(patt);
		}
		
		public boolean matches(String exception)
		{
			return pattern.matcher(exception).matches();
		}
		
		public String getSpecificPart(String exception)
		{
			Matcher matcher = pattern.matcher(exception);
			String result = "";
			boolean found = matcher.find();
			if (found)
				for (int i = 1; i <= matcher.groupCount(); i++)
					result += (result.isEmpty() ? "" : " || ") + matcher.group(i);
			
			return result;
		}
		
		public static ExceptionType match(String exception)
		{
			for (ExceptionType exc : values())
				if (exc.pattern.matcher(exception).matches())
					return exc;
			return null;
		}
		
		public String toString()
		{
			return pattern.toString();
		}
	}
	
	private static class ProgressLogger
	{
		private TreeMap<String, ExceptionStats> exceptions;

		private File log = new File(FILENAME);
		private int total = 0;
		private int count = 0;
		private int collect = 10;
		private final int MAX_COLLECT = 100;
		
		private int pass = 0;
		private int fail = 0;
		
		private static final String FILENAME = Cfg.asString("org.brackit.xquery.debugDir") + System.getProperty("file.separator") + "xqts.log";

		public void start(int total)
		{
			count = 0;
			pass = 0;
			fail = 0;
			collect = 10;
			
			exceptions = new TreeMap<String, ExceptionStats>();
			
			this.total = total;
		}
		
		public void pass()
		{
			pass++;
		}

		public void logException(Throwable e, Node testCase)
		{
			String msg = null;
			if (e instanceof QueryException)
				msg = e.getMessage().replace(((QueryException) e).getCode().toString() + ": ", "");
			else
				msg = e.getClass().getSimpleName() + ": " + e.getMessage();
				
			ExceptionStats stats = exceptions.get(msg);
			if (stats == null)
				exceptions.put(msg, new ExceptionStats(msg, getNodeValue(testCase, "query/@name")));
			else
				stats.logOccurence();
			fail++;
			
			if (count++ == collect)
			{
				count = 0;
				if (collect < MAX_COLLECT)
					collect++;
				dumpReport();
			}
		}
		
		public void dumpReport()
		{
			PrintWriter out = null;
			try
			{
				if (log.exists())
					log.delete();
				log.createNewFile();
				out = new PrintWriter(new FileWriter(log));
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
			
			out.println(String.format("Progress report after %d of %d test cases", fail + pass, total));
			out.println("=========================================");
			out.println();
			out.println(String.format("Progress: %.2f%%", (fail + pass) * 100/ (float) total));
			out.println(String.format("Test cases passed: %d", pass));
			out.println(String.format("Test cases failed: %d", fail));
			out.println(String.format("Current score: %.2f%%", pass * 100 / (float) total));
			out.println();
			
			List<ExceptionStats> statsSorted = new LinkedList<ExceptionStats>(exceptions.values());
			Collections.sort(statsSorted);
			for (ExceptionStats stats : exceptions.values())
				stats.unmark();
			
			
			for (ExceptionType et : ExceptionType.values())
			{
				out.println("Category \"" + et + "\":");
			
				for (ExceptionStats stats : statsSorted)
				{
					if (!stats.mark && et.matches(stats.exception))
					{
						stats.mark();
						out.println(String.format("%d x %s (e.g. '%s')", stats.frequency, et.getSpecificPart(stats.exception), stats.sampleTestCase));
					}
				}
				out.println();
			}
			
			out.println("Uncategorized exceptions:");
			for (ExceptionStats stats : statsSorted)
				if (!stats.isMarked())
					out.println(String.format("%d x %s (e.g. '%s')", stats.frequency, stats.exception, stats.sampleTestCase));
				
			out.flush();
		}
		
		private class ExceptionStats implements Comparable<ExceptionStats>
		{
			private String exception;
			private int frequency;
			private String sampleTestCase;
			
			private boolean mark;

			ExceptionStats(String exception, String sampleTestCase)
			{
				this.exception = exception;
				this.frequency = 1;
				this.sampleTestCase = sampleTestCase;
			}
			
			public String getException()
			{
				return exception;
			}

			public int getFrequency()
			{
				return frequency;
			}

			public String getSampleTestCase()
			{
				return sampleTestCase;
			}

			public boolean isMarked()
			{
				return mark;
			}
			
			public void mark()
			{
				mark = true;
			}
			
			public void unmark()
			{
				mark = false;
			}
			
			public void logOccurence()
			{
				frequency++;
			}
			
			public String toString()
			{
				return String.format("%s (%d occurrences, e.g. in: %s)", exception, frequency, sampleTestCase);
			}

			@Override
			public int compareTo(ExceptionStats other)
			{
				if (this.frequency < other.frequency)
					return 1;
				else if (this.frequency > other.frequency)
					return -1;
				else
					return 0;
			}
			
		}
	}

	@Test
	public void testSingleQuery() throws Exception
	{
		String test = new BufferedReader(new InputStreamReader(System.in)).readLine();
		runTestByName(test);
	}

	private void runTestByName(String test) throws XPathExpressionException
	{
		String predicate = String.format("@name='%s'", test);
		NodeList cases = (NodeList) xpath.evaluate(String.format("//test-case[%s]", predicate), catalog, NODESET);
		int total = cases.getLength();
		assertTrue(runStandardTestCase(cases.item(0), null));
	}
	
	public static void main(String[] args) throws Exception
	{
		XQTSCase.setUpBeforeClass();
		XQTSCase xqtsCase = new XQTSCase();
		xqtsCase.setUp();
		if (args.length > 0)
			xqtsCase.runTestByName(args[0]);
		else
			xqtsCase.runTestCases("true()");
	}
	
	@Test
	public void testAllTestCases() throws XPathExpressionException, IOException
	{
		runTestCases("true()");
	}

	@Test
	public void testStandardCasesWithError() throws XPathExpressionException, IOException
	{
		runTestCases("@scenario='standard' and expected-error");
	}
	
	@Test
	public void testStandardCasesWithoutError() throws XPathExpressionException, IOException
	{
		runTestCases("@scenario='standard' and not(expected-error)");
	}
	
	private void runTestCases(String predicate) throws XPathExpressionException
	{
		NodeList cases = (NodeList) xpath.evaluate(String.format("//test-case[%s]", predicate), catalog, NODESET);
		int total = cases.getLength();
		System.out.println(String.format("Detected %d test cases with search predicate '%s'.", total, predicate));
		int passed = 0;
		int failed = 0;
		int count = 0;
		
		pLog.start(total);
		
		for (int i = 0; i < total; i++)
		{
			Node node = cases.item(i);
			String name = getAttributeValue(node, "name");
			boolean pass = runStandardTestCase(node, pLog);
			if (pass)
			{
				passed++;
				System.out.println("pass! (" + name + ")");
			}
			else
			{
				failed++;
				System.err.println("failed (" + name + ")");
			}
			if (++count == 100)
			{
				pLog.dumpReport();
				count = 0;
			}
		}
		
		pLog.dumpReport();
		
		System.out.println("---------------------------------------------");
		System.out.println(String.format("Of %d test cases, %d passed, and %d failed. (Score: %.2f)", total, passed, failed, passed / (float) total));
	}


	@Test
	public void testFeatureOccurences() throws NumberFormatException, XPathExpressionException
	{
		int count = ((NodeList) xpath.evaluate("//test-case", catalog, NODESET)).getLength();
		System.out.println("Total number of test cases: " + count);
		System.out.println("Test cases with ...");
		count = ((NodeList) xpath.evaluate("//test-case[module]", catalog, NODESET)).getLength();
		System.out.println("... references to modules: " + count);
		count = ((NodeList) xpath.evaluate("//test-case[contextItem]", catalog, NODESET)).getLength();
		System.out.println("... use of context item: " + count);
		count = ((NodeList) xpath.evaluate("//test-case[defaultCollection]", catalog, NODESET)).getLength();
		System.out.println("... use of defaultCollection: " + count);
		count = ((NodeList) xpath.evaluate("//test-case[@scenario='standard']", catalog, NODESET)).getLength();
		System.out.println("... scenario Standard: " + count);
		count = ((NodeList) xpath.evaluate("//test-case[@scenario='trivial']", catalog, NODESET)).getLength();
		System.out.println("... scenario Trivial: " + count);
		count = ((NodeList) xpath.evaluate("//test-case[expected-error]", catalog, NODESET)).getLength();
		System.out.println("... expected-error: " + count);
		count = ((NodeList) xpath.evaluate("//test-case[input-query]", catalog, NODESET)).getLength();
		System.out.println("... input-queries: " + count);
		count = ((NodeList) xpath.evaluate("//test-case[output-file/@compare='Inspect']", catalog, NODESET)).getLength();
		System.out.println("... compare method Inspect: " + count);
		count = ((NodeList) xpath.evaluate("//test-case[@scenario='standard' and expected-error]", catalog, NODESET)).getLength();
		System.out.println("... scenario standard and expected errors: " + count);
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		try
		{			
			pLog = new ProgressLogger();
		}
		catch (Exception e)
		{
			Assert.fail("An exception occured while setting up XQTS.");
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
	}

	@Before
	public void setUp() throws Exception, FileNotFoundException
	{
		super.setUp();
		try
		{
			loadXQTS();
		}
		catch (Exception e)
		{
			log.error(e);
		}
	}

	@After
	public void tearDown() throws Exception
	{
	}

	private void loadXQTS() throws Exception
	{
		// Load catalog
		xpath = XPathFactory.newInstance().newXPath();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		builder = dbf.newDocumentBuilder();
		catalog = builder.parse(new File(xqtsDir + "RollupCatalog.xml"));
		
		// Set up configuration
		Node root = (Node) xpath.evaluate("/test-suite", catalog, NODE);
		queryPath = getAttributeValue(root, "XQueryQueryOffsetPath");
		xqueryFileExt = getAttributeValue(root, "XQueryFileExtension");
		resultPath = getAttributeValue(root, "ResultOffsetPath");
		sourcePath = getAttributeValue(root, "SourceOffsetPath");
		
		log.info("Storing single documents...");
		NodeList nodes = ((NodeList) xpath.evaluate("test-suite/sources/source/@FileName", catalog, NODESET));
		int success = 0;
		int failed = 0;
		for (int i = 0; i < nodes.getLength(); i++)
		{
			Node doc = nodes.item(i);
			String filename = doc.getNodeValue();
			String path = xqtsDir + filename;
			try
			{
				DocumentParser parser = new DocumentParser(new File(path));
				parser.setRetainWhitespace(true);
				store.create(new File(filename).getName(), parser);
				success++;
			}
			catch (Exception e)
			{
				failed++;
				log.error("Unable to store document '" + filename + "' in database:");
				log.error(e);
				// Expected for XQTSCatalog.xml which references relatively located documents not existing in XTC dir
			}
		}
		log.info("Successful: " + success + ". Failed: " + failed);
		
		log.info("Storing collections...");
		nodes = ((NodeList) xpath.evaluate("test-suite/sources/collection", catalog, NODESET));
		
		for (int i = 0; i < nodes.getLength(); i++)
		{
			Node coll = nodes.item(i);
			String collectionName = getAttributeValue(coll, "ID");
			NodeList docs = ((NodeList) xpath.evaluate("input-document/text()", coll, NODESET));
			Collection<?> collection;
			try {
			    collection = store.create(collectionName);
			} catch (DocumentException e) {
			    log.error(String.format("Error while creating collection '%s':", collectionName));
			    log.error(e);
			    throw e;
			}
			
			for (int j = 0; j < docs.getLength(); j++)
			{
				Node doc = docs.item(j);
				String docId = doc.getNodeValue();
				String exp = String.format("test-suite/sources/source[@ID=\"%s\"]/@FileName", docId);
				String fileName = getNodeValue(catalog, exp);
				String path = xqtsDir + fileName;
				try
				{
					DocumentParser parser = new DocumentParser(new File(path));
					parser.setRetainWhitespace(true);
					collection.add(parser);
				}
				catch (Exception e)
				{
					log.error(String.format("Error while storing document '%s' in collection '%s':", fileName, collectionName));
					log.error(e);
					throw e;
				}
			}
		}
		
	}

	protected void groupCase(String testGroupName, String testCaseName)
	{
		String selector = String.format("//test-group[@name='%s']/test-case[@name='%s']", testGroupName, testCaseName);
		Node testCase = null;
		try
		{
			testCase = (Node) xpath.evaluate(selector, catalog, NODE);
		}
		catch (XPathExpressionException e)
		{
			fail("Unable to look up test case in catalog.");
		}
		
		switch (SCENARIO.valueOf(adaptEnum(getAttributeValue(testCase, "scenario"))))
		{
			case STANDARD: runStandardTestCase(testCase, null); break;
			case PARSE_ERROR: runParseErrorTestCase(testCase); break;
			case RUNTIME_ERROR: runRuntimeErrorTestCase(testCase); break;
		}
		
	}

	private boolean runStandardTestCase(Node testCase, ProgressLogger pLog)
	{
		boolean pass = false;
		String query = null;
		
		try
		{

			query = setupContextAndQuery(ctx, testCase);
			
			// Execute query
//			result = executeQuery(ctx, query);
			Sequence sequence = new XQuery(query).execute(ctx);
			Document result = normalizeSequence(sequence, ctx);

			// Verify result
			pass = verifyResult(testCase, result);
		}
		catch (QueryException e)
		{
			pass = verifyException(e, testCase);
			if (!pass)
			{
				if (pLog != null)
				{
					pLog.logException(e, testCase);
				}
				else
				{
					e.printStackTrace();
				}
				log.error(e);
			}
		}
		catch (Throwable e)
		{
			if (pLog != null)
			{
				pLog.logException(e, testCase);
			}
			else
			{
				e.printStackTrace();
			}
			log.error(e);
			pass = false;
		}
		
		if (pass)
		{
			if (pLog != null)
			{
				pLog.pass();
			}
		}
		return pass;
	}

	private boolean runRuntimeErrorTestCase(Node testCase)
	{
		boolean pass = false;
		try
		{
			String query = setupContextAndQuery(ctx, testCase);

			// Execute query
			new XQuery(query).execute(ctx);
			
			// As a runtime error is expected, coming to this line is not good
			pass = false;
		}
		catch (QueryException e)
		{
			pass = verifyException(e, testCase);
			if (!pass)
			{
				log.error(e);
			}
		}
		catch (Exception e)
		{
			log.error(e);
			pass = false;
		}
		
		return pass;
	}

	private boolean runParseErrorTestCase(Node testCase)
	{
		boolean pass = false;
		try
		{
			String query = setupContextAndQuery(ctx, testCase);
			
			// Execute query
			new XQuery(query).execute(ctx);
			
			// As a parse error is expected, coming to this line is not good
			pass = false;
		}
		catch (QueryException e)
		{
			pass = verifyException(e, testCase);
			if (!pass)
			{
				log.error(e);
			}
		}
		catch (Exception e)
		{
			log.error(e);
			pass = false;
		}
		
		return pass;
	}

	private String setupContextAndQuery(QueryContext ctx, Node testCase) throws Exception
	{
		// Load query
		String queryFilename = xqtsDir + queryPath + getAttributeValue(testCase, "FilePath") + getNodeValue(testCase, "query/@name") + xqueryFileExt;
		String query = readFileAsString(new File(queryFilename));
		
		// Bind input documents
		NodeList inputFiles = (NodeList) xpath.evaluate("input-file", testCase, NODESET);
		for (int i = 0; i < inputFiles.getLength(); i++)
		{
			Node node = inputFiles.item(i);
			if (ROLE.valueOf(adaptEnum(getAttributeValue(node, "role"))) == ROLE.PRINCIPAL_DATA)
			{
				String docId = getNodeValue(node, "./text()");
				String exp = String.format("test-suite/sources/source[@ID=\"%s\"]/@FileName", docId);
				String fileName = new File(getNodeValue(catalog, exp)).getName();
				String docRef = String.format("fn:doc(\"%s\")", fileName);
				String variable = getAttributeValue(node, "variable");
				ctx.bind(new QNm(variable), new XQuery(docRef).execute(ctx));
			}
		}
		
		// Bind input uris
		NodeList inputUris = (NodeList) xpath.evaluate("input-URI", testCase, NODESET);
		for (int i = 0; i < inputUris.getLength(); i++)
		{
			Node node = inputUris.item(i);
			if (ROLE.valueOf(adaptEnum(getAttributeValue(node, "role"))) == ROLE.PRINCIPAL_DATA)
			{
				String docId = getNodeValue(node, "./text()");
				String exp = String.format("test-suite/sources/source[@ID=\"%s\"]/@FileName", docId);
				String fileName = new File(getNodeValue(catalog, exp)).getName();
				String docRef = "'" + fileName + "'";
				String variable = getAttributeValue(node, "variable");
				int pos = query.indexOf("declare variable $" + variable) + 18 + variable.length();
				query = query.substring(0, pos) + " as xs:string" + query.substring(pos); 
				ctx.bind(new QNm(variable), new XQuery(docRef).execute(ctx));
			}
		}
		
		// Bind results of input queries
		NodeList inputQueries = (NodeList) xpath.evaluate("input-query", testCase, NODESET);
		for (int i = 0; i < inputQueries.getLength(); i++)
		{
			Node node = inputQueries.item(i);
			String fileName = xqtsDir + queryPath + getAttributeValue(testCase, "FilePath") + getNodeValue(testCase, "input-query/@name") + xqueryFileExt;
			String inputQuery = readFileAsString(new File(fileName));
			String variable = getAttributeValue(node, "variable");
			ctx.bind(new QNm(variable), new XQuery(inputQuery).execute(ctx));
		}
		
		// Bind context item
		String contextItem = getNodeValue(testCase, "contextItem/text()");
		if (contextItem != null)
		{
			String exp = String.format("test-suite/sources/source[@ID=\"%s\"]/@FileName", contextItem);
			String fileName = new File(getNodeValue(catalog, exp)).getName();
			String docRef = String.format("fn:doc(\"%s\")", fileName);
			Sequence sequence = new XQuery(docRef).execute(ctx);
			ctx.setDefaultContext(sequence.iterate().next(), Int32.ONE, Int32.ONE);
		}
		
		// Bind default collection
		String defaultCollection = getNodeValue(testCase, "defaultCollection/text()");
		if (defaultCollection != null)
		{
			ctx.setDefaultCollection(store.lookup(defaultCollection));
		}
		
		return query;
	}

	private boolean verifyResult(Node testCase, Document result) throws IOException, XPathExpressionException
	{
		boolean pass = false;
		NodeList possibleResults = (NodeList) xpath.evaluate("output-file", testCase, NODESET);
		
		for (int i = 0; i < possibleResults.getLength() && !pass; i++)
		{
			Node item = possibleResults.item(i);
			String expected = null;
			
			if (!COMPARATOR.valueOf(adaptEnum(getAttributeValue(item, "compare"))).equals(COMPARATOR.IGNORE))
			{
				String filename = xqtsDir + resultPath + getAttributeValue(testCase, "FilePath") + getNodeValue(item, "./text()");
				expected = readFileAsString(new File(filename));
			}
			
			switch (COMPARATOR.valueOf(adaptEnum(getAttributeValue(item, "compare"))))
			{
				case TEXT: pass = compareAsXML(result, expected); break;
				case XML: pass = compareAsXML(result, expected); break;
				case FRAGMENT: pass = compareAsXML(result, expected); break;
				case IGNORE: pass = true; break;
				case INSPECT: pass = false; break;
			}
		}
		
		return pass;
	}


	/**
	 * Creates DOM trees of node sequences rooted with artificial node
	 * as per Sequence normalization spec http://www.w3.org/TR/xslt-xquery-serialization section 2
	 * 
	 */
	private Document normalizeSequence(Sequence result, QueryContext ctx) throws QueryException
	{		
		SequenceListener listener = new SequenceListener(XMLComparator.ROOT_NODE);
		
		if (result == null)
		{
			return listener.getDocument();
		}
		
		Iter it = result.iterate();
		Item item = null;
		Document document = null;
		
		StringBuilder sb = new StringBuilder();
		try
		{
			while ((item = it.next()) != null)
			{
				if (item instanceof Atomic)
				{
					// Fusion adjacent Atomics in a single text node 
					if (sb.length() > 0)
						sb.append(' ');
					sb.append(((Atomic) item).stringValue());
				}
				else if (item instanceof org.brackit.xquery.xdm.Node<?>)
				{
					org.brackit.xquery.xdm.Node<?> node = (org.brackit.xquery.xdm.Node<?>) item;
					if (node.getKind() == Kind.DOCUMENT)
					{
						// Pull children nodes up to top level and discard document node 
						if (sb.length() > 0)
						{
							listener.appendText(sb.toString());
							sb = new StringBuilder();
						}
						
						Stream<? extends org.brackit.xquery.xdm.Node<?>> children = node.getChildren();
						org.brackit.xquery.xdm.Node<?> child = null;
						
						while ((child = children.next()) != null)
						{
							Stream<?> subtree = child.getSubtree();
							StreamSubtreeProcessor processor = new StreamSubtreeProcessor(subtree, listener);
							processor.process();
						}
					}
					else
					{
						if (sb.length() > 0)
						{
							listener.appendText(sb.toString());
							sb = new StringBuilder();
						}
						
						StreamSubtreeProcessor processor = new StreamSubtreeProcessor(node.getSubtree(), listener);
						processor.process();
					}
				}
			}
			if (sb.length() > 0)
				listener.appendText(sb.toString());
			document = listener.getDocument();
		}
		catch (DocumentException e)
		{
			log.error(e);
		}
		finally
		{
			it.close();
		}
		return document;
	}

	private boolean verifyException(QueryException e, Node testCase)
	{
		NodeList possibleExceptions = null;
		try
		{
			possibleExceptions = (NodeList) xpath.evaluate("expected-error/text()", testCase, NODESET);
		}
		catch (XPathExpressionException e1)
		{
			log.error(e);
			return false;
		}
		
		if (possibleExceptions.getLength() == 0)
		{
			return false;
		}
		else
		{
			String code = e.getCode().getLocalName();
			// Check if e is contained in possible exceptions
			for (int i = 0; i < possibleExceptions.getLength(); i++)
			{
				if (possibleExceptions.item(i).getNodeValue().equals(code))
				{
					return true;
				}
			}
			return false;
		}
			
	}
	
	
	static String getNodeValue(Node context, String expression)
	{
		try
		{
			if (".".equals(expression))
				return context.getNodeValue();
			
			Node node = (Node) xpath.evaluate(expression, context, NODE);
			return (node == null ? null : node.getNodeValue());
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	String getAttributeValue(Node element, String name)
	{
		return element.getAttributes().getNamedItem(name).getNodeValue();
	}
	
	String readFileAsString(File path) throws java.io.IOException
	{
	    byte[] buffer = new byte[(int) path.length()];
	    BufferedInputStream f = new BufferedInputStream(new FileInputStream(path));
	    f.read(buffer);
	    return new String(buffer);
	}
	
	String adaptEnum(String enumVal)
	{
		return enumVal.toUpperCase().replace('-', '_');
	}
}
