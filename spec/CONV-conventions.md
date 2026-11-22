# CONV — Conventions and conformance

Chapter status: **Draft for adoption.** Code baseline: the commit that introduced this chapter.
These conventions define how Brackit states a requirement of its own, what evidence a conformance
claim needs, and the rules the PR mutation lane applies (CONV-201 to CONV-204). The language itself
is specified elsewhere; [README.md](README.md) names the specifications of record and
[../docs/DEVELOPMENT.md](../docs/DEVELOPMENT.md) explains why the project works this way.
Existing test links are evidence inventories, not assertions that those tests passed in this task.

## Scope

Document conventions, evidence honesty, and the mutation dispositions and test associations the
changed-line mutation lane enforces (CONV-201 to CONV-204). The mutation requirements are
implemented in the CI reporter. Requirements about process point at the authoritative workflow
artifacts; no engine method enforces them.

## Requirements

### CONV-101 — Normative statements and stable identity

- **Level:** MUST
- **Status:** proposed
- **Since:** 1.0
- **Code:** [README.md](README.md); [../docs/VERIFICATION.md](../docs/VERIFICATION.md)
- **Verified by:** No existing test mapped. **Planned:** a chapter contract test that parses the chapters and rejects duplicate, malformed or retired IDs, missing metadata and dangling citations.
- **Evidence class:** doc-only (unverified automation)
- **Gap/note:** This chapter is the only one that exists; no automated trace tooling is delivered here.

Behaviour that a specification of record already decides MUST be cited rather than renumbered: a
test, issue or pull request names the specification, its section or function, and the error code
where one applies. Behaviour the specifications of record do not decide — an extension, an
engine-specific error, or the observable contract an optimization must preserve — MUST be written as
a numbered `CHAPTER-NNN` requirement with Level, Status, Since, Code, Verified by, Evidence class and
Gap/note. IDs MUST NOT be reused or renumbered. Only uppercase RFC 2119/8174 keywords inside
requirement blocks, and tables those blocks explicitly incorporate, are normative. Unresolved
questions carry `open` and impose no chosen behaviour.

### CONV-102 — Conformance and evidence honesty

- **Level:** MUST
- **Status:** proposed
- **Since:** 1.0
- **Code:** [../CONTRIBUTING.md](../CONTRIBUTING.md); [../docs/VERIFICATION.md](../docs/VERIFICATION.md)
- **Verified by:** No existing test mapped. **Planned:** an evidence test that rejects an `enforced` claim with no passing default-lane result for the assessed baseline.
- **Evidence class:** doc-only (unverified automation)
- **Gap/note:** No claim in this change is promoted to `enforced` by source inspection.

A conformance claim MUST satisfy every decided MUST, including requirements whose implementation or
verification is incomplete. Status MUST NOT weaken the target. An `enforced` claim requires an
implemented behaviour and a mapped deterministic test that passed in a default CI lane; skipped,
disabled and assumption-aborted tests do not count. A planned test MUST be labelled planned. A claim
about an external specification's coverage MUST NOT be stated more strongly than the tests that cite
that specification support.

### CONV-103 — Specification before implementation

- **Level:** MUST
- **Status:** proposed
- **Since:** 1.0
- **Code:** [../CONTRIBUTING.md](../CONTRIBUTING.md); [../docs/DEVELOPMENT.md](../docs/DEVELOPMENT.md)
- **Verified by:** No existing test mapped. **Planned:** review acceptance — specification-to-test-to-change mapping and the PIT artifact with its survivor dispositions; for documentation or CI changes, an audit of links, citations and evidence instead of artificial application tests.
- **Evidence class:** doc-only (unverified automation)
- **Gap/note:** Process contract anchored in the two documents above; no engine behaviour changes with this requirement.

