#!/usr/bin/env bash
#
# benchmark-3way.sh - Compare native bjq vs JAR bjq vs jq on equivalent queries.
#
# Usage:
#   ./benchmark-3way.sh [--sizes "10000 100000 1000000"] [--rounds 3]
#
# Prerequisites:
#   - jq installed
#   - Native binary at target/bjq
#   - Jar at target/bjq-jar-with-dependencies.jar

set -euo pipefail

SIZES="${SIZES:-10000 100000 1000000}"
ROUNDS="${ROUNDS:-3}"
TMPDIR_BASE="${TMPDIR:-/tmp}/bjq-bench-$$"
JQ_JOIN_LIMIT=10000

NATIVE_CMD="target/bjq"
JAR_CMD="java --enable-preview --add-modules=jdk.incubator.vector -jar target/bjq-jar-with-dependencies.jar"

if [ ! -x "$NATIVE_CMD" ]; then
  echo "Error: native binary not found at $NATIVE_CMD. Run 'mvn -Pnative-pgo package -DskipTests'." >&2
  exit 1
fi
if [ ! -f "target/bjq-jar-with-dependencies.jar" ]; then
  echo "Error: jar not found. Run 'mvn package -DskipTests'." >&2
  exit 1
fi
if ! command -v jq &>/dev/null; then
  echo "Error: jq not found." >&2
  exit 1
fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    --sizes)  SIZES="$2"; shift 2 ;;
    --rounds) ROUNDS="$2"; shift 2 ;;
    *)        echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

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
  local size_mb=$(du -m "$file" | cut -f1)
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
  local size_mb=$(du -m "$file" | cut -f1)
  echo "  Generated: ${size_mb} MB"
}

# Time a command — return median of $ROUNDS runs in ms.
bench() {
  local -n cmdref=$1
  local query=$2
  local file=$3
  local times=()
  for ((r=1; r<=ROUNDS; r++)); do
    local start end elapsed
    start=$(date +%s%N)
    eval "${cmdref[*]}" "'$query'" "'$file'" > /dev/null 2>&1 || true
    end=$(date +%s%N)
    elapsed=$(( (end - start) / 1000000 ))
    times+=("$elapsed")
  done
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
  local size=$1 query=$2 nat=$3 jar=$4 jq_t=$5
  printf "  %-12s %-30s %10s  %10s  %10s  " "$size" "$query" "$(format_time "$nat")" "$(format_time "$jar")" "$(format_time "$jq_t")"
  # Speedup: native vs jq
  if [ "$jq_t" != "--" ] && [ "$jq_t" != "SKIP" ] && [ "$nat" != "--" ]; then
    python3 -c "
nat, jq = $nat, $jq_t
r = jq / nat
print(f'native {r:.1f}x vs jq' if r >= 1 else f'jq {1/r:.1f}x vs native')"
  else
    echo "--"
  fi
}

# Two-part bench: query from positional string, file from second positional.
# Each bench call passes the query and file to one of native/jar/jq.
bench_native() { bench_cmd NATIVE_ARR "$1" "$2"; }
bench_jar()    { bench_cmd JAR_ARR    "$1" "$2"; }
bench_jq()     { bench_cmd JQ_ARR     "$1" "$2"; }

bench_cmd() {
  local engine=$1 query=$2 file=$3
  local times=()
  for ((r=1; r<=ROUNDS; r++)); do
    local start end elapsed
    start=$(date +%s%N)
    case "$engine" in
      NATIVE_ARR) "$NATIVE_CMD" "$query" "$file" > /dev/null 2>&1 || true ;;
      JAR_ARR)    java --enable-preview --add-modules=jdk.incubator.vector -jar target/bjq-jar-with-dependencies.jar "$query" "$file" > /dev/null 2>&1 || true ;;
      JQ_ARR)     jq "$query" "$file" > /dev/null 2>&1 || true ;;
    esac
    end=$(date +%s%N)
    elapsed=$(( (end - start) / 1000000 ))
    times+=("$elapsed")
  done
  IFS=$'\n' sorted=($(sort -n <<<"${times[*]}")); unset IFS
  local mid=$(( ROUNDS / 2 ))
  echo "${sorted[$mid]}"
}

echo ""
echo "=================================================================="
echo "  native bjq vs jar bjq vs jq — 3-way comparison"
echo "=================================================================="
echo ""
echo "  Rounds per query: $ROUNDS (median reported)"
echo "  Dataset sizes:    $SIZES"
echo "  jq version:       $(jq --version)"
echo ""

# Warm up JVM
echo "  Warming up JVM..."
java --enable-preview --add-modules=jdk.incubator.vector -jar target/bjq-jar-with-dependencies.jar -n '1+1' > /dev/null 2>&1 || true
echo ""

