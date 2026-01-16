# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Brackit is a retargetable JSONiq query engine. It compiles and executes queries against semi-structured data (JSON and XML). Originally developed at TU Kaiserslautern, it's now maintained as part of the SirixDB ecosystem.

## Build Commands

```bash
mvn package                    # Build with tests
mvn -DskipTests package        # Build without tests
mvn clean test                 # Run all tests
mvn test -Dtest=ClassName      # Run single test class
mvn test -Dtest=ClassName#methodName  # Run single test method
```

## Running Brackit

```bash
java -jar target/brackit-0.6-SNAPSHOT-with-dependencies.jar -q "1+1"     # Direct query
java -jar target/brackit-0.6-SNAPSHOT-with-dependencies.jar -qf file.xq  # Query from file
java -jar target/brackit-0.6-SNAPSHOT-with-dependencies.jar -iq          # Interactive mode
```

## Architecture

### Compiler Pipeline

Query execution follows a layered pipeline:
1. **Parsing** (`JsoniqParser`) → AST
2. **Analysis** (`Analyzer`) → Semantic checking, type info
3. **Optimization** (`TopDownOptimizer`) → Query rewrites
4. **Translation** (`TopDownTranslator`) → Executable expressions
5. **Execution** → Pipelined operators with lazy evaluation

The `CompileChain` class orchestrates this pipeline and registers built-in functions.

### Key Packages

- `io.brackit.query` - Entry points: `Query`, `QueryContext`, `Main`
- `io.brackit.query.compiler` - Parser, analyzer, optimizer, translator
- `io.brackit.query.jdm` - Data model: `Item`, `Sequence`, `Expr`, `Node`
- `io.brackit.query.jdm.json` - JSON types: `Array`, `Object`, `JsonItem`
- `io.brackit.query.expr` - Expression implementations (FLWOR, paths, constructors)
- `io.brackit.query.operator` - Set-oriented operators (`ForBind`, `LetBind`, `GroupBy`, `OrderBy`, join operators)
- `io.brackit.query.atomic` - Atomic types (`Str`, `Int`, `Date`, `QNm`, etc.)
- `io.brackit.query.function` - Built-in functions (`fn/`, `bit/`, `io/`, `json/`)
- `io.brackit.query.update` - JSON/XML update operations

### Execution Model

FLWOR expressions use tuple-based operator pipelining (similar to relational databases). Each clause becomes an operator that processes tuple streams, enabling optimizations like hash-joins and predicate pushdown.

Sequences are lazily evaluated via the `Iter` interface. Updates are collected in an `UpdateList` and applied at query end.

### Extension Points

Data stores can add custom optimizations by extending the compile chain. Physical optimizations and index rewrite rules plug into the optimizer stage.

## Testing

Tests use JUnit 4. Extend `XQueryBaseTest` for new expression tests - it provides:
- `ctx` (QueryContext)
- `store` (NodeStore)
- Helper methods: `storeDocument()`, `xquery()`, `print()`

Test resources are in `src/test/resources/`.

## Language Notes

Brackit implements JSONiq with some differences:
- Array indexes start at 0 (not 1)
- Object projection syntax: `$object{field1,field2}` instead of a function
- Python-like array slices: `$arr[0:2]`, `$arr[::2]`, `$arr[-1]`
- Statement syntax: semicolon-terminated statements as syntactic sugar for let-bindings

## Code Style

Code formatting is enforced by `formatter-maven-plugin` using `src/main/tools/brackit-formatter.xml`. Formatting runs automatically during `process-sources` phase.
