# Lessons from failures

Keep entries short and evidence-based: the broken assumption, what to apply next time, and the
repository evidence. For each new bug, add a [write-up](postmortems/TEMPLATE.md), a regression test
that fails on `master` before the fix, and a lesson here. Extend an existing lesson when the same
failure mode recurs rather than adding a near-duplicate — a list of near-duplicates is read by
nobody, which defeats the point of keeping it.

The load-bearing column is the middle one. The first column explains one bug; the second is the
only part that prevents the next one, so it names a check or a rule concrete enough to apply, not
"be more careful".

| Failure / broken assumption | Apply next time | Repository evidence |
| --- | --- | --- |
| A conditional PIT setting was treated as proof that minions inherited the test lane's JVM flags, although Surefire configuration parsing was disabled; only tests needing neither flag had exercised the lane. | When a lane inherits JVM configuration, verify it with a test that actually requires the module or preview flag, rather than only tests that pass without it. | Explicit `jvmArgs` in the [`pr-mutation` profile](../pom.xml) and the corrected [runtime flag contract](VERIFICATION.md#pr-mutation-testing). |
| A verification inventory counted corpus resources and benchmark dependencies without active consumers, promising checks and measurements the repository did not provide. | Before listing a layer, follow it to the consumer and check that it runs, asserts the claimed result, or times the claimed work; unused resources and dependencies are not evidence. | Corrected [verification layer map](VERIFICATION.md#layer-map) and the disabled, assertion-free [`QueryTest.testExtension`](../src/test/java/io/brackit/query/QueryTest.java) it stopped counting. |
