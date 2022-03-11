<h1 align="center">Brackit - a retargetable JSONiq query engine</h1>

Brackit is a flexible JSONiq and XQuery query processor developed during Dr. Sebastian Bächles time as a PhD student at the TU Kaiserslautern in the context of their research in the field of query processing for semi-structured data. The system features a fast runtime and a flexible compiler backend, which is, e.g., able to rewrite queries for optimized join processing and efficient aggregation operations. It's either usable as an in-memory ad-hoc query engine or as the query engine of a data store. The data store itself can add sophisticated optimizations in different stages of the query processor. Thus, Brackit already bundles common optimizations and a data store can add further optimizations for instance for index matching.

Lately, Johannes Lichtenberger has added many optional temporal enhancements for temporal data stores such as SirixDB. Furthermore, JSON is now a first-class citizen. Brackit supports a slightly different syntax but the same data model as JSONiq and all update primitives described in the JSONiq specification. Brackit also supports Python-like array slices.

## Main features
- Retargetable, thus sharing optimizations, which are common for different data stores (physical optimizations and index rewrite rules can simply be added in further stages).
- JSONiq, a language which especially targets querying JSON, supporting user defined functions (higher order functions are on our TODO list), easy tree traversals, FLWOR expressions to iterate, filter, sort and project item sequences.
- Set-oriented processing, meaning pipelined execution of FLWOR clauses through operators, which operate on arrays of tuples and thus support known optimizations from relational database querying for joins and aggregates.

**We're currently working on a [Jupyter Notebook / Tutorial](https://colab.research.google.com/drive/19eC-UfJVm_gCjY--koOWN50sgiFa5hSC)**.

