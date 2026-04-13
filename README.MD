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

## Performance

bjq native binary (Oracle GraalVM, PGO, G1 GC, `-O3`) vs jq 1.7 — wall-clock, median of 3 runs:

**1M records (173 MB flat array):**

| Query | bjq | jq | Speedup |
|---|---|---|---|
| Filter (`age > 40 and active`) | **2.4s** | 3.7s | 1.6x |
| Group by dept + 3 aggregates | **1.4s** | 7.0s | **4.9x** |
| Group by 2 keys + sort | **1.5s** | 6.0s | **3.9x** |
| Hash-join (1M customers × 5M orders) | **44.4s** | — (O(n·m), skipped) | — |
| Join + group + aggregate + sort | **14.7s** | — (skipped) | — |
| 5-way aggregation | **1.7s** | 4.9s | **2.9x** |
| String equality filter | **2.0s** | 3.6s | 1.8x |
| Top-N (order + slice) | **1.2s** | 5.8s | **4.8x** |
| Compound AND filter | **2.8s** | 4.0s | 1.4x |
| Count distinct | **1.5s** | 5.9s | **3.9x** |
| Multi-key group + top-N | **5.8s** | 39.6s | **6.8x** |

**100k records (18 MB):**

| Query | bjq | jq | Speedup |
|---|---|---|---|
| Filter | **314ms** | 376ms | 1.2x |
| Group by dept + 3 aggregates | **249ms** | 634ms | **2.5x** |
| Group by 2 keys + sort | **249ms** | 543ms | **2.2x** |
| 5-way aggregation | **205ms** | 466ms | **2.3x** |
| Top-N | **240ms** | 517ms | **2.2x** |
| Count distinct | **184ms** | 496ms | **2.7x** |
| Multi-key group + top-N | **968ms** | 3.2s | **3.3x** |

**Reproducible**: `./examples/benchmark.sh --sizes "10000 100000 1000000"` (or `benchmark-3way.sh` for native vs JAR vs jq).

### Scaling: 1M → 100M → 1B records

Compact 48-byte schema (`{"age":N,"dept":"X","city":"X","active":T}`), Linux, 20 cores, NVMe SSD, parallel memory-mapped scan via `Arena.ofConfined` per worker. All five vectorized query shapes scale linearly.

**Group-by dept:**

| Engine | 1M (50MB) | 100M (4.9GB) | 1B (49GB) |
|---|---|---|---|
| native + PGO | **532ms** | **1.1s** | **14.8s** |
| JAR (JVM) | 644ms | 2.7s | 16.4s |
| jq | 3.5s | **OOM** | **OOM** |

**Filter-count (`age > 40 and active`):**

| Engine | 1M | 100M | 1B |
|---|---|---|---|
| native + PGO | **474ms** | **1.0s** | 15.2s |
| JAR (JVM) | 571ms | 1.3s | **14.4s** |
| jq | 1.4s | **OOM** | **OOM** |

**Pure aggregates (sum/avg/min/max over age) — single parallel scan:**

| Engine | 100M sum | 100M avg | 100M min | 100M max | 1B sum |
|---|---|---|---|---|---|
| native + PGO | **1.4s** | **1.1s** | **1.1s** | **1.1s** | **14.8s** |
| JAR (JVM) | 4.9s | 2.1s | 2.0s | 1.6s | 13.8s |

`jq` OOMs at 100M records (exit 137, OOM-killed). Both Brackit paths scale linearly and hit ~3 GB/s (≈65–76M records/s) at 1B — approximately raw NVMe read speed. Pick native for CLI/startup latency, JVM when embedding Brackit as a library.

## Quick Start

### Option 1: Native Binary (fastest)

Download the pre-built binary for your platform:

```bash
# Linux (x86-64)
curl -L https://github.com/sirixdb/brackit/releases/latest/download/bjq-linux-amd64 -o bjq
chmod +x bjq
sudo mv bjq /usr/local/bin/

# Linux (ARM64)
curl -L https://github.com/sirixdb/brackit/releases/latest/download/bjq-linux-arm64 -o bjq
chmod +x bjq
sudo mv bjq /usr/local/bin/

# macOS (Apple Silicon)
curl -L https://github.com/sirixdb/brackit/releases/latest/download/bjq-macos-arm64 -o bjq
chmod +x bjq
sudo mv bjq /usr/local/bin/

# macOS (Intel)
curl -L https://github.com/sirixdb/brackit/releases/latest/download/bjq-macos-amd64 -o bjq
chmod +x bjq
sudo mv bjq /usr/local/bin/

# Windows (x86-64) - download bjq-windows-amd64.exe from GitHub Releases
```

Then use it:

```bash
echo '{"name": "Alice"}' | bjq '$$.name'

# FLWOR expressions - the killer feature!
bjq 'for $u in $$.users[] where $u.age > 21 order by $u.name return $u' data.json
```

### Option 2: Java Jar

Requires **Java 25** or later. Download the jar from [GitHub Releases](https://github.com/sirixdb/brackit/releases), then:

```bash
alias bjq='java --enable-preview --add-modules=jdk.incubator.vector -jar /path/to/bjq-jar-with-dependencies.jar'
bjq 'for $u in $$.users[] where $u.age > 21 return $u' data.json
```

### Option 3: Build from Source

Requires **Java 25** or later.

```bash
git clone https://github.com/sirixdb/brackit.git
cd brackit
mvn package

# Set up bjq alias
alias bjq='java --enable-preview --add-modules=jdk.incubator.vector -jar '$(pwd)'/target/bjq-jar-with-dependencies.jar'

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
<dependency>
  <groupId>io.sirix</groupId>
  <artifactId>brackit</artifactId>
  <version>0.7</version>
</dependency>
```

### Gradle

```groovy
dependencies {
    implementation 'io.sirix:brackit:0.7'
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
