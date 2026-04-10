#!/usr/bin/env bash
#
# benchmark.sh - Compare bjq and jq performance on equivalent queries.
#
# Usage:
#   ./benchmark.sh [--sizes "10000 100000 500000 1000000"] [--rounds 3]
#
# Prerequisites:
#   - jq installed (https://jqlang.github.io/jq/)
#   - bjq jar built: mvn package -DskipTests
#     OR bjq native binary available on PATH
#   - python3 available (for data generation)
#
# The script generates JSON datasets at scale (up to millions of records)
# and runs equivalent queries in both tools, reporting wall-clock times.
# The default sizes are chosen to highlight where Brackit's query optimizer
# and hash-join engine outperform jq's brute-force approach.

set -euo pipefail

# ---------- configuration ----------

SIZES="${SIZES:-10000 100000 500000 1000000}"
ROUNDS="${ROUNDS:-3}"
TMPDIR_BASE="${TMPDIR:-/tmp}/bjq-bench-$$"
# Skip join benchmarks for jq above this size (O(n*m) takes too long)
JQ_JOIN_LIMIT=10000

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
  echo "  Generating flat array ($n records)..."
  python3 -c "
import json, random, sys
random.seed(42)
depts = ['Engineering','Sales','Marketing','Operations','HR','Finance','Legal','Support']
cities = ['New York','London','Tokyo','Berlin','Sydney','Toronto','Mumbai','Sao Paulo']
data = []
for i in range($n):
    data.append({
        'id': i,
        'name': f'user_{i}',
        'age': random.randint(18, 65),
        'score': round(random.uniform(0, 100), 2),
        'salary': round(random.uniform(30000, 200000), 2),
        'dept': random.choice(depts),
        'city': random.choice(cities),
        'active': random.choice([True, False]),
        'level': random.randint(1, 10),
        'tags': random.sample(['python','java','go','rust','js','sql','ml','devops'], random.randint(1,4))
    })
json.dump(data, sys.stdout)
" > "$file"
  local size_mb
  size_mb=$(du -m "$file" | cut -f1)
  echo "  Generated: ${size_mb} MB"
}

