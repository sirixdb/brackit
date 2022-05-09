declare default
element namespace "http://tpox-benchmark.com/custacc";
for $doc in
( bit:partition min=50 max=50 queue=6 ) { tpox:parse-batch(’batch-1.xml’) }
let $customer := $doc/Customer let $balance := (
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