A feature or behavioural fix MUST identify the behaviour, invariants and errors it must satisfy
before implementation — by citing the specification of record, or by writing the requirement here
when none covers it — and MUST derive its tests from that statement before or independently of the
implementation. Each test oracle MUST distinguish at least one plausible violation independently of
implementation details. Changed logic MUST receive mutation testing. Each surviving or uncovered
mutant MUST be resolved in exactly one of four ways: (1) a behaviour test, derived from the
requirements, that detects it through observable behaviour, including the lane's selection of an
existing one (CONV-204); (2) an `equivalent` disposition (CONV-201); (3) a `killed-out-of-lane`
disposition, when only a test that cannot run under mutation instrumentation observes it (CONV-201);
or (4) removal of the mutated code, when the mutant shows that code has no observable effect. Tests
are derived from requirements; they MUST NOT be written to cover lines or kill mutants for their own
sake (CONV-202). A test that detects a mutant only by inspecting private state, an AST or operator
shape, or other implementation details that no requirement specifies MUST NOT count as resolution
(1). A bug fix MUST include the root-cause write-up, the failing-first regression and the lesson
[CONTRIBUTING.md](../CONTRIBUTING.md) requires.

### CONV-104 — Citation traceability

- **Level:** SHOULD
- **Status:** proposed
- **Since:** 1.0
- **Code:** [../docs/DEVELOPMENT.md](../docs/DEVELOPMENT.md); [../src/main/java/io/brackit/query/ErrorCode.java](../src/main/java/io/brackit/query/ErrorCode.java)
- **Verified by:** No existing test mapped. **Planned:** a citation report over the test sources that lists behaviour tests citing no specification and error codes no test cites.
- **Evidence class:** doc-only (unverified automation)
- **Gap/note:** The convention is decided; no tool reports on it yet, and the existing suite predates it, so a missing citation in an old test is debt rather than a violation.

A behaviour test SHOULD name what it verifies — the specification, the section or function, and the
error code where one applies — in its name, Javadoc or a comment on the assertion, so that an
uncited behaviour and an uncovered error code are both visible. A citation MUST be exact enough to
look up; "per the spec" is not a citation. New tests follow this convention; retrofitting the
existing suite is recorded as open work rather than claimed as done.

### CONV-105 — Compatibility and versioning

- **Level:** MUST
- **Status:** proposed
- **Since:** 1.0
- **Code:** [../CONTRIBUTING.md](../CONTRIBUTING.md); [../README.MD](../README.MD)
- **Verified by:** No existing test mapped. **Planned:** release review comparing the documented extension set and public API against the previous release and rejecting an unclassified incompatible change.
- **Evidence class:** doc-only (unverified automation)
- **Gap/note:** Pre-1.0 specification work, not a declaration that the current public classes are frozen.

A deliberate deviation from a specification of record MUST be documented as an extension before it
ships, in its own change, with the reason recorded; it MUST NOT be introduced by editing a test's
oracle. A change that removes a guarantee or rejects previously valid queries MUST be classified as
breaking and require a major release; additive compatible behaviour is minor and editorial repair is
patch. A behavioural change MUST update the requirement or extension documentation that describes it
in the same change.

### CONV-201 — Mutation disposition record

- **Level:** MUST
- **Status:** partial
- **Since:** 1.0
- **Code:** [pr_mutation.py](../.github/scripts/pr_mutation.py) `parse_dispositions`, `entry_problems`, `evidence_problems`; [mutation-dispositions.toml](../.github/mutation-dispositions.toml)
- **Verified by:** [test_pr_mutation_dispositions.py](../.github/scripts/tests/test_pr_mutation_dispositions.py) `DispositionRecordTest` (both kinds, every missing, unknown, empty or mistyped field, pattern characters per identity field, duplicates, unknown kinds, the justification floor, the counterfactual evidence form, malformed documents).
- **Evidence class:** code+test (mapped tests not yet passed in a default CI lane for this baseline)
- **Gap/note:** The identity fields are PIT `mutations.xml` elements. The six-field identity and its churn under refactoring were measured on the SirixDB lane this reporter was ported from, over 170 saved PIT reports and 40,054 mutants: the six-field identity had no collision, dropping the instruction indexes caused 23,853 collisions and replacing them by the line number 4,640, and 399 of 512 identities shared by two revisions of one change kept their identity while their line moved. Those numbers are evidence for the scheme, not a measurement on Brackit reports. The boundary exception to the cost rule is judged in review (CONV-203); the reporter validates the justification's form, not its cost argument.

