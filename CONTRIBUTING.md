# Contributing

From opening a bug report to creating a pull request: every contribution is
appreciated and welcome. If you're planning to implement a new feature or change
the api please create an issue first. This way we can ensure that your precious
work is not in vain.

[How Brackit is developed](docs/DEVELOPMENT.md) is the project's development strategy and explains
why each practice below exists. Read it once before your first change.

## Submitting Changes

After getting some feedback, push to your fork and submit a pull request. We
may suggest some changes or improvements or alternatives, but for small changes
your pull request should be accepted quickly.

Some things to consider:

* **Derive the tests from the specification, before writing the implementation code or
  independently of it** — never from the code you just wrote, and never by recording what the
  engine currently prints (the
  [test derivation policy](docs/DEVELOPMENT.md#2-tests-are-derived-from-the-requirements-not-from-the-code))
* Assert behaviour only: the result sequence, the error code, the serialized output
* Follow the existing coding style
* Write a [good commit message](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html)

## Build and test

```bash
mvn package                           # build with tests
mvn clean test                        # run all tests
mvn test -Dtest=ClassName             # one test class
mvn test -Dtest=ClassName#methodName  # one test method
```

Formatting is enforced by `formatter-maven-plugin` and runs automatically during
`process-sources`, so there is no separate format step.

## Spec, mutation, exploration and learning

Brackit implements a language that is already specified: XQuery 3.1, XPath and XQuery Functions and
Operators 3.1, and JSONiq, plus Brackit's own
[documented extensions](README.MD#differences-from-standard-jsoniq).
[spec/README.md](spec/README.md) names those specifications of record and how to cite them.
[How Brackit is developed](docs/DEVELOPMENT.md) explains the practices below: the specification as
the source of truth, tests derived from the requirements rather than from the code, behaviour-only
tests, mutation testing, exploratory testing, review against the written intent, and the benchmark
protocol. What it asks of every pull request:

- Identify the specification section — or write the requirement in [`spec/`](spec/README.md) when
  none covers the behaviour — and cite it in the issue, then in the PR, following the
  [spec-first workflow](docs/DEVELOPMENT.md#1-the-specification-is-the-source-of-truth). Derive the
  tests from that statement **before writing implementation code, or independently of it** (the
  [test derivation policy](docs/DEVELOPMENT.md#2-tests-are-derived-from-the-requirements-not-from-the-code)),
  and say in the PR which test checks which behaviour. For a documentation or CI change, name the
  observable acceptance checks instead of inventing application tests.
- Mutation-test changed logic: inspect the PIT artifact and resolve every surviving or uncovered
  changed-line mutant under
  [CONV-103](spec/CONV-conventions.md#conv-103--specification-before-implementation) — a behaviour
  test, a reviewed disposition with its reason and evidence, or removing the code. Never write a
  test to kill a mutant. Scope, local commands, reviewed dispositions, test associations and the
  blocking switch are in [Verification](docs/VERIFICATION.md#pr-mutation-testing).
- Explore beyond the happy path: adversarial queries and inputs, property-based tests, or a
  differential comparison against an independent oracle such as an unoptimized plan or a second
  processor. Record the cases, seeds and outcomes in the PR, and promote any discovered failure to a
  deterministic regression.
- Give every bug a root-cause [write-up](docs/postmortems/TEMPLATE.md), a regression test that
  fails on `master` before the fix, and an actionable entry in [Lessons](docs/LESSONS.md). Link the
  evidence; explain why the old tests missed it and how the new check would catch it.
- State the intent in the PR description — what the change must achieve — and expect review against
  that intent rather than against the code
  ([why](docs/DEVELOPMENT.md#8-review-against-the-written-intent)).

## Setup your fork

Fork this repository! Once you have a copy of this repo on your own account, clone this repo to your computer by typing in something like:

1. `git clone https://github.com/sirixdb/brackit.git`

  (Replace the URL with your own repository URL path.)

2. Run `cd brackit`. Then, set up this repository as an upstream branch using:
  * `git remote add upstream https://github.com/sirixdb/brackit.git`

  Now, whenever you want to sync with the owner repository. Do the following:
  * `git fetch upstream`
  * `git checkout staging`
  * `git merge upstream/staging`

  Note: You can type `git remote -v` to check which repositories your `origin` and `upstream` are pointing to.
