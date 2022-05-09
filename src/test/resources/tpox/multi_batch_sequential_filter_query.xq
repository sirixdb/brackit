declare default
element namespace "http://tpox-benchmark.com/custacc";
declare variable $count external; for $batch in (1 to $count)
for $xml in
    ( bit:partition min=50 max=50 queue=6 )
{ tpox:read-batch(concat(’batch-’, $batch, ’.xml’)) } let $customer := bit:parse($xml)/Customer
let $balance := (
for $account in $customer/Accounts/Account
let $obalance := $account/Balance/OnlineActualBal where $obalance > 900000
and $account/Currency = "EUR"
and ($customer/Nationality = "Greece"
or $customer/Nationality = "Germany") return $obalance
)
where not(empty($balance))
return
<premium_customer id="{$customer/@id}">
    <name>{$customer/ShortNames/ShortName/text()}</name>
    <balance>{max($balance)}</balance>
    <nationality>{$customer/Nationality/text()}</nationality>
</premium_customer>