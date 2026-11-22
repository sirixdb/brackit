# Lessons from failures

Keep entries short and evidence-based: the broken assumption, what to apply next time, and the
repository evidence. For each new bug, add a [write-up](postmortems/TEMPLATE.md), a regression test
that fails on `master` before the fix, and a lesson here. Extend an existing lesson when the same
failure mode recurs rather than adding a near-duplicate — a list of near-duplicates is read by
nobody, which defeats the point of keeping it.

The load-bearing column is the middle one. The first column explains one bug; the second is the
only part that prevents the next one, so it names a check or a rule concrete enough to apply, not
"be more careful".

This file starts empty: the practice begins with the change that introduced
[docs/DEVELOPMENT.md](DEVELOPMENT.md), and the first bug fixed under it writes the first row. An
empty table is the honest state; backfilling rows from memory for bugs whose evidence was never
recorded would only make the file look authoritative
([CONV-102](../spec/CONV-conventions.md#conv-102--conformance-and-evidence-honesty)).

| Failure / broken assumption | Apply next time | Repository evidence |
| --- | --- | --- |