generate_join_data() {
  local n=$1 file=$2
  local order_count=$((n * 5))
  echo "  Generating join data ($n customers, $order_count orders)..."
  python3 -c "
import json, random, sys
random.seed(42)
tiers = ['platinum','gold','silver','bronze']
categories = ['Electronics','Furniture','Clothing','Food','Books','Sports','Tools','Garden']
regions = ['NA-East','NA-West','EU-West','EU-East','APAC','LATAM']
customers = []
for i in range($n):
    customers.append({
        'id': i,
        'name': f'customer_{i}',
        'tier': random.choice(tiers),
        'region': random.choice(regions),
        'since': f'{random.randint(2015,2025)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}'
    })
orders = []
for i in range($order_count):
    orders.append({
        'id': i,
        'customer_id': random.randint(0, $n - 1),
        'amount': round(random.uniform(5, 2000), 2),
        'category': random.choice(categories),
        'region': random.choice(regions),
        'date': f'2025-{random.randint(1,12):02d}-{random.randint(1,28):02d}',
        'quantity': random.randint(1, 50)
    })
json.dump({'customers': customers, 'orders': orders}, sys.stdout)
" > "$file"
  local size_mb
  size_mb=$(du -m "$file" | cut -f1)
  echo "  Generated: ${size_mb} MB"
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

format_time() {
  local ms=$1
  if [ "$ms" = "--" ] || [ "$ms" = "SKIP" ]; then
    echo "$ms"
  elif [ "$ms" -ge 60000 ]; then
    python3 -c "print(f'{$ms/60000:.1f}m')"
  elif [ "$ms" -ge 1000 ]; then
    python3 -c "print(f'{$ms/1000:.1f}s')"
  else
    echo "${ms}ms"
  fi
}

print_row() {
  local size=$1 query=$2 bjq_t=$3 jq_t=$4 ratio=$5
  local bjq_fmt jq_fmt
  bjq_fmt=$(format_time "$bjq_t")
  jq_fmt=$(format_time "$jq_t")
  printf "  %-12s %-30s %10s  %10s  %s\n" "$size" "$query" "$bjq_fmt" "$jq_fmt" "$ratio"
}

compute_ratio() {
  local bjq_t=$1 jq_t=$2
  if [ "$jq_t" = "--" ] || [ "$jq_t" = "SKIP" ] || [ "$bjq_t" = "--" ]; then
    echo "--"
  elif [ "$bjq_t" -gt 0 ]; then
    python3 -c "
bjq, jq = $bjq_t, $jq_t
ratio = jq / bjq
if ratio >= 1:
    print(f'bjq {ratio:.1f}x faster')
else:
    print(f'jq {1/ratio:.1f}x faster')
"
  else
    echo "--"
  fi
}

# ---------- benchmarks ----------

echo ""
echo "=================================================================="
echo "  bjq vs jq benchmark — large-scale JSON processing"
echo "=================================================================="
echo ""
echo "  Rounds per query: $ROUNDS (median reported)"
echo "  Dataset sizes:    $SIZES"
echo "  bjq command:      ${BJQ_CMD:0:60}..."
$JQ_AVAILABLE && echo "  jq version:       $(jq --version 2>&1)" || echo "  jq:               not available"
echo ""

# Warm up bjq JVM (if using jar)
if [[ "$BJQ_CMD" == java* ]]; then
  echo "  Warming up JVM (first run is always slow)..."
  echo '[1,2,3]' | eval "$BJQ_CMD" '-n "1+1"' > /dev/null 2>&1 || true
  echo ""
fi

for N in $SIZES; do
  echo "=================================================================="
  printf "  Dataset: %'d records\n" "$N"
  echo "=================================================================="

  FLAT="$TMPDIR_BASE/flat_${N}.json"
  JOIN="$TMPDIR_BASE/join_${N}.json"
  generate_flat_array "$N" "$FLAT"
  generate_join_data "$N" "$JOIN"
  echo ""

  printf "  %-12s %-30s %10s  %10s  %s\n" "Size" "Query" "bjq" "jq" "Winner"
  echo "  ----------------------------------------------------------------------------------------------------"

  # --- Query 1: Full scan with filter ---
  bjq_t=$(bench "bjq-filter-$N" "$BJQ_CMD" "'for \$u in \$\$[] where \$u.age > 40 and \$u.active return \$u.name'" "$FLAT")
  if $JQ_AVAILABLE; then
    jq_t=$(bench "jq-filter-$N" "jq" "'[.[] | select(.age > 40 and .active) | .name]'" "$FLAT")
  else
    jq_t="--"
  fi
  print_row "$N" "filter (scan + predicate)" "$bjq_t" "$jq_t" "$(compute_ratio "$bjq_t" "$jq_t")"

  # --- Query 2: Group by + multiple aggregates ---
  bjq_t=$(bench "bjq-group-$N" "$BJQ_CMD" "'for \$u in \$\$[] group by \$d := \$u.dept return {\"dept\": \$d, \"count\": count(\$u), \"avg_salary\": avg(\$u.salary), \"avg_score\": avg(\$u.score)}'" "$FLAT")
  if $JQ_AVAILABLE; then
    jq_t=$(bench "jq-group-$N" "jq" "'group_by(.dept) | map({dept: .[0].dept, count: length, avg_salary: (map(.salary) | add / length), avg_score: (map(.score) | add / length)})'" "$FLAT")
  else
    jq_t="--"
  fi
  print_row "$N" "group by + 3 aggregates" "$bjq_t" "$jq_t" "$(compute_ratio "$bjq_t" "$jq_t")"

  # --- Query 3: Group by two keys ---
  bjq_t=$(bench "bjq-group2-$N" "$BJQ_CMD" "'for \$u in \$\$[] where \$u.active group by \$d := \$u.dept, \$c := \$u.city let \$total := sum(\$u.salary) order by \$total descending return {\"dept\": \$d, \"city\": \$c, \"headcount\": count(\$u), \"total_salary\": \$total}'" "$FLAT")
  if $JQ_AVAILABLE; then
    jq_t=$(bench "jq-group2-$N" "jq" "'[.[] | select(.active)] | group_by(.dept, .city) | map({dept: .[0].dept, city: .[0].city, headcount: length, total_salary: (map(.salary) | add)}) | sort_by(-.total_salary)'" "$FLAT")
  else
    jq_t="--"
  fi
  print_row "$N" "group by 2 keys + sort" "$bjq_t" "$jq_t" "$(compute_ratio "$bjq_t" "$jq_t")"

  # --- Query 4: Hash-join ---
  if $JQ_AVAILABLE && [ "$N" -le "$JQ_JOIN_LIMIT" ]; then
    jq_t=$(bench "jq-join-$N" "jq" "'[.orders[] as \$o | .customers[] | select(.id == \$o.customer_id) | {order: \$o.id, customer: .name, amount: \$o.amount}]'" "$JOIN")
  elif $JQ_AVAILABLE; then
    jq_t="SKIP"
  else
    jq_t="--"
  fi
  bjq_t=$(bench "bjq-join-$N" "$BJQ_CMD" "'for \$o in \$\$.orders[], \$c in \$\$.customers[] where \$o.customer_id eq \$c.id return {\"order\": \$o.id, \"customer\": \$c.name, \"amount\": \$o.amount}'" "$JOIN")
  if [ "$jq_t" = "SKIP" ]; then
    print_row "$N" "hash-join" "$bjq_t" "$jq_t" "jq skipped (O(n*m) too slow)"
  else
    print_row "$N" "hash-join" "$bjq_t" "$jq_t" "$(compute_ratio "$bjq_t" "$jq_t")"
  fi

  # --- Query 5: Join + group + aggregate + sort (the "report" query) ---
  bjq_t=$(bench "bjq-report-$N" "$BJQ_CMD" "'for \$o in \$\$.orders[], \$c in \$\$.customers[] where \$o.customer_id eq \$c.id group by \$tier := \$c.tier, \$cat := \$o.category let \$revenue := sum(\$o.amount) let \$qty := sum(\$o.quantity) order by \$revenue descending return {\"tier\": \$tier, \"category\": \$cat, \"revenue\": \$revenue, \"units\": \$qty, \"orders\": count(\$o)}'" "$JOIN")
  if $JQ_AVAILABLE && [ "$N" -le "$JQ_JOIN_LIMIT" ]; then
    jq_t=$(bench "jq-report-$N" "jq" "'[.orders[] as \$o | .customers[] | select(.id == \$o.customer_id) | {tier: .tier, category: \$o.category, amount: \$o.amount, quantity: \$o.quantity}] | group_by(.tier, .category) | map({tier: .[0].tier, category: .[0].category, revenue: (map(.amount)|add), units: (map(.quantity)|add), orders: length}) | sort_by(-.revenue)'" "$JOIN")
  elif $JQ_AVAILABLE; then
    jq_t="SKIP"
  else
    jq_t="--"
  fi
  if [ "$jq_t" = "SKIP" ]; then
    print_row "$N" "join+group+agg+sort" "$bjq_t" "$jq_t" "jq skipped (O(n*m) too slow)"
  else
    print_row "$N" "join+group+agg+sort" "$bjq_t" "$jq_t" "$(compute_ratio "$bjq_t" "$jq_t")"
  fi

  # --- Query 6: Aggregation over large array ---
  bjq_t=$(bench "bjq-agg-$N" "$BJQ_CMD" "'let \$data := \$\$[] return {\"total_salary\": sum(\$data.salary), \"avg_age\": avg(\$data.age), \"min_score\": min(\$data.score), \"max_score\": max(\$data.score), \"count\": count(\$data)}'" "$FLAT")
  if $JQ_AVAILABLE; then
    jq_t=$(bench "jq-agg-$N" "jq" "'{total_salary: (map(.salary)|add), avg_age: (map(.age)|add/length), min_score: (map(.score)|min), max_score: (map(.score)|max), count: length}'" "$FLAT")
  else
    jq_t="--"
  fi
  print_row "$N" "5-way aggregation" "$bjq_t" "$jq_t" "$(compute_ratio "$bjq_t" "$jq_t")"

  echo ""

  # Clean up large files between sizes to avoid running out of disk
  rm -f "$FLAT" "$JOIN"
done

echo "=================================================================="
echo "  Notes"
echo "=================================================================="
echo ""
echo "  - bjq times include JVM startup when using the jar."
echo "    Use the native binary (mvn -Pnative package) for fair comparison."
echo "  - jq join queries are skipped above $JQ_JOIN_LIMIT records because"
echo "    jq uses O(n*m) nested loops — at 100k customers x 500k orders"
echo "    that's 50 billion iterations."
echo "  - bjq uses hash-joins (O(n+m)) so joins scale linearly."
echo "  - The 'report' query (join+group+agg+sort) is the real-world"
echo "    scenario where Brackit's optimizer makes the biggest impact."
echo ""