for N in $SIZES; do
  echo "=================================================================="
  printf "  Dataset: %'d records\n" "$N"
  echo "=================================================================="

  FLAT="$TMPDIR_BASE/flat_${N}.json"
  JOIN="$TMPDIR_BASE/join_${N}.json"
  generate_flat_array "$N" "$FLAT"
  generate_join_data "$N" "$JOIN"
  echo ""
  printf "  %-12s %-30s %10s  %10s  %10s  %s\n" "Size" "Query" "native" "jar" "jq" "native vs jq"
  echo "  ---------------------------------------------------------------------------------------------------------"

  # Q1 filter
  Q="for \$u in \$\$[] where \$u.age > 40 and \$u.active return \$u.name"
  QJ="[.[] | select(.age > 40 and .active) | .name]"
  nat=$(bench_cmd NATIVE_ARR "$Q" "$FLAT")
  jar=$(bench_cmd JAR_ARR "$Q" "$FLAT")
  jq_t=$(bench_cmd JQ_ARR "$QJ" "$FLAT")
  print_row "$N" "filter (scan + predicate)" "$nat" "$jar" "$jq_t"

  # Q2 group-by
  Q="for \$u in \$\$[] let \$d := \$u.dept group by \$d return {\"dept\": \$d, \"count\": count(\$u), \"avg_salary\": avg(\$u.salary), \"avg_score\": avg(\$u.score)}"
  QJ="group_by(.dept) | map({dept: .[0].dept, count: length, avg_salary: (map(.salary) | add / length), avg_score: (map(.score) | add / length)})"
  nat=$(bench_cmd NATIVE_ARR "$Q" "$FLAT")
  jar=$(bench_cmd JAR_ARR "$Q" "$FLAT")
  jq_t=$(bench_cmd JQ_ARR "$QJ" "$FLAT")
  print_row "$N" "group by + 3 aggregates" "$nat" "$jar" "$jq_t"

  # Q3 group-by 2 keys
  Q="for \$u in \$\$[] where \$u.active let \$d := \$u.dept, \$c := \$u.city group by \$d, \$c let \$total := sum(\$u.salary) order by \$total descending return {\"dept\": \$d, \"city\": \$c, \"headcount\": count(\$u), \"total_salary\": \$total}"
  QJ="[.[] | select(.active)] | group_by(.dept, .city) | map({dept: .[0].dept, city: .[0].city, headcount: length, total_salary: (map(.salary) | add)}) | sort_by(-.total_salary)"
  nat=$(bench_cmd NATIVE_ARR "$Q" "$FLAT")
  jar=$(bench_cmd JAR_ARR "$Q" "$FLAT")
  jq_t=$(bench_cmd JQ_ARR "$QJ" "$FLAT")
  print_row "$N" "group by 2 keys + sort" "$nat" "$jar" "$jq_t"

  # Q4 hash-join
  Q="for \$o in \$\$.orders[], \$c in \$\$.customers[] where \$o.customer_id eq \$c.id return {\"order\": \$o.id, \"customer\": \$c.name, \"amount\": \$o.amount}"
  QJ="[.orders[] as \$o | .customers[] | select(.id == \$o.customer_id) | {order: \$o.id, customer: .name, amount: \$o.amount}]"
  nat=$(bench_cmd NATIVE_ARR "$Q" "$JOIN")
  jar=$(bench_cmd JAR_ARR "$Q" "$JOIN")
  if [ "$N" -le "$JQ_JOIN_LIMIT" ]; then
    jq_t=$(bench_cmd JQ_ARR "$QJ" "$JOIN")
  else
    jq_t="SKIP"
  fi
  print_row "$N" "hash-join" "$nat" "$jar" "$jq_t"

  # Q5 join+group+agg+sort
  Q="for \$o in \$\$.orders[], \$c in \$\$.customers[] where \$o.customer_id eq \$c.id let \$tier := \$c.tier, \$cat := \$o.category group by \$tier, \$cat let \$revenue := sum(\$o.amount) let \$qty := sum(\$o.quantity) order by \$revenue descending return {\"tier\": \$tier, \"category\": \$cat, \"revenue\": \$revenue, \"units\": \$qty, \"orders\": count(\$o)}"
  QJ="[.orders[] as \$o | .customers[] | select(.id == \$o.customer_id) | {tier: .tier, category: \$o.category, amount: \$o.amount, quantity: \$o.quantity}] | group_by(.tier, .category) | map({tier: .[0].tier, category: .[0].category, revenue: (map(.amount)|add), units: (map(.quantity)|add), orders: length}) | sort_by(-.revenue)"
  nat=$(bench_cmd NATIVE_ARR "$Q" "$JOIN")
  jar=$(bench_cmd JAR_ARR "$Q" "$JOIN")
  if [ "$N" -le "$JQ_JOIN_LIMIT" ]; then
    jq_t=$(bench_cmd JQ_ARR "$QJ" "$JOIN")
  else
    jq_t="SKIP"
  fi
  print_row "$N" "join+group+agg+sort" "$nat" "$jar" "$jq_t"

  # Q6 5-way aggregation
  Q="let \$data := \$\$[] return {\"total_salary\": sum(\$data.salary), \"avg_age\": avg(\$data.age), \"min_score\": min(\$data.score), \"max_score\": max(\$data.score), \"count\": count(\$data)}"
  QJ="{total_salary: (map(.salary)|add), avg_age: (map(.age)|add/length), min_score: (map(.score)|min), max_score: (map(.score)|max), count: length}"
  nat=$(bench_cmd NATIVE_ARR "$Q" "$FLAT")
  jar=$(bench_cmd JAR_ARR "$Q" "$FLAT")
  jq_t=$(bench_cmd JQ_ARR "$QJ" "$FLAT")
  print_row "$N" "5-way aggregation" "$nat" "$jar" "$jq_t"

  # Q7 string equality
  Q="for \$u in \$\$[] where \$u.city eq \"Tokyo\" return \$u.name"
  QJ="[.[] | select(.city == \"Tokyo\") | .name]"
  nat=$(bench_cmd NATIVE_ARR "$Q" "$FLAT")
  jar=$(bench_cmd JAR_ARR "$Q" "$FLAT")
  jq_t=$(bench_cmd JQ_ARR "$QJ" "$FLAT")
  print_row "$N" "string equality filter" "$nat" "$jar" "$jq_t"

  # Q8 top-N
  Q="(for \$u in \$\$[] order by \$u.salary descending return {\"name\": \$u.name, \"salary\": \$u.salary})[0:10]"
  QJ="sort_by(-.salary) | .[:10] | map({name, salary})"
  nat=$(bench_cmd NATIVE_ARR "$Q" "$FLAT")
  jar=$(bench_cmd JAR_ARR "$Q" "$FLAT")
  jq_t=$(bench_cmd JQ_ARR "$QJ" "$FLAT")
  print_row "$N" "top-N (order + slice)" "$nat" "$jar" "$jq_t"

  # Q9 compound AND
  Q="for \$u in \$\$[] where \$u.age > 30 and \$u.age < 50 and \$u.active return {\"name\": \$u.name, \"dept\": \$u.dept}"
  QJ="[.[] | select(.age > 30 and .age < 50 and .active) | {name, dept}]"
  nat=$(bench_cmd NATIVE_ARR "$Q" "$FLAT")
  jar=$(bench_cmd JAR_ARR "$Q" "$FLAT")
  jq_t=$(bench_cmd JQ_ARR "$QJ" "$FLAT")
  print_row "$N" "compound AND filter" "$nat" "$jar" "$jq_t"

  # Q10 count distinct
  Q="count(for \$u in \$\$[] let \$d := \$u.dept group by \$d return \$d)"
  QJ="[.[].dept] | unique | length"
  nat=$(bench_cmd NATIVE_ARR "$Q" "$FLAT")
  jar=$(bench_cmd JAR_ARR "$Q" "$FLAT")
  jq_t=$(bench_cmd JQ_ARR "$QJ" "$FLAT")
  print_row "$N" "count distinct" "$nat" "$jar" "$jq_t"

  # Q11 multi-key group + top-N
  Q="(for \$o in \$\$.orders[] let \$c := \$o.category, \$r := \$o.region group by \$c, \$r let \$total := sum(\$o.amount) order by \$total descending return {\"category\": \$c, \"region\": \$r, \"total\": \$total})[0:10]"
  QJ=".orders | group_by(.category, .region) | map({category: .[0].category, region: .[0].region, total: (map(.amount)|add)}) | sort_by(-.total) | .[:10]"
  nat=$(bench_cmd NATIVE_ARR "$Q" "$JOIN")
  jar=$(bench_cmd JAR_ARR "$Q" "$JOIN")
  jq_t=$(bench_cmd JQ_ARR "$QJ" "$JOIN")
  print_row "$N" "multi-key group + top-N" "$nat" "$jar" "$jq_t"

  echo ""
  rm -f "$FLAT" "$JOIN"
done

echo "=================================================================="
echo "  Legend: native = PGO'd native-image binary (bjq),"
echo "          jar    = JVM-mode (includes ~300ms JVM startup),"
echo "          jq     = jq-1.7 reference"
echo "=================================================================="