A disposition MUST be an entry in the repository file `.github/mutation-dispositions.toml`, a TOML
document whose only top-level key is `mutant`, an array of tables; an absent file MUST mean no
entries. Each entry MUST have exactly the fields its kind requires, each non-empty; an unknown,
missing, empty or mistyped field MUST make the whole file invalid.

| Field | Content | Kinds |
|---|---|---|
| `class` | `<mutatedClass>`: the JVM binary name, e.g. `io.brackit.query.util.path.Path$Step` | both (identity) |
| `method` | `<mutatedMethod>`, e.g. `parse`, `<init>`, `lambda$parse$0` | both (identity) |
| `descriptor` | `<methodDescription>`: the JVM method descriptor | both (identity) |
| `mutator` | `<mutator>`: PIT's fully qualified mutator id | both (identity) |
| `indexes` | the integers of `<indexes>`, in report order: PIT's instruction ordinals within the method | both (identity) |
| `description` | `<description>`, compared as exact text | both (identity) |
| `kind` | `equivalent` or `killed-out-of-lane` | both |
| `checked_at` | full 40- or 64-hex commit ID whose PIT report the author inspected | both |
| `justification` | the argument the kind requires (below) | both |
| `test` | the detecting test as `<binary test class name>#<method name>` | `killed-out-of-lane` |
| `evidence` | the counterfactual record (below) | `killed-out-of-lane` |

