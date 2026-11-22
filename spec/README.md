# Specification

Brackit implements a language someone else specified. This directory therefore holds conventions and
Brackit's own requirements, not a restatement of the language: the behaviour of a JSONiq or XQuery
construct is decided by the documents below, and a test, an issue or a pull request cites them
directly. [docs/DEVELOPMENT.md](../docs/DEVELOPMENT.md) explains why the project works this way.

## Specifications of record

| Specification | What it decides |
|---|---|
| [XQuery 3.1](https://www.w3.org/TR/xquery-31/) | Syntax, static and dynamic semantics, FLWOR, error conditions. |
| [XPath and XQuery Functions and Operators 3.1](https://www.w3.org/TR/xpath-functions-31/) | Every built-in function: signature, result, and the error it raises. |
| [XQuery and XPath Data Model 3.1](https://www.w3.org/TR/xpath-datamodel-31/) | Items, sequences, node identity, typed values. |
| [XSLT and XQuery Serialization 3.1](https://www.w3.org/TR/xslt-xquery-serialization-31/) | The output Brackit must produce for a result. |
| [JSONiq](https://www.jsoniq.org/) | Objects, arrays, JSON navigation and the JSONiq Update Facility. |
| [Differences from Standard JSONiq](../README.MD#differences-from-standard-jsoniq) | Brackit's own documented extensions: zero-based array indexes, `$obj{field,...}` projection, Python-style slices, statement syntax. |

## How to cite them

A W3C section number and an error code are stable, public identifiers that mean the same thing a
year from now, so they are the unit of traceability here — Brackit does not number requirements that
someone else has already numbered. A behaviour test names what it verifies: the specification, the
section or function, and the error code where one applies. The `err:` codes are already in the
engine, with their Javadoc, in
[`ErrorCode`](../src/main/java/io/brackit/query/ErrorCode.java), so the citation in a test and the
constant the engine raises are the same identifier.

```java
/** F&O 3.1 §14.4.5 fn:sum - a non-numeric operand raises err:FORG0006. */
```

## Brackit's own requirements

Where the specifications are silent — an extension, an engine-specific error, the observable
contract an optimization must preserve — Brackit is the specification, and the requirement is
written here before it is implemented, under the conventions in
[CONV-conventions.md](CONV-conventions.md).

| Chapter | Scope |
|---|---|
| [CONV-conventions.md](CONV-conventions.md) | Requirement conventions, evidence honesty, and the PR mutation lane's dispositions and test associations (CONV-201 to CONV-204). |

**There is no extension chapter yet.** The documented extensions listed above are specified only by
the README section that describes them, which states the syntax but not the error conditions or the
edge cases. Closing that gap is open work; stating otherwise here would be the overstatement
[CONV-102](CONV-conventions.md#conv-102--conformance-and-evidence-honesty) forbids. New extensions
get a numbered chapter from the start.