[Here's](https://github.com/sirixdb/brackit/blob/master/mission.md) a more detailed document about the vision and overall mission of Brackit.

## Syntax differences in relation to JSONiq
- `=>` expression to dereference object fields instead of `.`
- array indexes start at position 0
- object projections via a special syntax (`$object{field1,field2,field3}` instead of a function)
- Python-like array slices

## Community
We have a [Discord server](https://discord.gg/AstddxGxjP), where we'd welcome everyone who's interested in the project.

## Publications
As the project started at a university (TU - Kaiserslautern under supervision of Dr. Dr. Theo Härder we'd be happy if it would be used as a research project again, too as there's a wide field of topics for future research and improvements.)

- Ph.D thesis of Dr. Sebastian Bächle:
[Separating Key Concerns in Query Processing - Set Orientation, Physical Data Independence, and Parallelism](http://wwwlgis.informatik.uni-kl.de/cms/fileadmin/publications/2013/Dissertation-Baechle.pdf)

- Sebastian Bächle and Caetano Sauer:
[Unleashing XQuery for Data-independent Programming](http://wwwlgis.informatik.uni-kl.de/cms/fileadmin/publications/2014/Unleash.2014.pdf)

- Henrique Valer, Caetano Sauer and Theo Härder:
[XQuery Processing over NoSQL Stores](http://wwwlgis.informatik.uni-kl.de/cms/fileadmin/publications/2013/ValerGvD2013.pdf)

- Caetano Sauer, Sebastian Bächle and Theo Härder:
[Versatile XQuery Processing in MapReduce](http://wwwlgis.informatik.uni-kl.de/cms/fileadmin/publications/2013/ADBIS.2013.final.pdf)

- Caetano Sauer, Sebastian Bächle and Theo Härder:
[BrackitMR: Flexible XQuery Processing in MapReduce](http://wwwlgis.informatik.uni-kl.de/cms/fileadmin/publications/2013/WAIM2013.pdf)

## Getting started

### If you simply want to use Brackit as a standalone query processor use the JAR provided with the [release](https://github.com/sirixdb/brackit/releases/tag/brackit-0.1.10)

Otherwise for contributing

### [Download ZIP](https://github.com/sirixdb/brackit/archive/master.zip) or Git Clone

```
git clone https://github.com/sirixdb/brackit.git
```
or use the following dependencies in your Maven or Gradle project if you want to add queries in your Java or Kotlin projects for instance or if you want to implement some interfaces and add custom rewrite rules to be able to query your data store.

**Brackit uses Java 17, thus you need an up-to-date Gradle (if you want to work on Brackit) and an IDE (for instance IntelliJ or Eclipse).**

## Maven / Gradle
At this stage of development, you should use the latest SNAPSHOT artifacts from [the OSS snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/io/sirix/brackit/) to get the most recent changes. You should use the most recent Maven/Gradle versions as we'll update to the newest Java versions.

Just add the following repository section to your POM or build.gradle file:
```xml
<repository>
  <id>sonatype-nexus-snapshots</id>
  <name>Sonatype Nexus Snapshots</name>
  <url>https://oss.sonatype.org/content/repositories/snapshots</url>
  <releases>
    <enabled>false</enabled>
  </releases>
  <snapshots>
    <enabled>true</enabled>
  </snapshots>
</repository>
```
```groovy
repository {
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots/"
        mavenContent {
            snapshotsOnly()
        }
    }
}
```

```xml
<dependency>
  <groupId>io.sirix</groupId>
  <artifactId>brackit</artifactId>
  <version>0.1.10-SNAPSHOT</version>
</dependency>
```
```groovy
compile group:'io.sirix', name:'brackit', version:'0.1.10-SNAPSHOT'
```

## What's Brackit?
Brackit is a query engine, which either could be used by different storage/database backends whereas common optimizations are shared as for instance set-oriented processing and hash-joins of FLWOR-clauses. Furthermore, in-memory stores for both processing XML and JSON are supported, thus brackit can simply be used as an in-memory query processor for ad-hoc analysis.

At the moment we support XQuery 1.0 including library module support, the XQuery Update Facility 1.0 and some features of XQuery 3.0 like the FLWOR clauses group by and count.

As a speciality, Brackit comes with extensions to work natively with JSON-style arrays and objects mostly as in JSONiq, also supporting all the update statements of JSONiq. Furthermore array index slices as in Python are supported. Another extension allows you to use a special statement syntax for writing query programs in a script-like style.

## Jupyter Notebook / Tutorial
**We're currently working on a [tutorial](https://colab.research.google.com/drive/19eC-UfJVm_gCjY--koOWN50sgiFa5hSC
), where you can execute interactive queries on Brackit's in-memory store.**

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

That's all. You find the ready-to-use jar file(s) in the subdirectory _./target_

Step 3: Dependency

If you want to use brackit in your other maven- or gradle-based projects, please have a look into the "Maven / Gradle" section. 


## First Steps

### Running from the command line

Brackit ships with a rudimentary command line interface to run ad-hoc queries. Invoke it with 

```java
java -jar brackit-x.y.z-SNAPSHOT-with-dependencies.jar
```

where _x.y.z_ is the version number of brackit.

#### Simple queries

The simplest way to run a query is by passing it via STDIN:

```java
echo "1+1" | java -jar brackit-x.y.z-SNAPSHOT-with-dependencies.jar

=> 2
```

If the query is stored in a separate file, let's say test.xq, type:

```java
java -jar brackit-x.y.z-SNAPSHOT-with-dependencies.jar -q test.xq
```

or use the file redirection of your shell:

```java
java -jar brackit-x.y.z-SNAPSHOT-with-dependencies.jar < test.xq
```

You can also use an interactive shell and enter a bunch of queries terminated with an "END" on the last line:

```java
java -jar brackit-x.y.z-SNAPSHOT-with-dependencies.jar -iq
```

#### Querying documents

Querying documents is as simple as running any other query.

The default "storage" module resolves any referred documents accessed by the XQuery functions ```fn:doc()``` and ```fn:collection()``` at query runtime (XML).

To query a document in your local filesytem simply use the path to this document in the fn:doc() function:

```java
java -jar brackit-x.y.z-SNAPSHOT-with-dependencies.jar -q "doc('products.xml')//product[@prodno = '4711']"
```

For JSON there's the function ```jn:doc()```. Let's assume we have the following simple JSON structure:

```json
{"products":[{"productno":4711,"product":"Product number 4711"},{"productno":5982,"product":"Product number 5982"}]}
```

We can query this first dereferencing the "products" object field with `=>`, then unbox the array value via `[]` and add a filter where "." denotes the current context item and "{fieldName}" projects the resulting object into a new object, which is returned.

```java
java -jar brackit.jar -q "jn:doc('products.json')=>products[][.=>productno eq 4711]{product}"

Query result
{"product":"Product number 4711"}

```

Of course, you can also directly query documents via http(s), or ftp. For example:

```java
java -jar brackit-x.y.z-SNAPSHOT-with-dependencies.jar -q "count(doc('http://example.org/foo.xml')//bar)"

or

java -jar brackit-x.y.z-SNAPSHOT-with-dependencies.jar -q "count(jn:doc('http://example.org/foo.xml')=>bar[])"
```

### Coding with Brackit

Running a query embedded in a Java program requires only a few lines of code:

```Java
String query = """
    for $i in (1 to 4)
    let $d := {$i}
    return $d
    """;

// initialize a query context
QueryContext ctx = new QueryContext();

// compile the query
XQuery xq = new XQuery(query);

// enable formatted output
xq.setPrettyPrint(true);

// run the query and write the result to System.out
xq.serialize(ctx, System.out);
```

## JSON

Brackit features a seamless integration of JSON-like objects and arrays directly at the language level.

You can easily mix arbitrary XML and JSON data in a single query or simply use brackit to convert data from one format into the other. This allows you to get the most out of your data.

The language extension allows you to construct and operate JSON data directly; additional utility functions help you to perform typical tasks.

Everything is designed to simplify joint processing of XDM and JSON and to maximize the freedom of developers. Thus, our extension effectively supports some sort of superset of XDM and JSON. That means, it is possible to create arrays and objects which do not strictly conform to the JSON RFC. It's up to you to decide how you want to have your data look like!

### Arrays

Arrays can be created using an extended version of the standard JSON array syntax:

```XQuery
(: statically create an array with 3 elements of different types: 1, 2.0, "3" :)
[ 1, 2.0, "3" ]

(: for compliance with the JSON syntax we have to use functions to create the values 'true', 'false', and 'null'. They are translated into the XML values xs:bool('true'), xs:bool('false') and an atomic null value.
:)
[ true(), false(), jn:null() ]

(: as that's cumbersome per default Brackit will parse the tokens 'true', 'false' to the XDM boolean values and 'null' to the new type js:null. :)
[ true, false, null ]

(: is different to :)
[ (./true), (./false), (./null) ]
(: where each field is initialized as the result of a path expression
   starting from the current context item, e,g., './true'.
:)

(: dynamically create an array by evaluating some expressions: :)
[ 1+1, substring("banana", 3, 5), () ] (: yields the array [ 2, "nana", () ] :)

(: arrays can be nested and fields can be arbitrary sequences :)
[ (1 to 5) ] (: yields an array of length 1: [(1,2,3,4,5)] :)
[ some text ] (: yields an array of length 1 with an XML fragment as field value :)
[ 'x', [ 'y' ], 'z' ] (: yields an array of length 3: [ 'x' , ['y'], 'z' ] :)

(: a preceding '=' distributes the items of a sequence to individual array positions :)
[ =(1 to 5) ] (: yields an array of length 5: [ 1, 2, 3, 4, 5 ] :)

(: array fields can be accessed by the '[[ ]]' postfix operator: :)
let $a := [ "Jim", "John", "Joe" ] return $a[[1]] (: yields the string "John" :)

(: the function bit:len() returns the length of an array :)
bit:len([ 1, 2 ]) (: yields 2 :)

(: array slices are supported as for instance (as in Python) :)
let $a := ["Jim", "John", "Joe" ] return $a[[0:2]] (: yields ["Jim", "John"] :)

(: array slices with a step operator :)
let $a := ["Jim", "John", "Joe" ] return $a[[0:2:-1]] (: yields ["John", "Jim"] :)

let $a := [{"foo": 0}, "bar", {"baz":true}] return $a[[::2]] (: yields [{"foo":0},{"baz:true}] :)

(: array unboxing :)
let $a := ["Jim", "John", "Joe"] return $a[] (: yields the sequence "Jim" "John" "Joe" :)

(: the unboxing is made implicitly in for-loops :)
let $a := ["Jim", "John", "Joe]
for $value in $a
return $value (: yields the same as above :)

(: negative array index :)
let $a := ["Jim", "John", "Joe"] return $a[[-1]] (: yields "Joe" :)
```

### Objects 

Objects provide an alternative to XML to represent structured data. Like with arrays we support an extended version of the standard JSON object syntax:

```XQuery
(: statically create a record with three fields named 'a', 'b' and 'c' :)
{ "a": 1, "b" : 2, "c" : 3 }

(: 'null' is a new atomic type and jn:null() creates this type, true and false are translated into the XML values xs:bool('true'), xs:bool('false').
:)
{ "a": true(), "b" : false(), "c" : jn:null()}

or simply

{ "a": true, "b": false, "c": null}

(: field values may be arbitrary expressions:)
{ "a" : concat('f', 'oo') , "b" : 1+1, "c" : [1,2,3] } (: yields {"a":"foo","b":2,"c":[1,2,3]} :)

(: field values are defined by key-value pairs or by an expression
   that evaluates to an object
:)
let $r := { "x":1, "y":2 } return { $r, "z":3} (: yields {"x":1,"y":2,"z":3} :)

(: fields may be selectively projected into a new object :)
{"x": 1, "y": 2, "z": 3}{z,y} (: yields {"z":3,"y":2} :)

(: values of object fields can be accessed using the deref operator '=>' :)
{ "a": "hello", "b": "world" }=>b (: yields the string "world" :)

(: the deref operator can be used to navigate into deeply nested object structures :)
let $n := yval let $r := {"e" : {"m":'mvalue', "n":$n}} return $r=>e=>n/y (: yields the XML fragment yval :)

(: the deref operator can be used to navigate into deeply nested object structures in combination with the array unboxing operator for instance :)
(: note, that here the expression "[]" is unboxing the array and a sequence of items is evaluated for the next deref operator :)
(: the deref operator thus either get's a sequence input or an object as the left operand :)
let $r := {"e": {"m": [{"n":"o"}, true, null, {"n": "bar"}] }, "n":"m"}} return $r=>e=>m[]=>n (: yields "o" "bar" :)

(: to only retrieve the first item/value in the array you can use an index :)
let $r := {"e": {"m": [{"n":"o"}, true, null, {"n": "bar"}] }, "n":"m"}} return $r=>e=>m[[0]]=>n (: yields "o" :)

(: the function bit:fields() returns the field names of an object :)
let $r := {"x": 1, "y": 2, "z": 3} return bit:fields($r) (: yields the xs:QName array [x,y,z ] :)

(: the function bit:values() returns the field values of an object :)
let $r := {"x": 1, "y": 2, "z": (3, 4) } return bit:values($r) (: yields the array [1,2,(2,4)] :)
```

### JSONiq update expressions

Brackit supports all defined update statements in the JSONiq specification. It makes sense to implement these in a data store backend as for instance in SirixDB. 

```XQuery
(: rename a field in an object :)
let $object := {"foo": 0}
return rename json $object=>foo as "bar"  (: renames the field foo of the object to bar :)

(: append values into an array :)
append json (1, 2, 3) into ["foo", true, false, null]  (: appends the sequence (1,2,3) into the array (["foo",true,false,null,[1,2,3]]) :)

(: insert at a specific position :)
insert json (1, 2, 3) into ["foo", true, false, null] at position 2  (: inserts the sequence (1,2,3) into the second position of the array (["foo",true,[1,2,3],false,null]) :)

(: insert a json object and merge the field/values into an existing object :)
insert json {"foo": not(true), "baz": null} into {"bar": false}   (: inserts/appends the two field/value pairs into the object ({"bar":false,"foo":false,"baz:null}) :)

(: delete a field/value from an object :)
delete json {"foo": not(true), "baz": null}=>foo    (: removes the field "foo" from the object :)

(: delete an array item at position 1 in the array :)
delete json ["foo", 0, 1][[1]]  (: removes the 0 (["foo",1]) :)

(: replace a JSON value of a field with another value :)
replace json value of {"foo": not(true), "baz": null}=>foo with 1     (: thus, the object is adapted to {"foo":1,"baz":null} :)

(: replace an item in an array at the second position (that is the third) :)
replace json value of ["foo", 0, 1][[2]] with "bar"   (: thus, the array is adapted to ["foo",0,"bar"]

```

### Parsing JSON

```XQuery
(: the utility function json:parse() can be used to parse JSON data dynamically
   from a given xs:string
:)
let $s := io:read('/data/sample.json') return json:parse($s)
```
## Statement Syntax Extension (Beta)

**IMPORTANT NOTE:**

**
This extension is only a syntax extension to simplify programmer's life when writing XQuery. It is neither a subset of nor an equivalent to the XQuery Scripting Extension 1.0.
**

Almost any non-trivial data processing task consists of a series of consecutive steps. Unfortunately, the functional style of XQuery makes it a bit cumbersome to write code in a convenient, script-like fashion. Instead, the standard way to express a linear multi-step process (with access to intermediate results) is to write a FLWOR expression with a series of let-clauses.

As a shorthand, Brackit allows you to write such processes as a sequence of ';'-terminated statements, which most developers are familiar with:

```XQuery
(: declare external input :)
declare variable $file external;

(: read input data :)
$events := fn:collection('events');

(: join the two inputs :)
$incidents := for $e in $events
              where $e/@severity = 'critical'
              let $ip := x/system/@ip
              group by $ip
              order by count($e)
              return {$ip} count($e) ;

(: store report to file :)
$report := {$incidents};
$output := bit:serialize($report);
io:write($file, $output);

(: return a short message as result :)
Generated '{count($incidents)}' incident entries to report '{$file}'
```

Internally, the compiler treats this as a FLWOR expression with let-bindings. The result, i.e., the return expression, is the result of the last statement. Accordingly, the previous example is equivalent to:

```XQuery
(: declare external input :)
declare variable $file external;

(: read input data :)
let $events := fn:collection('events')

(: join the two inputs :)
let $incidents := for $e in $events
                  where $e/@severity = 'critical'
                  let $ip := x/system/@ip
                  group by $ip
                  order by count($e)
                  return {$ip} count($e)

(: store report to file :)
let $report := {$incidents}
let $output := bit:serialize($report)
let $written := io:write($file, $output)

(: return a short message as result :)
return Generated '{count($incidents)}' incident entries to report '{$file}'
```

The statement syntax is especially helpful to improve readability of user-defined functions.

The following example shows an - admittedly rather slow - implementation of the quicksort algorithm:

```XQuery
declare function local:qsort($values) {
    $len := count($values);
    if ($len <= 1) then (
        $values 
    ) else (
        $pivot := $values[$len idiv 2];
        $less := $values[. < $pivot];
        $greater := $values[. > $pivot];
        (local:qsort($less), $pivot, local:qsort($greater))
    )
};

local:qsort((7,8,4,5,6,9,3,2,0,1))
```
