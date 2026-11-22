# Verifying correctness (especially of AI-generated code)

[docs/DEVELOPMENT.md](DEVELOPMENT.md) explains *why* this project verifies the way it does. This
document is the operational half: which layers exist, what bug class each one catches, how to run
it, and the exact rules of the pull-request mutation lane.

The ordering principle is that layers must fail for *different* reasons. A suite of twenty tests
that all go wrong together when the same assumption is wrong is one test. Most of what follows is
chosen because it fails independently of the layer above it.

## Layer map

| Layer | What it proves | How to run |
|---|---|---|
| Behaviour tests (JUnit 5) | A query's result sequence, its error code and its serialized output match the specification of record. Most of the suite extends [`XQueryBaseTest`](../src/test/java/io/brackit/query/XQueryBaseTest.java), which supplies the context and store; [`ResultChecker`](../src/test/java/io/brackit/query/ResultChecker.java) compares sequences item by item. | `mvn test` |
| Repeated-evaluation check | A compiled plan gives the same answer when evaluated twice. `ResultChecker.dCheckS` evaluates its cursor twice and asserts the same result both times, which catches state a plan leaves behind — the classic defect of an operator that consumes an iterator it does not own. Use it, rather than `checkS`, for any new cursor assertion. | part of `mvn test` |
| Optimizer-equivalence corpus | The same 20 XMark queries and the same golden outputs under three optimizer configurations: unnesting off ([`XMarkTestNested`](../src/test/java/io/brackit/query/XMarkTestNested.java)), unnesting on ([`XMarkTestUnnested`](../src/test/java/io/brackit/query/XMarkTestUnnested.java)), and unnesting plus join detection ([`XMarkTestUnnestedJoin`](../src/test/java/io/brackit/query/XMarkTestUnnestedJoin.java)). A rewrite that changes an answer fails here even when every unit test passes, because the unoptimized arm is an independent oracle for the optimized one. | part of `mvn test` |
| Golden-output corpora | `src/test/resources` holds the XMark, TPoX and TPC-H query texts and their expected output. Serialization is part of the contract, so these compare the printed result, not an in-memory structure. | part of `mvn test` |
| Changed-line mutation testing | That the tests covering a change *assert* something: PIT mutates the changed lines and reports which mutants no test noticed. This is the only layer that measures assertion strength rather than execution. | [PR mutation testing](#pr-mutation-testing) |
| Mutation-lane tooling tests | The reporter's own behaviour — selection, changed-line attribution, test resolution, the verdict and every disposition rule — as behaviour tests over fixtures. | `python3 -B -m unittest discover -s .github/scripts/tests -p 'test_pr_mutation*.py'` |
| JMH benchmarks | Timed cost of compilation and evaluation in process: [`QueryBenchmark`](../src/test/java/io/brackit/query/QueryBenchmark.java), [`SIMDBenchmark`](../src/test/java/io/brackit/query/util/simd/SIMDBenchmark.java). | run the JMH class directly |
| End-to-end timed comparison | Wall-clock cost of the `bjq` binary against `jq` over generated corpora: `examples/benchmark.sh`, `benchmark-3way.sh`, `benchmark-1b.sh`, which produced the README's published numbers. Timed numbers from either harness are a claim only under the [benchmark protocol](DEVELOPMENT.md#the-benchmark-protocol). | `./examples/benchmark.sh --sizes "10000 100000"` |

**What is deliberately not on this list.** Brackit has no property-based generator harness, no
model-based differential oracle against a second processor, and the
[W3C QT3 test suite](https://github.com/w3c/qt3tests) is not wired into CI. Those are the obvious
next layers for a language with a published conformance corpus, and they are open work.
[CONV-102](../spec/CONV-conventions.md#conv-102--conformance-and-evidence-honesty) is the reason
this paragraph exists rather than a row in the table: a layer map that lists harnesses the
repository does not contain is worse than a short one, because it tells a reviewer that a bug class
is covered when nothing covers it. Where a change needs one of these, build it in that change and
add the row.

## Why these layers, specifically

Query-engine defects cluster in three shapes, and each layer above answers one of them.

**A wrong answer that looks right.** The behaviour tests and the golden corpora catch this only if
their oracle came from the specification rather than from a previous run of the engine. This is why
[DEVELOPMENT.md §2](DEVELOPMENT.md#2-tests-are-derived-from-the-requirements-not-from-the-code)
insists on the derivation order: a golden file regenerated from the engine's current output records
whatever the engine currently does, including the bug, and then defends it forever.

**A rewrite that is correct for the query you tested.** An optimizer is a claim that two plans are
equivalent, and the counterexample is usually a different query, not a different input. The
optimizer-equivalence corpus attacks this directly by running the unoptimized plan as the oracle for
the optimized one. It is the cheapest high-value layer in the repository, and widening it — more
queries, more configurations — is usually a better investment than another unit test.

**A test that executes the change and checks nothing.** Neither of the layers above can see this:
the suite is green either way, and coverage reports the line as covered. Only mutation testing
distinguishes "a test ran this line" from "a test would notice if this line were wrong", which is
why the lane below exists and why its verdict is about assertions rather than about percentages.

## PR mutation testing

`PIT changed-source mutation testing` in [build.yml](../.github/workflows/build.yml) runs on
`pull_request` events after the short `Python mutation tooling` job. It compiles the project itself
rather than depending on the test lane, so it adds no dependency to `Test` or `publish` and cannot
delay them. Its **30-minute job limit** bounds the whole job; PIT processes share a **20-minute
execution budget**, leaving room for compilation, setup and report upload. Each module may use all
the time still left except a `MIN_MODULE_SECONDS` floor for every module after it; Brackit is a
single Maven module, so in practice one module receives the whole budget. A deadline kills the
process group, preserves partial reports and explicitly reports incomplete analysis. A large diff
may need follow-up focused runs; timeouts and missing evidence are never a clean score.

Scope comes from `git diff --diff-filter=ACMRT <base>...<head>`: added, modified and renamed main
Java sources only — paths under `src/main/java` or `src/examples/java`, which the `build-helper`
plugin adds as a second main source root so that both compile into the same `target/classes`.
Deleted files, tests, resources, docs and `package-info.java`/`module-info.java` are excluded. Both
diff revisions must be full hexadecimal commit IDs; option-like values are rejected before Git runs.
Diff paths must be canonical relative paths and selected sources must exist in the head commit.
Literal pathspecs follow `--`; source symlinks cannot escape the module, which for Brackit's single
root module means the repository. Every subprocess uses an argument list with `shell=False`.

Changed **lines** come from a second diff, `git diff -U0 --merge-base <base> -- src`, whose base side
is the merge base of `<base>` and the checked-out revision and whose new side is the **working
tree** — the exact content the job compiled and PIT mutates — so every mapped line number describes
the file that was actually mutated. The job therefore compiles with `-Dformatter.skip=true`: the
formatter plugin otherwise rewrites sources during `process-sources`, and a reformatted working tree
would no longer be the revision the line map describes. Rename detection runs over the whole `src/`
diff rather than per path, so a renamed-and-edited file contributes only its edited lines and a pure
rename contributes none; deleted lines contribute no new-side line, and a deleted file
(`+++ /dev/null`, excluded from the selection anyway) contributes nothing. Hunks are attributed
through their `+++ b/<path>` header and kept only for already selected sources; a hunk that precedes
any file header, an unparsable hunk header and a path Git had to quote all fail selection instead of
silently reporting a file as untouched. `selection.json` records `modules` (what to mutate),
`changed_lines` (what to judge, one entry per selected source), and `dispositions_base` (the
dispositions file at the merge base, or null when absent); `run` refuses a manifest that does not map
every selected source rather than judging nothing.

The checkout is the PR merge revision. JVM `SourceFile` attributes map source files to **exact
compiled class names**, including nested, anonymous and extra top-level classes. No prefix glob or
whole-module fallback broadens mutation scope. Unmapped sources fail analysis explicitly (for
example a file with no emitted bytecode); justify these individually in the PR. A new Maven module
would need an entry in `MODULES` in [pr_mutation.py](../.github/scripts/pr_mutation.py); that map
exists so the one-module case is a configuration value rather than an assumption spread through the
reporter.

Test selection is resolved **per changed source** and is never widened to the module suite. PIT's
coverage phase has to execute every class in `targetTests`, and the corpus tests parse and run whole
XMark, TPoX and TPC-H query sets, so whole-module coverage collection can exhaust the budget before
mutation analysis starts. Test identities come from declared top-level Java type names and packages,
not source filenames or directories. Discovery does not require compiled test classes. For a source
`Foo` in package `p` the first matching step wins:

1. `p.FooTest` or `p.FooTests`.
2. Any test in the module whose simple name starts with `Foo` — or with `Foo` minus a leading
   `Abstract` or trailing `Impl` — at a word boundary and ends with `Test`, `Tests` or `IT`.
   The remainder has to begin a new word, so `Path` reaches `PathParserTest` but never a merely
   similar `PathologyTest`, and `AbstractSequence` reaches `SequenceTest`.
3. Tests in `p` and its subpackages ending with `Test`, `Tests` or `IT`, only when there are at most
   `PACKAGE_TEST_LIMIT` of them.
4. Otherwise the source is **unmapped**.

To select an existing behaviour test that the name-based rules miss, add an association in
[`.github/mutation-test-associations.toml`](../.github/mutation-test-associations.toml).
[CONV-204](../spec/CONV-conventions.md#conv-204--lane-test-associations) owns the schema, declared-class
ownership rules, validation, module cap and PIT baseline exclusions. The file also owns the current
association inventory.

PIT samples at most `MUTANT_BUDGET / changed classes` mutants per class, clamped to
`[MIN_MUTANTS_PER_CLASS, MAX_MUTANTS_PER_CLASS]`, so a module's work scales with the size of the
diff: one changed class gets every mutant the cap allows, while a 57-class diff samples a few per
class. This is what keeps a large diff inside the budget once the test scope is correct — a mutant
whose covering test hangs costs a whole minion restart, and those restarts, not the mutant count, are
the dominant cost. The limit is PIT's `CLASSLIMIT` feature, which distributes the sample evenly
across each class rather than taking the first mutants; it does not guarantee that every changed
method is exercised. PIT rejects the older `maxMutationsPerClass` setting outright and names this
feature as its replacement. The job summary records the per-class limit per module. For every mutant
of a class, omit the limit in a focused rerun using the artifact's `targets.txt`, as in the example
below.

A source that resolves to nothing never changes the scope chosen for the other changed sources; the
union is capped at `MODULE_TEST_LIMIT` distinct declared test names. Named matches are admitted
before related matches, then package fallbacks; any source whose full candidate set would exceed the
cap is marked `over-limit`, including a named match. Selected names receive a trailing `*` to include
nested classes; other classes sharing that prefix can also match. When any tests are selected, all
changed classes remain in `targetClasses`; mutants without coverage from those tests report
`NO_COVERAGE`. A source without its own mapping may still be covered by tests selected for another
source. `results.json` records unresolved sources (`unmapped` or `over-limit`) and every source's
resolution kind; the job summary lists unresolved sources. A module where nothing maps reports state
`unmapped` instead of running PIT without a baseline. This focused selection can omit the corpus
assertions; if a survivor is already killed by a broader deterministic test, rerun with that test
included and link the report. Benchmarks are excluded from the PR mutation baseline by the
`pr-mutation` profile: they are not behaviour tests, and exploration belongs in its own lane with
discovered seeds promoted to deterministic regressions.

The `pr-mutation` artifact contains source selection with its changed-line map, exact targets and
test filters, every undetected mutant in `results.json`, HTML/XML reports, logs and `summary.md`.
The job summary shows, per module, the state, the number of mutated classes and test patterns, the
unmapped sources, the changed-line counts that decide the outcome (changed lines, mutants on them,
killed, survived, no coverage), the whole-report killed/total separately from PIT's detected/total
score (which includes timed-out mutants), status counts, up to 100 failing changed-line mutants and
— separately, marked advisory — the unchanged-line totals with the classes carrying the most
undetected mutants; the artifact retains the full list. `results.json` holds `modules` (one result
per module, with that split under `changed` and `unchanged`), `dispositions` and `associations`;
each module's `changed` lists its `failing`, `dispositioned_equivalent` and
`dispositioned_out_of_lane` mutants. Each module's `unattributed` counts mutants whose source file
never mapped to a changed source (normally zero, since `targetClasses` holds only changed classes).
Zero mutations, setup failures, malformed or missing XML and timeouts are incomplete, not 100%.

### Blocking criterion: the PR's own lines

[CONV-202](../spec/CONV-conventions.md#conv-202--pr-mutation-verdict-with-dispositions) owns the
clean-run criterion, including reviewed dispositions; association validation is specified by
[CONV-204](../spec/CONV-conventions.md#conv-204--lane-test-associations). A mutant sits on a changed
line when its `<sourceFile>` and the package of its `<mutatedClass>` match a changed source and its
`<lineNumber>` is in that file's changed-line set; nested, anonymous and extra top-level classes are
therefore attributed to the file they were compiled from. Use the
[disposition helpers below](#reviewed-dispositions) to inspect claims alongside a local report.

Mutants on **unchanged** lines of the same classes are advisory: the summary and `results.json`
report their counts and the classes carrying the most of them. This debt alone never fails the job;
disposition validation still applies to entries matching these mutants (CONV-202). They are
pre-existing test debt the PR did not introduce, and a whole-class criterion would leave every
larger PR red however completely its own change is tested. Reduce that debt in dedicated work rather
than by widening a PR.

Incomplete analysis still fails on its own, independently of any line: a module that timed out,
errored, mapped to no test (`unmapped`), produced zero mutations, or attributed none of its mutants
to a changed source is never clean — an empty attribution judges nothing and must not read as a
pass. PIT's own `mutationThreshold` and `coverageThreshold` stay at 0, so PIT never decides the
outcome; the reporter does.

**Known limitation.** Per-class mutant sampling is chosen before the line map is applied and, when
the `CLASSLIMIT` limit binds, spreads its sample evenly over each class, so a large diff in a large
class can leave zero mutants on changed lines — nothing to judge, and the job stays green. The
summary always prints a module's changed-line count beside its changed-line mutant count, so `0`
mutants judged out of many changed lines is visible rather than silent; a focused rerun without the
limit (below) includes every PIT-generated mutant of the changed classes, including those on
unchanged lines.

### Reviewed dispositions

Resolve mutants under
[CONV-103](../spec/CONV-conventions.md#conv-103--specification-before-implementation). For a reviewed
disposition, edit [`.github/mutation-dispositions.toml`](../.github/mutation-dispositions.toml),
which includes an example entry.
[CONV-201](../spec/CONV-conventions.md#conv-201--mutation-disposition-record) owns the schema,
equivalence standard and counterfactual evidence requirements;
[CONV-202](../spec/CONV-conventions.md#conv-202--pr-mutation-verdict-with-dispositions) owns the
acceptance and re-confirmation rules. Review each added claim in the job summary under
[CONV-203](../spec/CONV-conventions.md#conv-203--disposition-review-abuse-limits-and-refactoring).

After compiling and generating PIT reports, use the local helpers below. Check and remap read the
reports without changing files. Counterfactual temporarily applies a patch and runs the supplied test
command before restoring the sources; its supported patch operations are defined by CONV-201. CI
never runs these helpers, and none writes the dispositions file.

```bash
# Judge entries against local PIT reports as the lane would (stale, re-confirmation, unconfirmed).
python3 .github/scripts/pr_mutation.py dispositions --check --base "$(git rev-parse origin/master)" \
  --report /tmp/pr-mutation/brackit/report/mutations.xml
# Propose new identities for entries a rename, move or edit left stale; review before using.
python3 .github/scripts/pr_mutation.py dispositions --remap \
  --report /tmp/pr-mutation/brackit/report/mutations.xml
# Evidence for killed-out-of-lane: the test passes, fails with the injected mutant, and the
# sources are restored byte-identically (SHA-256 checked). Prints the [mutant.evidence] table.
python3 .github/scripts/pr_mutation.py counterfactual --patch mutant.patch -- \
  mvn -B test -Dtest=io.brackit.query.SomeAllocationTest
```

Pass `--sampled` to `--check` when the reports came from a run with a per-class limit.

The reporter itself is covered by behaviour tests over fixtures
([`.github/scripts/tests`](../.github/scripts/tests)), run with branch coverage in the
`Python mutation tooling` job. A coverage floor is appropriate there and nowhere else: these are
small pure functions with no specification of their own beyond this document, which is exactly the
case [DEVELOPMENT.md §5](DEVELOPMENT.md#5-coverage-is-a-diagnostic-not-a-target) carves out.

The job runs on normal `pull_request` events, including forks and Dependabot, with only
`contents: read`, no secrets and no persisted checkout credentials. Never use `pull_request_target`
or add credentials to run untrusted PR code. Reports are artifacts and job summaries, so reporting
needs no PR write token.

**Required or advisory:** the job is **advisory** by decision, and starts that way because the lane
has never run on `master`. It has `continue-on-error`, so it reports without gating: its scope is the
PR's own changed lines, a red result is normally a test-quality finding for the author rather than a
broken build, and the per-source test mapping is a heuristic that can legitimately find no test. It
stays visibly informational even when the
[CONV-202 verdict](../spec/CONV-conventions.md#conv-202--pr-mutation-verdict-with-dispositions) fails
its analysis step. Once representative PRs meet that criterion, maintainers can set the repository
Actions variable **`MUTATION_BLOCKING=true`** and require `PIT changed-source mutation testing` in
branch protection. That single switch removes job-level tolerance; the reporter already applies the
CONV-202 verdict. Revert the variable to restore informational rollout while investigating an
infrastructure failure.

Local reproduction (JDK 25):

```bash
python3 -B -m unittest discover -s .github/scripts/tests -p 'test_pr_mutation*.py'
mvn -B -DskipTests -Dformatter.skip=true test-compile
python3 .github/scripts/pr_mutation.py select \
  --base "$(git rev-parse origin/master)" --head "$(git rev-parse HEAD)" --output /tmp/pr-mutation
python3 .github/scripts/pr_mutation.py run --output /tmp/pr-mutation --seconds 1200
# Add --blocking to enforce the clean-run criterion locally (CONV-202, CONV-204).
```

For a broader deterministic test selection, or for every mutant of a class rather than the lane's
sample, reuse the artifact's `targets.txt` and run the goal directly. Omitting
`-DprMutationFeatures` disables `CLASSLIMIT`, which is what makes a sample provably complete:

```bash
mvn -B -Ppr-mutation org.pitest:pitest-maven:mutationCoverage \
  -DprMutationTargetClasses="$(paste -sd, /tmp/pr-mutation/brackit/targets.txt)" \
  -DprMutationTargetTests='io.brackit.query.util.path.PathTest*' \
  -DprMutationReportDir=/tmp/pr-mutation-expanded
```

The opt-in [`pr-mutation` profile](../pom.xml) owns every PIT setting and is absent from every normal
lifecycle, so the ordinary test lane keeps its timing. It requires both the class and the test list,
so an unset property fails the run instead of silently mutating the whole engine. Minions inherit
surefire's `argLine`, so they run with the same module and preview flags as the ordinary test lane,
plus a bounded CI-sized heap.

## Running the layers

```bash
# Every behaviour test, the repeated-evaluation checks and all three optimizer configurations
mvn clean test

# One test class, or one method
mvn test -Dtest=XMarkTestUnnestedJoin
mvn test -Dtest=NumericCorrectnessTest#sumOfBooleanRaisesTypeError

# The mutation lane's own behaviour tests
python3 -B -m unittest discover -s .github/scripts/tests -p 'test_pr_mutation*.py'
```

## Checklist for new (AI-generated) changes

1. The specification section (or the requirement in `spec/`) is identified before the code, and the
   test's expected result came from it rather than from running the engine.
2. For a bug: the regression test was seen to fail on `master`, and the write-up says why the
   existing layers missed it.
3. Every assertion is on observable behaviour — result sequence, error code, serialized output,
   documented cost — and none reads private state or a plan shape for its own sake.
4. A change to the compiler or optimizer is checked against an unoptimized arm, not only against its
   own expected output.
5. The mutation artifact was read, and every surviving or uncovered changed-line mutant is resolved
   by a behaviour test, an association, a reviewed disposition with its evidence, or deletion.
6. No test in the change exists to cover a line or kill a mutant.
7. A cost contract carries the assertion that bounds it, on counted work rather than wall-clock time.
8. The PR states the intent it is to be reviewed against.