The six identity fields form the mutant's identity, PIT's own mutation identifier (class, method,
descriptor, instruction indexes, mutator) plus its description. An entry MUST match a mutant only
when all six are equal as exact strings and integer lists. Fields MUST NOT be interpreted as
patterns. A value not well formed for its field MUST make the file invalid: `class` and `mutator`
MUST be dot-separated Java identifiers (`$` allowed), `method` a JVM unqualified method name
(`<init>`, `<clinit>` or a name without `.`, `;`, `[`, `/`, `<`, `>`), `descriptor` a JVM method
descriptor, and `indexes` non-negative integers; a glob, regular-expression or wildcard character
(`*`, `?`, `[`, `]`, `{`, `}`, `|`, `\`) MUST make the file invalid in every identity field except
where the descriptor grammar requires `[` for an array type. Line numbers MUST NOT be part of the
identity, so an entry survives edits that only move the mutant's line; an edit of the same method
that changes the mutant's instruction ordinal or description makes the entry stale (CONV-202). Two
entries with the same identity MUST make the file invalid. `checked_at` records evidence provenance
only; the reporter MUST validate its form, not its ancestry, because merge strategies may make it
unreachable.

**`equivalent`.** `justification` MUST argue, from the specification, the public contract or a
documented invariant, that no observable behaviour differs between the original and the mutated
program for any input the contract admits. Observable behaviour includes results, errors, serialized
output, and cost: allocation, counted work and latency. A mutant that changes only cost is therefore
never equivalent, with one narrow exception: a mutant that produces the same result and differs only
at a single boundary input, by at most one extra constant-time step, MAY be recorded as `equivalent`
when its justification states exactly that. Every other cost-only mutant MUST be detected by an
allocation, counted-work or latency test, in the lane where that test can run, or else dispositioned
as `killed-out-of-lane`; a cost that no test bounds MUST receive such a test or its code MUST be
removed. A justification MUST NOT argue from test difficulty or test selection. A mutant that a
deterministic test detects, including a test the lane did not select, is not equivalent: it is a
selection miss, resolved through CONV-204 or a focused rerun, never by an entry.

**`killed-out-of-lane`.** This kind is reserved for a mutant whose only observable effect is checked
by a test that cannot run under PIT instrumentation, such as a clean-JVM allocation measurement or a
latency gate. `test` MUST name that test, and `justification` MUST state which bounded effect the
mutant changes and why the test cannot run in the lane. `evidence` MUST be a table with exactly
these fields: `patch`, a unified diff that injects exactly this mutant into existing source files in
place (each old and new header MUST name the same repository path; creation, deletion, rename, copy,
binary and file-mode operations MUST be rejected, including header-only Git sections); `command`, the
argument list that runs the test in its own clean process; `baseline_exit`, the exit status of that
command on the unmodified sources, which MUST be 0; `injected_exit`, its exit status with the patch
applied, which MUST be non-zero; `failure`, the failing assertion's output excerpt; and `sources`,
the repository path and SHA-256 of every file the patch touches, taken before injection and verified
identical after the patch was reversed. The reporter MUST validate the form of this record; the
evidence is produced by the local counterfactual helper (CONV-203), and review judges whether
`patch` is the exact mutant.

Both kinds' justifications are checked by a floor: the reporter MUST reject a justification shorter
than 60 characters after whitespace normalization or containing, case-insensitively, `hard to test`,
`difficult to test`, `untestable`, `not testable` or the words `TODO`, `TBD` or `FIXME`; review
(CONV-203) judges the argument. A mutant that shows the mutated code has no observable effect at all
MUST be resolved by removing that code (CONV-103 resolution (4)), not by an entry.

### CONV-202 — PR mutation verdict with dispositions

- **Level:** MUST
- **Status:** partial
- **Since:** 1.0
- **Code:** [pr_mutation.py](../.github/scripts/pr_mutation.py) `judge_entry`, `judge_dispositions`, `apply_dispositions`, `is_clean`, `read_class`; [build.yml](../.github/workflows/build.yml)
- **Verified by:** [test_pr_mutation_dispositions.py](../.github/scripts/tests/test_pr_mutation_dispositions.py) `VerdictTest` (end-to-end `run` over fixture reports, class images and files: both kinds reported apart, `NO_COVERAGE` and other statuses never dispositioned, line-shifted and near-miss identities, unconfirmed, contradicted, superseded, stale, not-judged and inactive entries, re-confirmation by changed method only, invalid files, incomplete runs) and `MergeBaseFileTest`; `ClassReaderTest`.
- **Evidence class:** code+test (mapped tests not yet passed in a default CI lane for this baseline)
- **Gap/note:** Extends the [blocking criterion](../docs/VERIFICATION.md#blocking-criterion-the-prs-own-lines), whose changed-line attribution, incomplete-analysis rules and use of PIT's `detected` attribute (`TIMED_OUT`, `MEMORY_ERROR` and `RUN_ERROR` count as detected) it does not change. Under per-class sampling an absent mutant cannot be told from an unsampled one, so mutant-level staleness is judged only where the sample is provably complete. A reported count below the limit is no such proof: PIT runs its class limit (an evenly strided sample, one entry per binary class) before later equivalence filters, which can remove sampled mutants. In the sampled lane, re-confirmation by changed method (rule 2) catches most identity-changing edits; the local check judges complete samples from a focused run without a limit.

Tests MUST NOT be written to cover lines or to kill mutants for their own sake. A surviving or
uncovered mutant is a signal about the specification and its behaviour tests, resolved as CONV-103
lists. In particular a changed line that no selected test executes (`NO_COVERAGE`) MUST be resolved
by exactly one of: (1) the specified behaviour that needs this code receives a behaviour test derived
from its requirement, not aimed at the line, which then covers it; (2) the code has no specified
behaviour and is removed; (3) existing behaviour tests do cover it but the lane did not select them,
and a CONV-204 association makes the lane run them. A `NO_COVERAGE` mutant MUST NOT be dispositioned
by either kind, because a mutant that never executed has shown nothing.

Definitions for one lane run. A *changed-line mutant* is a reported mutant attributed to a line the
PR changed, as defined by the blocking criterion. A method is *changed* when a line of its compiled
line-number table is a changed line of its source. An entry is *added* when the dispositions file at
the merge base of the PR's base and checked-out revisions contains no entry equal to it in every
field, and *pre-existing* otherwise; an added entry is a *re-confirmation* when the merge-base file
holds an entry with the same identity, and *new* otherwise. If the merge-base file is absent or
invalid, every entry MUST be treated as new. A *mutated class* is a class in the `targetClasses` of a
module whose run completed. A mutated class's sample is *complete* only when the run applied no
per-class mutant limit.

The run MUST be clean only if the blocking criterion's completeness conditions hold, the dispositions
file is valid (CONV-201), and every one of the following holds:

1. Every changed-line mutant is detected, or has status `SURVIVED` and is matched by an added entry.
   A changed-line `NO_COVERAGE` mutant MUST fail even when matched, and so MUST every other
   undetected status.
2. No pre-existing entry belongs to a changed method. Such an entry MUST fail as *needs
   re-confirmation*: the author re-reviews the argument (and, for `killed-out-of-lane`, re-runs the
   counterfactual) and updates the entry, at least its `checked_at`. Entries of methods the PR did
   not change MUST NOT need re-confirmation, whatever else changed in their file.
3. Every new entry matches a `SURVIVED` mutant reported in this run. A new entry that matches
   nothing, and any added entry that matches an undetected mutant whose status is not `SURVIVED`,
   MUST fail as *unconfirmed*; matches to detected mutants follow rule 4. Since only changed sources
   are mutated, entries cannot be added for code outside the PR's changed files; a new entry may
   match a `SURVIVED` mutant on an unchanged line of a changed file, which lets an author
   re-identify an entry after a refactor (CONV-203), and the summary MUST mark such matches as on an
   unchanged line.
4. No `equivalent` entry matches a `KILLED` mutant: it MUST fail as *contradicted*, because a
   detecting test refutes the claim. No `killed-out-of-lane` entry matches a detected mutant: it MUST
   fail as *superseded*, because an in-lane test now detects it. An `equivalent` entry matching
   `TIMED_OUT`, `MEMORY_ERROR` or `RUN_ERROR` MUST be reported, not failed, because those statuses
   can arise from timing or infrastructure.
5. No entry is *stale*. An entry MUST be stale when its class is absent from every module's compiled
   main classes, its method name and descriptor are absent from that class, or, for
   `killed-out-of-lane`, its test class is no longer declared in any module's test sources, all
   checked for every entry in every run; or when its class is a mutated class with a complete sample
   and no reported mutant matches it.

A re-confirmation that matches no reported mutant in a class whose sample is incomplete or that this
run did not mutate MUST be reported as *not judged*, without failing, and so MUST a pre-existing
entry in a class whose sample is incomplete. A pre-existing entry of an unchanged method that matches
a mutant with a non-dispositionable undetected status MUST be reported as *inactive*, without
failing. Pre-existing entries of unchanged methods that match `SURVIVED` mutants on unchanged lines
MUST be reported as advisory dispositions and MUST NOT change the outcome.

The job summary and `results.json` MUST keep the two kinds apart and list, per module: the
changed-line mutants killed, otherwise detected, dispositioned as equivalent, dispositioned as
killed out of lane (with the named test) and failing; each dispositioned mutant with its matching
entry's `checked_at` and justification (the summary MAY truncate the justification; `results.json`
MUST NOT); each failing mutant with its reason (no entry, not dispositionable status); and every
entry in a failing or reported state (needs re-confirmation, unconfirmed, contradicted, superseded,
stale, not judged, detected-other) with its identity, its position in the file and the action that
resolves it, naming the local helper command for stale and re-confirmation states. Validation errors
MUST name the entry's position in the file and the violated rule. All file-derived text MUST be
escaped as existing summary text is.

### CONV-203 — Disposition review, abuse limits and refactoring

- **Level:** MUST
- **Status:** partial
- **Since:** 1.0
- **Code:** [pr_mutation.py](../.github/scripts/pr_mutation.py) `format_dispositions`, `build_parser`, `check_dispositions`, `remap_dispositions`, `counterfactual`; [../docs/VERIFICATION.md](../docs/VERIFICATION.md#reviewed-dispositions)
- **Verified by:** [test_pr_mutation_dispositions.py](../.github/scripts/tests/test_pr_mutation_dispositions.py) `ReviewTest` (summary counts and full added claims, escaping, CI commands without disposition input, removal monotone over every subset), `RefactorHelperTest`, `CounterfactualTest`. Review acceptance for the reviewer obligation.
- **Evidence class:** code+test (mapped tests not yet passed in a default CI lane for this baseline)
- **Gap/note:** Precedents compared: PIT's `excludedMethods`, `excludedClasses` and `avoidCallsTo`, and its `@Generated`/`@DoNotMutate` filter, remove whole methods or classes before mutation, invisibly in results; Stryker's `// Stryker disable next-line <mutators>: <reason>` comments keep an optional reason in the diff but cover a source line and a mutator list, not one mutant; mutmut's and cosmic-ray's `# pragma: no mutate` cover whole lines, and cosmic-ray's operator filter covers operators by regular expression. This requirement keeps their visibility in review and rejects their breadth: exact per-mutant identity, a mandatory reason, re-confirmation when the method changes, and no numeric cap or label, for the reasons stated in the requirement.

Dispositions are expected to be rare: behaviour and cost tests detect most mutants, and an entry
records a reviewed exception, not a way to finish a PR. A disposition MUST apply to one mutant
identity; class-, method-, mutator-, line- or file-wide waivers MUST NOT exist in the file, the
reporter or the workflow. Dispositions MUST come only from the dispositions file of the checked-out
revision and of the merge base; the reporter MUST NOT accept dispositions or a relaxation of
CONV-202 from command-line flags, environment variables, PR labels, comments or artifacts. The job
summary MUST open its disposition section with the number of added and removed entries and MUST list
every added entry with its kind, identity and full justification, so a reviewer sees each claim
beside the diff. A reviewer approving a PR that adds or changes entries MUST check each justification
against CONV-201's standard. No per-PR numeric cap and no approval label applies: a cap would push
authors toward line-targeted tests that CONV-202 forbids, and a label would need a re-run on every
label event. Removing an entry MUST always be permitted; it can only withdraw dispositions and MUST
NOT turn any undetected changed-line mutant from failing into accepted.

A pure refactor MUST stay cheap. The reporter MUST offer local helpers that CI never runs: a
read-only *check* that, given a PIT report and the compiled classes, lists stale, re-confirmation,
unconfirmed and superseded entries with the action for each; a read-only *remap* that, for each stale
entry, proposes candidate identities from the report with the same mutator and description,
preferring the same method with other instruction indexes, then a renamed method of the same class,
then a method of the same name and descriptor in another class, and prints every eligible candidate
once in tier order, labelling each with its tier and whether its instruction indexes match, for the
author to review without writing the file; and a *counterfactual* runner that hashes the touched
sources, checks that the test passes on them, applies the injection patch, runs the test in its own
process, reverses the patch, verifies the hashes and prints the `evidence` table, refusing to print
one when the baseline fails, the injected run passes or a hash differs. No helper output MUST be
applied to the dispositions file automatically.

### CONV-204 — Lane test associations

- **Level:** MUST
- **Status:** partial
- **Since:** 1.0
- **Code:** [pr_mutation.py](../.github/scripts/pr_mutation.py) `parse_associations`, `load_associations`, `test_scope`; [mutation-test-associations.toml](../.github/mutation-test-associations.toml)
- **Verified by:** [test_pr_mutation_dispositions.py](../.github/scripts/tests/test_pr_mutation_dispositions.py) `AssociationTest`.
- **Evidence class:** code+test (mapped tests not yet passed in a default CI lane for this baseline)
- **Gap/note:** Selecting tests by measured coverage of the changed classes would remove the need for associations but needs a per-test coverage map inside the lane's budget; that is recorded follow-up work. A test the `pr-mutation` profile excludes from PIT's baseline does not run even when associated; its effects are resolved through CONV-201's `killed-out-of-lane` kind.

The repository file `.github/mutation-test-associations.toml` MUST state which existing behaviour
tests the lane must run for a changed class, and nothing more. Its only top-level key MUST be
`association`, an array of tables with exactly the fields `class`, the binary name of a top-level
main class declared by a source the lane can select, and `tests`, a non-empty list of binary names of
top-level test classes declared in the same module. Names MUST be exact; a pattern character, an
unknown or missing field, a duplicate `class`, a class with no declaring main source, or a test not
declared in that module MUST make the file invalid, and an invalid file MUST make the run not clean
with a message naming the entry and the rule. Ownership MUST follow the declared package and the
top-level Java types, independently of source filenames and directories. Class literals and other
member references MUST NOT count as declarations or hide subsequent declarations. When a source is
selected, the tests associated with every top-level binary class it declares MUST be added to its
candidate tests and mark it resolved even if no name-based rule matched; they MUST count toward the
module test cap as other candidates do.

## Status and terminology (CONV-101, CONV-102)

| Status | Meaning |
|---|---|
| enforced | implemented, mapped deterministic default-lane test passed for the assessed baseline |
| partial | some behaviour or evidence exists; coverage, lane or execution evidence is incomplete |
| gap | implemented behaviour with no adequate mapped behavioural test |
| designed | decided behaviour with an implementation mismatch or in-flight implementation |
| proposed | specified follow-up capability not implemented here |
| open | intent or contract detail awaits a decision; alternatives are recorded explicitly |

Evidence classes are `code+test`, `code-only` and `doc-only (unverified)`. Source inspection
establishes what code and assertions say; it does not establish a test pass. `Draft` means awaiting
chapter adoption; `Adopted` means binding. Paths are repository-relative, and references name symbols
rather than drifting line numbers.

## Traceability

| Requirement | Existing or planned verification | Status |
|---|---|---|
| CONV-101 | No existing test mapped. **Planned:** chapter contract test over IDs, metadata and citations. | proposed |
| CONV-102 | No existing test mapped. **Planned:** evidence test rejecting an `enforced` claim without a passing default-lane result. | proposed |
| CONV-103 | No existing test mapped. **Planned:** review acceptance — specification-to-test-to-change mapping plus the PIT artifact with its dispositions. | proposed |
| CONV-104 | No existing test mapped. **Planned:** citation report over the test sources. | proposed |
| CONV-105 | No existing test mapped. **Planned:** release review of the documented extension set and public API. | proposed |
| CONV-201 | [`DispositionRecordTest`](../.github/scripts/tests/test_pr_mutation_dispositions.py) | partial |
| CONV-202 | [`VerdictTest`, `MergeBaseFileTest`, `ClassReaderTest`](../.github/scripts/tests/test_pr_mutation_dispositions.py) | partial |
| CONV-203 | [`ReviewTest`, `RefactorHelperTest`, `CounterfactualTest`](../.github/scripts/tests/test_pr_mutation_dispositions.py) | partial |
| CONV-204 | [`AssociationTest`](../.github/scripts/tests/test_pr_mutation_dispositions.py) | partial |
