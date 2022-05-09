
declare default
element namespace "http://tpox-benchmark.com/custacc";
declare variable $count external; for $batch in (1 to $count)
for $xml in
    ( bit:partition min=50 max=50 queue=6 )
{ tpox:read-batch(concat(’custacc/batch-’, $batch, ’.xml’) } let $customer := bit:parse($xml)/Customer
where $customer/Accounts/Account/Balance/
OnlineActualBal > 500000
let $nationality := $customer/Nationality group by $nationality
return
<nation>
  <name>{$nationality}</name>
  <count>{count($customer)}</count>
</nation>
