#!/usr/bin/env bash
#
# benchmark.sh - Compare bjq and jq performance on equivalent queries.
#
# Usage:
#   ./benchmark.sh [--sizes "100 1000 10000"] [--rounds 5]
#
# Prerequisites:
#   - jq installed (https://jqlang.github.io/jq/)
#   - bjq jar built: mvn package -DskipTests
#     OR bjq native binary available on PATH
#
# The script generates JSON datasets of varying sizes and runs equivalent
# queries in both tools, reporting wall-clock times.

set -euo pipefail

# ---------- configuration ----------

SIZES="${SIZES:-100 1000 10000}"
ROUNDS="${ROUNDS:-5}"
TMPDIR_BASE="${TMPDIR:-/tmp}/bjq-bench-$$"

# Try to find bjq: native binary first, then jar
if command -v bjq &>/dev/null; then
  BJQ_CMD="bjq"
elif [ -f "target/bjq-jar-with-dependencies.jar" ]; then
  BJQ_CMD="java --enable-preview --add-modules=jdk.incubator.vector -jar target/bjq-jar-with-dependencies.jar"
else
  echo "Error: bjq not found. Build with 'mvn package -DskipTests' first." >&2
  exit 1
fi

if ! command -v jq &>/dev/null; then
  echo "Warning: jq not found, will only benchmark bjq." >&2
  JQ_AVAILABLE=false
else
  JQ_AVAILABLE=true
fi

# ---------- parse arguments ----------

while [[ $# -gt 0 ]]; do
  case "$1" in
    --sizes)  SIZES="$2"; shift 2 ;;
    --rounds) ROUNDS="$2"; shift 2 ;;
    *)        echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

# ---------- helpers ----------

mkdir -p "$TMPDIR_BASE"
trap 'rm -rf "$TMPDIR_BASE"' EXIT

generate_flat_array() {
  local n=$1 file=$2
  python3 -c "
import json, random
data = [{'id': i, 'name': f'user_{i}', 'age': random.randint(18,65),
         'score': round(random.uniform(0,100),2),
         'dept': random.choice(['Eng','Sales','Mkt','Ops','HR']),
         'active': random.choice([True, False])}
        for i in range($n)]
print(json.dumps(data))
" > "$file"
}

generate_join_data() {
  local n=$1 file=$2
  python3 -c "
import json, random
customers = [{'id': i, 'name': f'customer_{i}', 'tier': random.choice(['gold','silver','bronze'])}
             for i in range($n)]
orders = [{'id': i, 'customer_id': random.randint(0, $n - 1),
           'amount': round(random.uniform(10, 500), 2),
           'category': random.choice(['Electronics','Furniture','Clothing','Food'])}
          for i in range($n * 3)]
print(json.dumps({'customers': customers, 'orders': orders}))
" > "$file"
}

# Time a command, return median of $ROUNDS runs in milliseconds
bench() {
  local label="$1"; shift
  local times=()
  for ((r = 1; r <= ROUNDS; r++)); do
    local start end elapsed
    start=$(date +%s%N)
    eval "$@" > /dev/null 2>&1
    end=$(date +%s%N)
    elapsed=$(( (end - start) / 1000000 ))
    times+=("$elapsed")
  done
  # Sort and take median
  IFS=$'\n' sorted=($(sort -n <<<"${times[*]}")); unset IFS
  local mid=$(( ROUNDS / 2 ))
  echo "${sorted[$mid]}"
}

print_row() {
  printf "%-12s %-35s %8s ms  %8s ms  %s\n" "$1" "$2" "$3" "$4" "$5"
}

# ---------- benchmarks ----------

echo "=========================================="
echo "  bjq vs jq benchmark"
echo "=========================================="
echo ""
echo "Rounds per query: $ROUNDS (median reported)"
echo "bjq command: $BJQ_CMD"
echo ""

# Warm up bjq JVM (if using jar)
if [[ "$BJQ_CMD" == java* ]]; then
  echo "Warming up JVM..."
  echo '{}' | eval "$BJQ_CMD" '-n "1+1"' > /dev/null 2>&1 || true
  echo ""
fi

print_row "Size" "Query" "bjq" "jq" "Speedup"
echo "--------------------------------------------------------------------------------------------"

