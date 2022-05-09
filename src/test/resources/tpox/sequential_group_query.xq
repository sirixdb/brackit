declare default
element namespace "http://tpox-benchmark.com/custacc";
for $xml in
( bit:partition min=50 max=50 queue=6 )
{ tpox:read-batch(’custacc/batch-1.xml’) }
let $customer := bit:parse($xml)/Customer where $customer/Accounts/Account/Balance/
OnlineActualBal > 500000
let $nationality := $customer/Nationality group by $nationality
return
<nation>
  <name>{$nationality}</name>
  <count>{count($customer)}</count>
</nation>