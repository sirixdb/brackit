# bjq vs jq: Side-by-Side Comparison

This document compares equivalent operations in `jq` and `bjq` (Brackit JSONiq).
For simple tasks they're comparable; for complex queries, bjq's FLWOR expressions
and automatic optimizations shine.

All examples use the sample data in `data/`.

## Setup

```bash
# bjq alias (adjust path as needed)
alias bjq='java -jar /path/to/bjq-jar-with-dependencies.jar'
```

---

## 1. Basic Field Access

**jq:**
```bash
jq '.users[0].name' data/users.json
```

**bjq:**
```bash
bjq '$$.users[0].name' data/users.json
```

Verdict: equivalent.

---

## 2. Iterate an Array

**jq:**
```bash
jq '.users[].name' data/users.json
```

**bjq:**
```bash
bjq '$$.users[].name' data/users.json
```

Verdict: equivalent.

---

## 3. Filter with a Condition

**jq:**
```bash
jq '.users[] | select(.age > 30)' data/users.json
```

**bjq:**
```bash
bjq 'for $u in $$.users[] where $u.age > 30 return $u' data/users.json
```

Verdict: similar. bjq reads like a SQL WHERE clause.

---

## 4. Group By with Aggregation

> This is where bjq starts to pull ahead.

**jq:**
```bash
jq '[.users[] | {department, name}] | group_by(.department)
    | map({department: .[0].department, count: length})' data/users.json
```

**bjq:**
```bash
bjq 'for $u in $$.users[]
     group by $dept := $u.department
     return {"department": $dept, "count": count($u)}' data/users.json
```

Verdict: bjq is significantly clearer. No need to manually `group_by` + `map`.

---

## 5. Group By with Multiple Aggregates

**jq:**
```bash
jq 'group_by(.category) | map({
      category: .[0].category,
      total_revenue: (map(.quantity * .unit_price) | add),
      total_units: (map(.quantity) | add),
      num_sales: length
    }) | sort_by(-.total_revenue)' data/sales.json
```

**bjq:**
```bash
bjq 'for $s in $$[]
     group by $cat := $s.category
     let $revenue := sum(for $x in $s return $x.quantity * $x.unit_price)
     order by $revenue descending
     return {
       "category": $cat,
       "total_revenue": $revenue,
       "total_units": sum($s.quantity),
       "num_sales": count($s)
     }' data/sales.json
```

Verdict: bjq handles multi-aggregate grouping naturally. The jq version requires
nested `map` calls and manual `sort_by`.

---

## 6. Join Two Collections

> jq has no native join. bjq optimizes this automatically with hash-joins.

**jq:**
```bash
jq '[.orders[] as $o | .customers[] | select(.id == $o.customer_id) |
     {order_id: $o.id, customer: .name, product: $o.product, amount: $o.amount}]' \
     data/orders.json
```

**bjq:**
```bash
bjq 'for $o in $$.orders[], $c in $$.customers[]
     where $o.customer_id eq $c.id
     return {
       "order_id": $o.id,
       "customer": $c.name,
       "product": $o.product,
       "amount": $o.amount
     }' data/orders.json
```

Verdict: bjq's join syntax is SQL-familiar and automatically uses hash-joins
under the hood. The jq version is an O(n*m) nested loop with no optimization.

---

## 7. Join + Group + Aggregate (the "report" query)

> This is the kind of query that makes jq painful and bjq natural.

**jq:**
```bash
jq '[.orders[] as $o | .customers[] | select(.id == $o.customer_id) |
     {customer: .name, tier: .tier, amount: $o.amount}]
   | group_by(.customer)
   | map({
       customer: .[0].customer,
       tier: .[0].tier,
       total_spent: (map(.amount) | add),
       order_count: length
     })
   | sort_by(-.total_spent)' data/orders.json
```

**bjq:**
```bash
bjq 'for $o in $$.orders[], $c in $$.customers[]
     where $o.customer_id eq $c.id
     group by $name := $c.name, $tier := $c.tier
     let $total := sum($o.amount)
     order by $total descending
     return {
       "customer": $name,
       "tier": $tier,
       "total_spent": $total,
       "order_count": count($o)
     }' data/orders.json
```

Verdict: the bjq version reads like SQL. The jq version requires building an
intermediate array, grouping, mapping over groups, and manual sorting.

---

## 8. Top-N with Slice

**jq:**
```bash
jq '[.users[] | select(.active)] | sort_by(-.age) | .[:3] | .[].name' data/users.json
```

**bjq:**
```bash
bjq '(for $u in $$.users[]
      where $u.active
      order by $u.age descending
      return $u.name)[0:3]' data/users.json
```

Verdict: similar length, but bjq's Python-style slicing is convenient.

---

## 9. Pivot / Cross-Tab

> Summarize sales by category and region.

**jq:**
```bash
jq 'group_by(.region) | map({
      region: .[0].region,
      widget_revenue: ([.[] | select(.category == "Widgets") | .quantity * .unit_price] | add // 0),
      gadget_revenue: ([.[] | select(.category == "Gadgets") | .quantity * .unit_price] | add // 0)
    })' data/sales.json
```

**bjq:**
```bash
bjq 'for $s in $$[]
     group by $region := $s.region
     return {
       "region": $region,
       "widget_revenue": sum(
         for $x in $s where $x.category eq "Widgets"
         return $x.quantity * $x.unit_price
       ),
       "gadget_revenue": sum(
         for $x in $s where $x.category eq "Gadgets"
         return $x.quantity * $x.unit_price
       )
     }' data/sales.json
```

Verdict: both are multi-line, but bjq's nested FLWOR inside aggregation functions
is more composable.

---

## 10. User-Defined Functions

> jq supports `def`; bjq supports `declare function` with full recursion.

**jq:**
```bash
jq 'def factorial: if . <= 1 then 1 else . * ((. - 1) | factorial) end;
    10 | factorial' <<< 'null'
```

**bjq:**
```bash
bjq -n 'declare function local:factorial($n) {
          if ($n le 1) then 1 else $n * local:factorial($n - 1)
        };
        local:factorial(10)'
```

Verdict: equivalent expressiveness.

---

## Summary

| Scenario | jq | bjq | Winner |
|---|---|---|---|
| Simple field access | `.foo` | `$$.foo` | Tie |
| Array iteration | `.[]` | `$$[]` | Tie |
| Filtering | `select(...)` | `where` clause | Tie |
| Group by | `group_by` + `map` | `group by` clause | **bjq** |
| Multi-aggregate | Nested `map` | Natural in FLWOR | **bjq** |
| Joins | Manual nested loop | Auto hash-join | **bjq** |
| Join + group + sort | Very complex | SQL-like | **bjq** |
| Top-N | `sort_by \| .[:N]` | `order by` + slice | Tie |
| User functions | `def` | `declare function` | Tie |

**Rule of thumb:** if your query has a GROUP BY, JOIN, or ORDER BY, bjq
will be simpler to write and faster to execute.
