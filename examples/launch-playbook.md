# Brackit Launch Playbook

Step-by-step guide for a successful public launch.

---

## Phase 1: Pre-Launch (1–2 weeks before)

### 1.1 Ship downloadable binaries

- [ ] Trigger the `native-binaries` workflow manually to verify it builds
- [ ] Create a GitHub release `v0.7` (or tag a pre-release `v0.7-rc1`)
- [ ] Confirm binaries appear as release assets:
  - `bjq-linux-amd64`
  - `bjq-linux-arm64`
  - `bjq-macos-amd64`
  - `bjq-macos-arm64`
  - `bjq-windows-amd64.exe`
- [ ] Test each binary on a clean machine (at least linux + macOS)
- [ ] Add install instructions to README:
  ```bash
  # Download (example for macOS ARM)
  curl -L https://github.com/sirixdb/brackit/releases/latest/download/bjq-macos-arm64 -o bjq
  chmod +x bjq
  sudo mv bjq /usr/local/bin/
  ```

### 1.2 Run benchmarks and collect numbers

- [ ] Run `examples/benchmark.sh` on a standard machine (e.g., c5.xlarge or M2 Mac)
- [ ] Run the Java `QueryBenchmark` for in-process numbers
- [ ] Record results in a `BENCHMARKS.md` or gist — real numbers, not estimates
- [ ] Key numbers to capture:
  - bjq vs jq on 100k-record group-by (bjq should win)
  - bjq hash-join at 100k+ where jq can't even finish
  - Native binary startup time vs jar startup time

### 1.3 Prepare a demo GIF

- [ ] Install [asciinema](https://asciinema.org/) or [vhs](https://github.com/charmbracelet/vhs)
- [ ] Record a 30-second terminal session showing:
  1. `echo '...' | bjq 'for $u in $$.users[] where $u.age > 21 return $u.name'`
  2. A group-by query on a real dataset
  3. A join that would be painful in jq
- [ ] Convert to GIF and add to README above the fold

### 1.4 Polish README

- [ ] Verify all examples in README actually work with current bjq
- [ ] Add a "Quick Install" section at the very top (binary download)
- [ ] Add benchmark summary table to README (link to full results)
- [ ] Ensure the first 5 lines communicate what bjq does and why you'd care

---

## Phase 2: Launch Day

### 2.1 Timing

- **Best days:** Tuesday, Wednesday, or Thursday
- **Best time:** 9:00–11:00 AM US Eastern (6:00–8:00 AM Pacific)
- **Avoid:** Mondays, Fridays, weekends, major tech news days

### 2.2 Post title

Pick ONE of these (test with a friend first):

1. **"Show HN: bjq – Like jq, but with SQL-like joins, grouping, and functions"**
2. **"Show HN: bjq – Query JSON with FLWOR expressions (SQL for JSON)"**
3. **"Show HN: bjq – A jq alternative with hash-joins and query optimization"**

Rules:
- Lead with `bjq`, not `Brackit` (the CLI is the hook)
- Don't say "JSONiq" in the title (too niche, filters people out)
- Keep under 80 characters

### 2.3 Post body

HN Show posts get a text field. Keep it short:

```
bjq is a command-line JSON processor (like jq) powered by a JSONiq
query engine. Where it differs from jq:

- FLWOR expressions: for/let/where/group by/order by/return
- Automatic hash-join optimization for multi-source queries
- User-defined functions with full recursion
- Python-style array slicing

Quick start:
  curl -L <release-url> -o bjq && chmod +x bjq
  echo '{"users":[{"name":"Alice","age":30},{"name":"Bob","age":25}]}' | \
    bjq 'for $u in $$.users[] where $u.age > 26 return $u.name'

On a 100k-record join, bjq finishes in Xs while jq takes Y minutes
(hash-join vs nested loop).

GitHub: https://github.com/sirixdb/brackit
```

Replace X and Y with actual benchmark numbers.

### 2.4 Prepare a top-level comment

Post this as the first comment immediately after submitting:

```
I maintain Brackit, a JSONiq query engine originally built during a PhD
at TU Kaiserslautern. We recently added bjq as a more accessible
entry point.

The core insight: jq is fantastic for simple field extraction, but once
you need joins, grouping, or multi-step aggregation, its syntax becomes
write-only. bjq uses the same FLWOR expressions that SQL developers
already know (for = FROM, where = WHERE, group by = GROUP BY, etc.)
and the engine automatically converts joins to hash-joins.

Honest trade-offs:
- jq is faster for simple queries (C vs JVM, though our native binary
  closes the gap)
- jq's ecosystem is much larger (more tutorials, Stack Overflow answers)
- bjq shines on analytical queries over large JSON datasets

Happy to answer any questions about the query optimizer, the JSONiq
language, or how SirixDB uses Brackit for temporal queries.
```

### 2.5 Be available

- **First 2 hours are critical** — respond to every comment quickly
- Have these ready to paste:
  - Link to `examples/bjq-vs-jq.md` for side-by-side comparisons
  - Link to benchmark results
  - One-liner install command
- Common questions to prepare for:
  - "Why not just use jq?" → joins, grouping, optimization
  - "Why Java?" → JVM enables SIMD via Vector API, GraalVM gives native binaries
  - "How does it compare to DuckDB?" → DuckDB is SQL-first for analytics;
    bjq is JSONiq-first for hierarchical JSON with nested arrays/objects
  - "What about streaming?" → Brackit uses lazy evaluation via iterators
  - "Array indexing at 0?" → Yes, deliberate choice for developer ergonomics

---

## Phase 3: Post-Launch (first 48 hours)

### 3.1 Monitor and engage

- [ ] Check HN every 30 minutes for the first 4 hours
- [ ] Respond to technical questions with specifics (link to source code)
- [ ] Don't argue with critics — acknowledge limitations honestly
- [ ] If someone files a bug, fix it fast and comment back with the fix

### 3.2 Capture momentum

- [ ] Tweet/post about the HN discussion with a link
- [ ] Post to relevant subreddits if HN goes well:
  - r/programming, r/commandline, r/json
- [ ] Update README with "As seen on Hacker News" if it hits front page
- [ ] Track GitHub stars, clones, and release downloads

### 3.3 Follow-up content (next 1–2 weeks)

Ideas for follow-up posts or blog entries:
- "How Brackit's query optimizer turns O(n*m) joins into O(n+m)"
- "Building a jq alternative in Java: lessons learned"
- "SIMD-accelerated JSON query processing with Java's Vector API"
- "JSONiq vs jq vs SQL: choosing the right query language for JSON"

---

## Checklist Summary

```
Pre-launch:
  [ ] Native binaries build and work
  [ ] Benchmarks run, numbers recorded
  [ ] Demo GIF in README
  [ ] README polished with quick install
  [ ] All examples verified working

Launch day:
  [ ] Post between 9-11am ET, Tue-Thu
  [ ] Title leads with bjq
  [ ] First comment posted immediately
  [ ] Available for 2+ hours after posting

Post-launch:
  [ ] Respond to all comments
  [ ] Fix any reported bugs same-day
  [ ] Cross-post if HN goes well
  [ ] Plan follow-up content
```
