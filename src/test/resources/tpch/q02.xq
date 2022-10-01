declare ordering unordered;
declare variable $schema-file external;
let $schema := rel:parse-schema($schema-file, (), ()) for $p in $schema.part,
$s in $schema.supplier, $ps in $schema.partsupp, $n in $schema.nation,
$r in $schema.region
where $p.p_partkey eq $ps.ps_partkey and $s.s_suppkey eq $ps.ps_suppkey and $p.p_size eq 15
and fn:ends-with($p.p_type, ’BRASS’) and $s.s_nationkey eq $n.n_nationkey and $n.n_regionkey eq $r.r_regionkey and $r.r_name eq ’EUROPE’
let $supplycost :=
for $ps in $schema.partsupp,
$s in $schema.supplier, $n in $schema.nation, $r in $schema.region
where $p.p_partkey eq $ps.ps_partkey and $s.s_suppkey eq $ps.ps_suppkey and $s.s_nationkey eq $n.n_nationkey and $n.n_regionkey eq $r.r_regionkey and $r.r_name eq ’EUROPE’
return $ps.ps_supplycost
where $ps.ps_supplycost eq min($supplycost) order by $s.s_acctbal descending,
         $n.n_name, $s.s_name, $p.p_partkey
return
  {
    s_acctbal : $s.s_acctbal,
    s_name : $s.s_name,
    n_name : $n.n_name,
    p_partkey : $p.p_partkey,
    p_mfgr : $p.p_mfgr,
    s_address : $s.s_address,
    s_phone : $s.s_phone,
    s_comment : $s.s_comment
}