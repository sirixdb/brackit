[![Build & test](https://github.com/sirixdb/brackit/actions/workflows/build.yml/badge.svg)](https://github.com/sirixdb/brackit/actions/workflows/build.yml)

<p align="center">
  <h1 align="center">Brackit</h1>
  <p align="center"><strong>A powerful JSONiq engine for querying JSON and XML</strong></p>
  <p align="center">Use it standalone like <code>jq</code>, or embed it in your data store</p>
</p>

---

## Why Brackit?

**Two ways to use it:**

1. **Command-line tool** (`bjq`) - Like `jq`, but with FLWOR expressions, joins, and user-defined functions
2. **Embeddable query engine** - Add JSONiq queries to your data store with automatic optimizations

```bash
# Query JSON from the command line
echo '{"users": [{"name": "Alice", "age": 30}, {"name": "Bob", "age": 25}]}' | \
  bjq 'for $u in $$.users[] where $u.age > 26 return $u.name'

"Alice"
```

## Quick Start

### Option 1: Download and Run

Download from [GitHub Releases](https://github.com/sirixdb/brackit/releases):

```bash
# Interactive mode
java -jar brackit-with-dependencies.jar -iq

# Query a JSON file
java -jar brackit-with-dependencies.jar -q "json-doc('data.json').users[].name"
```

**For jq-style usage**, create an alias for the bjq jar:

```bash
alias bjq='java -jar /path/to/bjq-jar-with-dependencies.jar'

# Simple field access
echo '{"name": "Alice"}' | bjq '$$.name'

# FLWOR expressions - the killer feature!
bjq 'for $u in $$.users[] where $u.age > 21 order by $u.name return $u' data.json
```

### Option 2: Build from Source

Requires **Java 25** or later.

```bash
git clone https://github.com/sirixdb/brackit.git
cd brackit
mvn package

# Set up bjq alias
alias bjq='java -jar '$(pwd)'/target/bjq-jar-with-dependencies.jar'

# Try it out - FLWOR with grouping!
echo '[{"cat":"A","v":1},{"cat":"B","v":2},{"cat":"A","v":3}]' | \
  bjq 'for $x in $$[] group by $c := $x.cat return {$c: sum($x.v)}'
```

## Features at a Glance

| Feature | Example |
|---------|---------|
| **Field access** | `$$.users[0].name` |
| **Array iteration** | `$$.items[].price` |
| **Python-style slices** | `$$[0:5]`, `$$[-1]`, `$$[::2]` |
| **Object projection** | `$${name, email}` |
| **Predicates** | `$$.users[][?$$.active]` |
| **FLWOR expressions** | `for $x in $$ where $x.age > 21 return $x` |
| **User-defined functions** | `declare function local:double($x) { $x * 2 }` |
| **Automatic join optimization** | Hash-joins for FLWOR with multiple `for` clauses |
| **JSON updates** | `insert`, `delete`, `replace`, `rename` |

## Mutable JSON with Update Expressions

Brackit supports the full JSONiq Update Facility - modify JSON data with declarative expressions:

```xquery
(: Insert fields into an object :)
insert json {"status": "active", "updated": current-dateTime()} into $user

(: Append to an array :)
append json $newItem into $order.items

(: Update a value :)
replace json value of $product.price with $product.price * 0.9

(: Remove a field :)
delete json $user.temporaryToken

(: Rename a field :)
rename json $record.oldFieldName as "newFieldName"
```

This makes Brackit ideal for data stores that need to expose update capabilities through a query language.

## The Power of FLWOR

Unlike simple path-based query languages, Brackit supports full **FLWOR expressions** (for, let, where, order by, return) - the SQL of JSON:

```xquery
(: Group sales by category and compute totals :)
for $sale in $$.sales[]
let $cat := $sale.category
group by $cat
order by sum($sale.amount) descending
return {
  "category": $cat,
  "total": sum($sale.amount),
  "count": count($sale)
}
```

```xquery
(: Join orders with customers - automatically optimized! :)
for $order in $$.orders[], $customer in $$.customers[]
where $order.customer_id eq $customer.id
return {
  "order": $order.id,
  "customer": $customer.name,
  "total": $order.total
}
```

## bjq: The jq Alternative

`bjq` provides a familiar jq-like interface with JSONiq power:

```bash
# Basic field access
bjq '$$.name' data.json

# Array operations
bjq '$$.users[].email' data.json
bjq '$$[0:5]' data.json              # First 5 elements
bjq '$$[-1]' data.json               # Last element

# Filtering
bjq 'for $u in $$.users[] where $u.active return $u' data.json

# Aggregation
bjq 'sum($$.prices[])' data.json

# Raw output (no quotes)
bjq -r '$$.name' data.json

# Compact output
bjq -c '$$' data.json
```

## Embed in Your Data Store

Brackit is designed as a **retargetable query compiler**. Data stores can plug in their own:

- **Physical optimizations** (index scans, specialized operators)
- **Storage backends** (your custom Node/Item implementations)
- **Rewrite rules** (index matching, predicate pushdown)

```java
// Minimal example: run a query in Java
QueryContext ctx = new BrackitQueryContext();
Query query = new Query("for $i in 1 to 10 return $i * $i");
query.serialize(ctx, System.out);
```

The optimizer automatically applies:
- Hash-joins for multi-variable FLWOR expressions
- Predicate pushdown
- Constant folding
- And more...

## Installation

### Maven

```xml
<repository>
  <id>sonatype-nexus-snapshots</id>
  <url>https://oss.sonatype.org/content/repositories/snapshots</url>
  <snapshots><enabled>true</enabled></snapshots>
</repository>

<dependency>
  <groupId>io.sirix</groupId>
  <artifactId>brackit</artifactId>
  <version>0.6-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
repositories {
    maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}

dependencies {
    implementation 'io.sirix:brackit:0.6-SNAPSHOT'
}
```

## JSONiq Syntax

### Arrays

```xquery
[ 1, 2, 3 ]                          (: literal array :)
[ =(1 to 5) ]                        (: spread: [1, 2, 3, 4, 5] :)
$arr[0]                              (: index access (0-based!) :)
$arr[-1]                             (: last element :)
$arr[1:3]                            (: slice :)
$arr[]                               (: unbox to sequence :)
```

### Objects

```xquery
{ "name": "Alice", "age": 30 }       (: literal object :)
$obj.name                            (: field access :)
$obj{name, age}                      (: projection :)
{ $obj1, $obj2 }                     (: merge objects :)
```

### Updates (for mutable stores)

```xquery
insert json {"new": "field"} into $obj
delete json $obj.field
replace json value of $obj.name with "Bob"
rename json $obj.old as "new"
```

## Differences from Standard JSONiq

- Array indexes start at **0** (not 1)
- Object projection: `$obj{field1, field2}` instead of `jn:project()`
- Python-style array slices: `$arr[start:end:step]`
- Statement syntax with semicolons (syntactic sugar for let-bindings)

## Community

Join us on [Discord](https://discord.gg/AstddxGxjP) to ask questions, share ideas, or contribute!

## Used By

- [SirixDB](https://github.com/sirixdb/sirix) - A bitemporal, append-only database storing JSON and XML with full version history at the node level

## Origins & Publications

Brackit was created by [Sebastian Bächle](http://wwwlgis.informatik.uni-kl.de/cms/index.php?id=team) during his PhD at TU Kaiserslautern, researching query processing for semi-structured data. It's now maintained as part of the SirixDB project.

- [Separating Key Concerns in Query Processing](http://wwwlgis.informatik.uni-kl.de/cms/fileadmin/publications/2013/Dissertation-Baechle.pdf) - Ph.D thesis by Dr. Sebastian Bächle
- [Unleashing XQuery for Data-independent Programming](http://wwwlgis.informatik.uni-kl.de/cms/fileadmin/publications/2014/Unleash.2014.pdf)
- [XQuery Processing over NoSQL Stores](http://wwwlgis.informatik.uni-kl.de/cms/fileadmin/publications/2013/ValerGvD2013.pdf)

## License

[New BSD License](LICENSE)
