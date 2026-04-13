#!/usr/bin/env bash
#
# benchmark-1b.sh — Compare native bjq, JAR bjq, and jq across 1M/100M/1B
# records on all vectorized query shapes.
#
# Prerequisites:
#   - examples/Gen1B.java compiled and run (produces /tmp/bench_1B.json, ~48GB)
#     This script also auto-generates 1M and 100M files with the same schema.
#   - Native bjq built with PGO: mvn -Pnative-pgo-instrument package -DskipTests
#     ...training run... then mvn -Pnative-pgo package -DskipTests
#   - Jar built: mvn package -DskipTests
#   - jq installed (1.7+)
#
# Usage:
#   ./benchmark-1b.sh                 # run all three sizes
#   SIZES="1M 100M" ./benchmark-1b.sh # subset
#
# Schema: {"age":N,"dept":"X","city":"X","active":T} (~48 bytes/record)
# At 1B records the file is ~48GB, matching the memory baseline for
# the 3 GB/s (raw NVMe speed) numbers.

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NATIVE="${NATIVE:-$REPO_DIR/target/bjq}"
JAR_PATH="${JAR_PATH:-$REPO_DIR/target/bjq-jar-with-dependencies.jar}"
JAR="java --enable-preview --add-modules=jdk.incubator.vector -jar $JAR_PATH"
SIZES="${SIZES:-1M 100M 1B}"
TIMEOUT_S="${TIMEOUT_S:-180}"

if [ ! -x "$NATIVE" ]; then echo "native binary not found at $NATIVE" >&2; exit 1; fi
if [ ! -f "$JAR_PATH" ]; then echo "jar not found at $JAR_PATH" >&2; exit 1; fi
if ! command -v jq >/dev/null; then echo "jq not found" >&2; exit 1; fi

# Ensure the datasets exist. 1B is expensive to regenerate; the others are cheap.
ensure_dataset() {
  local sz=$1 path=$2 n=$3
  if [ ! -f "$path" ]; then
    echo "Generating $sz records at $path ..."
    (cd /tmp && \
      cp "$REPO_DIR/examples/Gen1B.java" . && \
      javac Gen1B.java 2>/dev/null || true && \
      java -Xmx1g Gen1B "$n" "$path")
  fi
}

ensure_dataset 1M /tmp/bench_1M.json 1000000
ensure_dataset 100M /tmp/bench_100M.json 100000000
# 1B file is intentionally not auto-generated here (~48GB, ~1 min on NVMe).
# Run manually: java Gen1B 1000000000 /tmp/bench_1B.json

declare -A BJQ JQ LABEL

BJQ[filter]='count(for $u in $$[] where $u.age > 40 and $u.active return $u)'
JQ[filter]='[.[] | select(.age > 40 and .active)] | length'
LABEL[filter]='filter-count'

BJQ[groupby]='for $u in $$[] let $d := $u.dept group by $d return {"dept": $d, "count": count($u)}'
JQ[groupby]='group_by(.dept) | map({dept: .[0].dept, count: length})'
LABEL[groupby]='group-by dept'

BJQ[groupby2]='for $u in $$[] let $d := $u.dept, $c := $u.city group by $d, $c return {"dept": $d, "city": $c, "count": count($u)}'
JQ[groupby2]='group_by(.dept, .city) | map({dept: .[0].dept, city: .[0].city, count: length})'
LABEL[groupby2]='group-by 2 keys'

BJQ[fltgb]='for $u in $$[] where $u.active let $d := $u.dept group by $d return {"dept": $d, "count": count($u)}'
JQ[fltgb]='[.[] | select(.active)] | group_by(.dept) | map({dept: .[0].dept, count: length})'
LABEL[fltgb]='filter + group-by'

BJQ[cntdist]='count(for $u in $$[] let $d := $u.dept group by $d return $d)'
JQ[cntdist]='[.[].dept] | unique | length'
LABEL[cntdist]='count distinct'

BJQ[sum]='sum(for $u in $$[] return $u.age)'
JQ[sum]='[.[].age] | add'
LABEL[sum]='sum(age)'

BJQ[avg]='avg(for $u in $$[] return $u.age)'
JQ[avg]='[.[].age] | add / length'
LABEL[avg]='avg(age)'

BJQ[min]='min(for $u in $$[] return $u.age)'
JQ[min]='[.[].age] | min'
LABEL[min]='min(age)'

BJQ[max]='max(for $u in $$[] return $u.age)'
JQ[max]='[.[].age] | max'
LABEL[max]='max(age)'

# String-equality filter falls through to Volcano (executeFilterCount only
# accepts numeric thresholds today). Tracked as follow-up.
# BJQ[streq]='count(for $u in $$[] where $u.city eq "NYC" return $u.age)'
# JQ[streq]='[.[] | select(.city == "NYC")] | length'
# LABEL[streq]='string eq count'

QS="filter groupby groupby2 fltgb cntdist sum avg min max"

time_one() {
  local label=$1; shift
  local start end
  start=$(date +%s%N)
  timeout "$TIMEOUT_S" "$@" > /dev/null 2>/tmp/err-bjq-bench.log
  local rc=$?
  end=$(date +%s%N)
  local ms=$(( (end - start) / 1000000 ))
  if [ "$rc" -eq 124 ]; then echo "TIMEOUT"
  elif [ "$rc" -ne 0 ]; then
    if grep -qi "outofmemory\|out of memory\|killed" /tmp/err-bjq-bench.log 2>/dev/null; then echo "OOM"
    else echo "FAIL($rc)"
    fi
  else echo "${ms}ms"
  fi
}

format_cell() {
  local s=$1
  case "$s" in
    *TIMEOUT*|*OOM*|*FAIL*) echo "$s" ;;
    *) # ms or s
      local ms=${s%ms}
      if [ "$ms" -ge 60000 ]; then python3 -c "print(f'{$ms/60000:.1f}m')"
      elif [ "$ms" -ge 1000 ]; then python3 -c "print(f'{$ms/1000:.1f}s')"
      else echo "${ms}ms"
      fi ;;
  esac
}

for SIZE in $SIZES; do
  case $SIZE in
    1M) FILE=/tmp/bench_1M.json ;;
    100M) FILE=/tmp/bench_100M.json ;;
    1B) FILE=/tmp/bench_1B.json ;;
    *) echo "unknown size: $SIZE" >&2; exit 1 ;;
  esac
  if [ ! -f "$FILE" ]; then echo "missing dataset: $FILE (run Gen1B manually for 1B)" >&2; continue; fi

  echo "===================================="
  echo "  Dataset: $SIZE  ($(du -h "$FILE" | cut -f1))"
  echo "===================================="
  printf "  %-22s %10s %10s %10s\n" "Query" "native" "jar" "jq"
  echo "  -------------------------------------------------------"
  for Q in $QS; do
    t_nat=$(time_one "nat_${SIZE}_${Q}" "$NATIVE" "${BJQ[$Q]}" "$FILE")
    t_jar=$(time_one "jar_${SIZE}_${Q}" $JAR "${BJQ[$Q]}" "$FILE")
    t_jq=$(time_one  "jq_${SIZE}_${Q}"  jq -r "${JQ[$Q]}" "$FILE")
    printf "  %-22s %10s %10s %10s\n" "${LABEL[$Q]}" "$(format_cell "$t_nat")" "$(format_cell "$t_jar")" "$(format_cell "$t_jq")"
  done
  echo ""
done
