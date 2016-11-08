# Welcome to Brackit!

Brackit is a flexible XQuery-based query engine developed during my time as PhD student at the TU Kaiserslautern in the context of our research in the field of query processing for semi-structured data. The system features a fast runtime and a flexible compiler backend, which is , e.g., able to rewrite queries for optimized join processing and efficient aggregation operations.

## Features

At the moment we support XQuery 1.0 including library module support, the XQuery Update Facility 1.0 and some features of XQuery 3.0 like the new FLWOR clauses group by and count.

As a speciality, Brackit comes with extensions to work natively with JSON-style arrays and records. Another extension allows you to use a special statement syntax for writing XQuery programs in a script-like style.

## Installation

### Compiling from source

To build and package change into the root directy of the project and run Maven:

```
mvn package
```

To skip running the unit tests run instead.

```
mvn -DskipTests package
```

That's all. You find the ready-to-use jar file in the subdirectory _./target_

Step 3: Install (optional)

If you want to use brackit in your other Maven-based projects, you need to install brackit in your local maven repository.

```
mvn install
```

## First Steps

### Running from the command line

Brackit ships with a rudimentary command line interface to run ad-hoc queries. Invoke it with 

```
java -jar brackit-x.y.z.jar
```

where _x.y.z_ is the version number of brackit.

#### Simple queries

The simplest way to run a query is by passing it via stdin:

```
echo "1+1" | java -jar brackit-x.y.z.jar

=> 2
```

If the query is stored in a separate file, let's say test.xq, type:

```
java -jar brackit-x.y.z.jar -q test.xq
```

or use the file redirection of your shell:

```
java -jar brackit-x.y.z.jar < test.xq
```

#### Querying documents

Querying documents is as simple as running any other query.

The default "storage" module resolves any referred documents accessed by the XQuery functions ```fn:doc()``` and ```fn:collection()``` at query runtime.

To query a document in your local filesytem simply type use the path to this document in the fn:doc() function:

```
echo "doc('products.xml')//product[@prodno = '4711']" | java -jar brackit-x.y.z.jar
```

Of course, you can also directly query documents via http(s), or ftp. For example:

```
echo "count(doc('http://example.org/foo.xml')//bar)" | java -jar brackit-x.y.z.jar
```

### Coding with Brackit

Running XQuery embedded in a Java program requires only a few lines of code:

```Java
String query =
    "for $i in (1 to 4)"
  + "let $d := {$i}"
  + "return $d";

// initialize a query context
QueryContext ctx = new QueryContext();

// compile the query
XQuery xq = new XQuery(query);

// enable formatted output
xq.setPrettyPrint(true);

// run the query and write the result to System.out
xq.serialize(ctx, System.out);
```
