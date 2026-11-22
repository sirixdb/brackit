# How Brackit is developed

A query engine fails quietly. A storage bug corrupts a file and something eventually crashes; a
compiler or evaluation bug returns a sequence, in the right type, of a plausible length, and the
caller believes it. There is no stack trace to read and no checksum to fail. Worse, Brackit is
embedded: SirixDB runs its queries through this engine, and SirixDB never overwrites, so an answer
this engine computes today can be the answer someone reads out of an old revision in five years.
A rewrite rule that is subtly wrong is therefore not a local defect. It is a wrong answer
distributed over every query that the rule happens to fire on.

The thing that makes this tractable is unusual, and the whole strategy is built on it: **what
Brackit must do is already written down by someone else.** XQuery 3.1, XPath and XQuery Functions
and Operators 3.1, and JSONiq are published, sectioned, and specific down to the error code a
function raises on a bad operand. "What is correct here?" is almost never a matter of taste in this
project. It is a lookup.

This document is the project's development strategy: how we decide what to build, how tests come to
exist, how we know a change is finished, and why each of those practices is the way it is. It is
written for contributors and for the AI agents that do much of the work here. Both need the reasons
and not only the rules, because a rule whose reason is unknown gets satisfied literally and defeated
in substance. TigerBeetle's [TIGER_STYLE.md](https://github.com/tigerbeetle/tigerbeetle/blob/main/docs/TIGER_STYLE.md)
and SQLite's [How SQLite Is Tested](https://www.sqlite.org/testing.html) are the models for its
form: one public narrative instead of folklore spread over review comments. It is the same strategy
[SirixDB](https://github.com/sirixdb/sirix/blob/main/docs/DEVELOPMENT.md) develops under, adapted to
an engine that owns no bytes on disk.

It explains the *why*. The authoritative detail lives in the documents it points to:

| Document | What it owns |
|---|---|
| [spec/README.md](../spec/README.md) | The specifications of record, how to cite them, and where Brackit's own extensions are specified. |
| [spec/CONV-conventions.md](../spec/CONV-conventions.md) | Requirement conventions, evidence rules and the mutation dispositions (CONV-201 to CONV-204). |
| [docs/VERIFICATION.md](VERIFICATION.md) | Every verification layer, the bug class it catches, how to run it, and the PR mutation lane. |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | Build, branch and pull-request mechanics. |
| [docs/postmortems/TEMPLATE.md](postmortems/TEMPLATE.md), [docs/LESSONS.md](LESSONS.md) | Bug write-ups and the lessons taken from them. |
| [README.MD](../README.MD) | The language Brackit implements, its documented extensions, and the published benchmark numbers. |

## 1. The specification is the source of truth

Behaviour is decided before it is implemented, and for almost everything Brackit does it was decided
by a W3C working group or by the JSONiq specification. The specifications of record are:

- [XQuery 3.1](https://www.w3.org/TR/xquery-31/) — syntax, static and dynamic semantics, FLWOR.
- [XPath and XQuery Functions and Operators 3.1](https://www.w3.org/TR/xpath-functions-31/) — every
  built-in function, its signature, its result and the error it raises.
- [XQuery and XPath Data Model 3.1](https://www.w3.org/TR/xpath-datamodel-31/) and
  [Serialization 3.1](https://www.w3.org/TR/xslt-xquery-serialization-31/) — the item model and the
  output Brackit must produce.
- [JSONiq](https://www.jsoniq.org/) — the JSON extensions, the update facility and object/array
  navigation.
- Brackit's own documented extensions, listed in
  [Differences from Standard JSONiq](../README.MD#differences-from-standard-jsoniq): zero-based
  array indexes, `$obj{field,...}` projection, Python-style slices, and statement syntax.

**A citation is the unit of traceability here.** Sirix numbers its own requirements because nobody
else has written down how its pages work. Brackit does not need to: a W3C section number and an
error code are already stable, already public, and already mean the same thing a year later. So a
behaviour test names the thing it checks — the specification section, the function, and the error
code where one applies — in its test name, its Javadoc or a comment on the assertion, and an issue
or pull request about a behaviour cites the same. [`ErrorCode`](../src/main/java/io/brackit/query/ErrorCode.java)
already carries those codes with their `err:` names in its Javadoc, so the citation in a test and
the constant in the engine are the same identifier.

SQLite's test suite is written against its documented requirements, with evidence marks tying each
requirement to the tests that check it. We take that model with W3C's text in the place of SQLite's:
**every behaviour test cites what it verifies, and a specified behaviour that no test cites is
visible as a gap.** Tracing both directions is what makes the suite auditable — a test that cites
nothing is as visible as an uncited requirement.

Where the specifications are silent, Brackit is on its own and the Sirix discipline applies in full:
**write the requirement first.** An extension, a new optimization's observable contract, or an
engine-specific error gets a numbered requirement in [`spec/`](../spec/README.md) before it is
implemented, stating the behaviour, the invariants, the errors and the cost — and the change begins
by filling that gap rather than by guessing. The guess becoming permanent is exactly what the order
prevents, and in a language with a published specification an undocumented extension is worse than
a gap: it is a dialect nobody can check.

The second rule of the specification is honesty about its own evidence
([CONV-102](../spec/CONV-conventions.md#conv-102--conformance-and-evidence-honesty)). A behaviour is
claimed as conforming only when it exists and a mapped deterministic test passed in a default CI
lane; otherwise the claim says what is partial, absent or untested. A specification that overstates
its own evidence is worse than no specification, because it moves the reader's attention away from
the one part that needs it.

## 2. Tests are derived from the requirements, not from the code

A test written after the implementation, by reading the implementation, re-states what the code
does. It passes, it survives mutation, and it proves nothing about whether the behaviour is right —
it has recorded the bug along with everything else. In an engine whose correct answers are published
this failure is both more tempting and less excusable: the oracle is a click away, and running the
query to see what comes out is always easier. So:

- **Feature tests are derived from the specification, before the implementation or independently of
  it.** Read the section, write the expected sequence and the expected error code, then make it
  pass. Writing the oracle from the specification forces the requirement to be precise enough to
  test, which is most of the value of writing it down at all. The result it should produce comes
  from the text, never from the engine's current output.
- **A bug fix carries a regression test that fails on `master` first.** The failing run is the only
  evidence that the test actually observes the bug. A test added beside a fix and never seen to fail
  may be checking something adjacent to the defect, and will keep passing the next time the defect
  returns.
- **Implementers never weaken a spec test.** If an implementation disagrees with a test derived from
  a requirement, either the implementation is wrong or the requirement is wrong. Changing the test's
  oracle to match the code turns a caught failure into a silent behaviour change — and when the
  requirement is a W3C recommendation, changing the oracle is simply leaving the language. Where
  Brackit does deviate on purpose, the deviation is documented as an extension
  ([CONV-105](../spec/CONV-conventions.md#conv-105--compatibility-and-versioning)) in its own change,
  with the reason recorded, and never by editing an assertion.
- **Tests assert behaviour only.** The observable surface is the result sequence, the error raised,
  the serialized output and the cost. No reading private fields of an expression, no asserting on
  the shape of an AST or an operator tree, no tests aimed at a particular line. Brackit's internals
  move constantly — that is what an optimizer is — and a test bound to them is a tax paid again at
  every rewrite while saying nothing about the contract. The deliberate exception is cost: an
  assertion that a rewrite fired, or that evaluation allocates no per-tuple garbage, is admitted
  because evaluation cost *is* part of the observable contract (§6), not because a plan dump is
  convenient to reach. Such an assertion still goes through an interface the project documents, and
  it states the cost it is defending.

### Independence without mandatory separate authorship

We do not require that a different person or agent write the tests. Independence here is bought by
four other things: the published specification, which is an oracle no implementer wrote; the
citation tracing above; review against the specification rather than against the code (§8); and
mutation testing (§4), which measures whether the tests assert anything at all, regardless of who
wrote them. For the maintainers' agent-built features one more layer is added on top: a separate
test author who works from the specification without seeing the implementation.

## 3. Requirements and acceptance criteria worth writing

A requirement states the behaviour, the invariants that must still hold after it, the errors it must
raise and its cost. It does not state how the code achieves any of that; a requirement that
describes an implementation cannot survive the implementation being replaced, and it silently
forbids better ones. For a built-in function this is usually a citation plus the cases that matter;
for an optimization it is the behaviour that must be *unchanged* by it, which is the part that
rewrite bugs violate.

Acceptance criteria must be observable through an interface the project supports — a result
sequence, an error code, serialized output, a documented counter. "Works correctly" is not
acceptance; "`sum((1, true()))` raises `err:FORG0006` rather than throwing `ClassCastException`" is,
and "the join is still correct when the build side is empty" is.

For a documentation or CI change, name the observable acceptance checks — the links resolve, the
generated file matches, the validator rejects this fixture — instead of inventing application tests.
A unit test invented for prose asserts only that the prose exists, and it has to be maintained
forever.

## 4. Mutation testing, as a complement

A suite can execute every line of a change and assert almost nothing. This is the characteristic
failure of machine-written tests, and of human tests written against the code: they run the query,
then check something that was going to be true either way — that the result is non-empty, that no
exception was thrown, that the count matches. Mutation testing is the one layer that measures this
directly. PIT changes the production code — flips a comparison, drops a call, returns a constant —
and reports which changes no test noticed. Coverage says a line ran; a killed mutant says a test
disagreed with a wrong version of it.

The pull-request lane judges a PR by **its own changed lines**, not by the classes it touched. A
whole-class criterion leaves every large change red for test debt it did not introduce, which
teaches authors to shrink diffs rather than to test them; the lane's exact attribution and
completeness rules are in
[VERIFICATION.md](VERIFICATION.md#blocking-criterion-the-prs-own-lines).

Every surviving or uncovered changed-line mutant is resolved in exactly one of four ways
([CONV-103](../spec/CONV-conventions.md#conv-103--specification-before-implementation)):

1. **A behaviour test, derived from a requirement, detects it.** The normal outcome. The mutant has
   pointed at a specified behaviour whose test was too weak, so the fix is a better oracle for that
   behaviour — not a test aimed at the mutant. Sometimes such a test already exists and the lane
   simply did not select it; then the fix is an association that makes the lane run it
   ([CONV-204](../spec/CONV-conventions.md#conv-204--lane-test-associations)), never a record
   claiming the mutant is harmless.
2. **It is recorded as equivalent, with a reason.** The argument must come from the specification,
   the public contract or a documented invariant, and must show that no input the contract admits
   can tell the original from the mutant. Observable behaviour includes results, errors, serialized
   output and cost, so a mutant that only changes cost is not equivalent: it belongs to an
   allocation or counted-work test — in the lane where that test can run, or under (3) when it
   cannot — and a cost that no test bounds needs such a test of its own. One narrow exception to the
   cost rule is accepted: a mutant that produces the same result and differs only at a single
   boundary input, by at most one extra constant-time step, may be recorded as equivalent when its
   justification says exactly that.
3. **It is recorded as killed out of lane, with counterfactual evidence.** Some effects are only
   checked by tests that cannot run under mutation instrumentation, such as a clean-JVM allocation
   measurement. The record names that test and carries the evidence that it fails when the mutant is
   injected and passes without it: the injection patch, the command, both exit statuses, the failing
   output and the hashes of the touched sources.
4. **The mutated code is removed.** A mutant with no observable effect at all is telling you the
   code has none either.

An *uncovered* changed-line mutant is never recorded as either kind: a mutant that no test executed
has shown nothing about the code, so only a behaviour test, a lane association that makes existing
tests run, or removing the code resolves it. Untested new code is a finding, not the absence of one.

Both kinds of record are per-mutant, reviewed beside the diff and re-confirmed when their method
changes. There are no class-, file-, line- or mutator-wide waivers, because a waiver that broad
removes the signal silently and permanently. The full rules — the record format, the verdicts, the
review obligation and the local helpers — are CONV-201 to CONV-204 in
[spec/CONV-conventions.md](../spec/CONV-conventions.md), with the workflow in
[VERIFICATION.md](VERIFICATION.md#pr-mutation-testing).

**A test is never written to kill a mutant or to cover a line.** Such a test is necessarily bound to
the implementation that produced the mutant, which is what §2 forbids, and it makes the suite worse
while making the report green. A surviving mutant is a question about the specification; answer that
question.

## 5. Coverage is a diagnostic, not a target

An uncovered branch in changed code is treated exactly like a surviving mutant: either some
specified behaviour needs this code and gets a specification-derived test, which then happens to
cover it, or the code has no specified behaviour and is removed. There is no coverage percentage to
hit for engine code, and a test is never written to light up lines.

SQLite reaches 100% branch and MC/DC coverage, and it gets there through requirement-derived tests
in a dedicated harness over a feature set that barely changes. We take that principle and not the
number: a coverage percentage used as a gate in an engine whose optimizer is actively changing buys
line-shaped tests — the implementation-bound kind §2 rejects — and the number then certifies them.
(One lane-level floor does exist, on the Python CI helper scripts that run the mutation lane; that
is CI plumbing, where a line-shaped test is exactly the right thing, not a target for engine code.)

## 6. Performance is part of correctness

Brackit owns no bytes on disk, so it has none of the page-level counters a storage engine guards.
What it does own is the cost of answering a query: how many times a tuple is copied through the
FLWOR pipeline, whether a path is evaluated lazily or materialized, whether a rewrite fires, and how
much garbage an expression allocates per item. Those are contract, not preference. An engine that
quietly starts materializing a lazily evaluated sequence is as broken as one that returns the wrong
sequence, and nobody notices until the input is large.

**Be honest about what exists.** Brackit has no standing work-budget harness and no official budget
inventory; Sirix has both, and this is one of the places the two projects genuinely differ. The
consequence is a rule rather than a tool: when a change's cost is part of what it must achieve, the
change carries the assertion that bounds it — an allocation measurement, a counted-work assertion,
or a lazily-evaluated-sequence assertion that fails if the sequence is materialized. Such an
assertion asserts on counted work and never on wall-clock time: a time threshold on a shared runner
is flaky and cannot say what changed, while a count is exact on any machine and names the path that
grew. A bound is never widened to get a green build, and a wall-clock assertion never replaces one.

### The benchmark protocol

Counted work is blind to the same work done more slowly, which is the second layer's job. Brackit
has two kinds of timed harness, and they are not interchangeable. In-process measurements use
standalone manual timing programs in `src/test/java`, run via their `main` methods:
[`QueryBenchmark`](../src/test/java/io/brackit/query/QueryBenchmark.java) times evaluation of
precompiled queries over generated JSON data, and
[`SIMDBenchmark`](../src/test/java/io/brackit/query/util/simd/SIMDBenchmark.java) times SIMD and
scalar operations over generated arrays. Both use `System.nanoTime()`, not JMH; neither measures
query compilation. End-to-end comparisons are the shell harness in `examples/` (`benchmark.sh`,
`benchmark-3way.sh`, `benchmark-1b.sh`), which produced the
published `bjq`-versus-`jq` numbers in the [README](../README.MD); those were medians of three
runs, which is the README's stated method and *not* the protocol below. A performance claim without
its method is not a claim, so a timed change states which harness it used and follows this
protocol:

- **One timed campaign at a time, on a quiet machine.** Two campaigns on one box measure each other.
- **Alternating fresh-process pairs**: old, new, old, new, each arm in its own JVM. Within one
  process JIT state and warmed caches carry from the first arm into the second, and a thermally
  throttling machine can fake a large regression outright.
- **The plan is written down before the run**: what is compared, how many pairs, and the decision
  rule. A bar chosen after seeing the numbers is not a bar, and extending a run until its interval
  excludes zero invalidates the interval.
- **The bar for accepting a change on a timed path**: the paired point estimate at most +1%, the
  lower 95% bound at most 0, the median allocation delta at most 0 rounded to whole bytes, and cold
  paths included in the comparison — in a query engine the cold arm is not optional, because
  compilation happens once per query and a change that is free after warm-up can double the cost of
  a short query.
- **A disturbed run is rejected on evidence of outside load, never on its result.** Load from
  another process or a competing job justifies discarding a run and retrying it; a number that
  merely looks wrong does not, and neither does throttling that the measured workload caused
  itself. Re-running until the answer is agreeable is how a campaign fools itself.
- **A failed gate is rerun whole**, never only its failing benchmarks, and never with more pairs
  added after a failing result. Both shortcuts turn a fixed-sample decision into a search for a pass.

Latency that matters to a storage engine is gated downstream rather than here. SirixDB embeds this
engine and owns the timed campaigns and work budgets for the paths it drives; a change whose point
is query latency inside Sirix is measured there, under
[Sirix's benchmark protocol and campaign record](https://github.com/sirixdb/sirix/blob/main/docs/BENCHMARK_CAMPAIGNS.md),
and the Brackit pull request links that evidence. Brackit's own manual timing results do not
substitute for it.

## 7. Exploration beyond the happy path

Specification-derived tests check the cases someone thought of. The bugs that reach a query engine
are in the cases nobody did — an empty sequence where one item was assumed, a heterogeneous array,
a negative slice step, a predicate that is positional in one context and boolean in another, a
window clause over a sequence of one. So every change that touches load-bearing logic is also
explored: adversarial queries and inputs, property-based tests that generate and shrink their own
counterexamples, and differential comparison against an independently written oracle.

A query engine has an unusually good source of oracles, and it should be used before anything is
hand-rolled: the [W3C QT3 test suite](https://github.com/w3c/qt3tests) is a published conformance
corpus for exactly this language, and a second processor is a differential oracle for any query the
specification pins down. Neither is wired into Brackit's CI today — stating otherwise would be the
dishonesty §1 warns about — so for now each is a deliberate, recorded step in the pull requests that
use it, and the
[layer map](VERIFICATION.md#layer-map) says what each harness in the repository actually proves.

Record the cases, seeds and outcomes in the pull request, and promote every discovered failure to a
deterministic regression with its seed pinned. A random failure that is not pinned is a bug report
you cannot re-run.

## 8. Review against the written intent

Every change carries a written intent: what it must achieve, in the words of whoever asked for it.
**That intent is the acceptance contract.** Review asks whether the change does what the intent says
— not whether the code looks reasonable, which is a much weaker question. **Only the intent's owner
widens it.** A reviewer who thinks the intent is too narrow says so and asks; an implementer who
discovers that more work is needed records it and does not quietly absorb it.

The reason is that the dominant failure mode in both human and agent work is not bad code. It is
delivering something else: a scope narrowed because one part turned out to be hard, an unrequested
extra folded in, a goal reinterpreted into an easier one. Ordinary code review cannot catch any of
that, because the delivered code is self-consistent and often good. Only a comparison against the
written intent catches it, which is why the intent is written down before the work starts and is
widened only by whoever owns it.

The maintainers run this with `no-mistakes`, a pipeline that takes the intent as its input and runs
review, tests, lint, documentation checks, the mutation lane and CI against it, holding the change
at each gate until the finding is answered. **Outside contributors are not required to install it.**
Open a pull request with the intent stated plainly in the description — what it must achieve, and
which specification sections or error codes it is about — and a maintainer applies the same intent
review. PostgreSQL's commitfest process is the precedent: a named reviewer reads a submission
against what it claims to do, in rounds, before any committer takes it.

## 9. Every bug leaves a lesson

A bug fix is three artifacts, not one: a root-cause [write-up](postmortems/TEMPLATE.md), a
regression test that fails before the fix, and an entry in [LESSONS.md](LESSONS.md).

The write-up's load-bearing field is *why tests missed it*. The root cause explains this bug; that
field explains the hole that let it through, and it is the only part that prevents the next one.
Name the broken invariant and the causal chain rather than the failing line, say which layer should
have caught it, and state what changes so the next bug of that shape is caught — then make the
lesson concrete enough to apply, and extend an existing lesson when a failure mode recurs rather
than adding a near-duplicate. In this engine the recurring shapes are worth naming as they are
found: a rewrite that is correct for the query in the test and wrong for an equivalent one, an
eagerly evaluated sequence that was specified lazy, a type promotion applied in one branch of a
comparison and not the other.

## The short version

1. Find the specification section before you write code. Cite it in the issue, the test and the PR;
   where the specifications are silent, write the requirement in `spec/` first.
2. Derive the tests from the specification, before or independently of the implementation. For a
   bug, watch the regression test fail on `master` first.
3. Assert behaviour only: result sequence, error code, serialized output, documented cost. No
   private state, no AST or plan shape for its own sake, no line-targeted tests.
4. Explore beyond the happy path, and pin every discovered failure with its seed.
5. Mutation-test the changed logic. Kill each survivor with a behaviour test, record a reviewed
   disposition with its reason and evidence, or delete the code. Never write a test to kill a mutant.
6. Treat coverage as a diagnostic. Explain an uncovered branch; do not chase a number.
7. If the change has a cost contract, carry the assertion that bounds it, on counted work rather
   than wall-clock time. Never widen a bound to get a green build.
8. If it is a timed change, write the measurement plan down first, then measure it under the
   protocol in §6 — and where the latency belongs to SirixDB, measure it there and link the evidence.
9. Make every behaviour test say what it verifies, so an uncited behaviour is visible.
10. State the intent in the PR, and expect review against it rather than against the code.
11. For a bug: root-cause write-up, failing-first regression, lesson.