for N in $SIZES; do
  FLAT="$TMPDIR_BASE/flat_${N}.json"
  JOIN="$TMPDIR_BASE/join_${N}.json"
  generate_flat_array "$N" "$FLAT"
  generate_join_data "$N" "$JOIN"

  # --- Query 1: Simple field access ---
  bjq_t=$(bench "bjq-simple-$N" "$BJQ_CMD" "'\$\$[0].name'" "$FLAT")
  if $JQ_AVAILABLE; then
    jq_t=$(bench "jq-simple-$N" "jq" "'.[0].name'" "$FLAT")
    if [ "$bjq_t" -gt 0 ]; then
      ratio=$(python3 -c "print(f'{$jq_t/$bjq_t:.2f}x')")
    else
      ratio="--"
    fi
  else
    jq_t="--"; ratio="--"
  fi
  print_row "$N" "field access" "$bjq_t" "$jq_t" "$ratio"

  # --- Query 2: Filter ---
  bjq_t=$(bench "bjq-filter-$N" "$BJQ_CMD" "'for \$u in \$\$[] where \$u.age > 30 return \$u.name'" "$FLAT")
  if $JQ_AVAILABLE; then
    jq_t=$(bench "jq-filter-$N" "jq" "'[.[] | select(.age > 30) | .name]'" "$FLAT")
    if [ "$bjq_t" -gt 0 ]; then
      ratio=$(python3 -c "print(f'{$jq_t/$bjq_t:.2f}x')")
    else
      ratio="--"
    fi
  else
    jq_t="--"; ratio="--"
  fi
  print_row "$N" "filter (age > 30)" "$bjq_t" "$jq_t" "$ratio"

  # --- Query 3: Group by with aggregation ---
  bjq_t=$(bench "bjq-group-$N" "$BJQ_CMD" "'for \$u in \$\$[] group by \$d := \$u.dept return {\$d: count(\$u)}'" "$FLAT")
  if $JQ_AVAILABLE; then
    jq_t=$(bench "jq-group-$N" "jq" "'group_by(.dept) | map({dept: .[0].dept, count: length})'" "$FLAT")
    if [ "$bjq_t" -gt 0 ]; then
      ratio=$(python3 -c "print(f'{$jq_t/$bjq_t:.2f}x')")
    else
      ratio="--"
    fi
  else
    jq_t="--"; ratio="--"
  fi
  print_row "$N" "group by + count" "$bjq_t" "$jq_t" "$ratio"

  # --- Query 4: Aggregation ---
  bjq_t=$(bench "bjq-agg-$N" "$BJQ_CMD" "'sum(for \$u in \$\$[] return \$u.score)'" "$FLAT")
  if $JQ_AVAILABLE; then
    jq_t=$(bench "jq-agg-$N" "jq" "'[.[].score] | add'" "$FLAT")
    if [ "$bjq_t" -gt 0 ]; then
      ratio=$(python3 -c "print(f'{$jq_t/$bjq_t:.2f}x')")
    else
      ratio="--"
    fi
  else
    jq_t="--"; ratio="--"
  fi
  print_row "$N" "sum aggregation" "$bjq_t" "$jq_t" "$ratio"

  # --- Query 5: Join ---
  bjq_t=$(bench "bjq-join-$N" "$BJQ_CMD" "'for \$o in \$\$.orders[], \$c in \$\$.customers[] where \$o.customer_id eq \$c.id return {\$c.name: \$o.amount}'" "$JOIN")
  if $JQ_AVAILABLE; then
    jq_t=$(bench "jq-join-$N" "jq" "'[.orders[] as \$o | .customers[] | select(.id == \$o.customer_id) | {(.name): \$o.amount}]'" "$JOIN")
    if [ "$bjq_t" -gt 0 ]; then
      ratio=$(python3 -c "print(f'{$jq_t/$bjq_t:.2f}x')")
    else
      ratio="--"
    fi
  else
    jq_t="--"; ratio="--"
  fi
  print_row "$N" "join (orders x customers)" "$bjq_t" "$jq_t" "$ratio"

  echo ""
done

echo "=========================================="
echo "  Notes"
echo "=========================================="
echo ""
echo "- bjq times include JVM startup when using the jar. Use the"
echo "  native binary (mvn -Pnative package) for startup-sensitive workloads."
echo "- jq uses O(n*m) nested loops for joins; bjq uses hash-joins."
echo "- Speedup > 1.0 means bjq is faster; < 1.0 means jq is faster."
echo "- For small datasets, JVM startup dominates. The join query is"
echo "  where bjq's optimizer makes the biggest difference at scale."
