package org.brackit.xquery.compiler.parser;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//new ANTLRParser().parse("3 instance of xs:int*").display();
		//new XQParser("<a><b/> 345 <c/>{ (1+1, (<a b='' c=\"aha  --{{{3>5}\"   />)<4)}</a>").parse().display();
		//new XQParser("element foo {1 + 1}").parse().display();
		// new XQParser("<a><b/> 345 <c/><?target hjaea?as>?>{ 1 + 1}</a>").parse().display();
		//new XQParser("for $a in (/) return foo($a/b/c) + fn:data()").parse().display();
		new XQParser("foo -foo").parse().display();
	}
}
