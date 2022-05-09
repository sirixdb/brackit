
declare default
element namespace "http://www.fixprotocol.org/FIXML-4-4";
declare namespace c="http://tpox-benchmark.com/custacc"; for $ordxml in
    ( bit:partition min=50 max=50 queue=6 )
{ tpox:read-batch(’order/batch-1.xml’) } let $ord := bit:parse($ordxml)/FIXML/Order for $custxml in
     ( bit:partition min=50 max=50 )
{ tpox:read-batch(’custacc/batch-1.xml’) } let $cust := bit:parse($custxml)/c:Customer where $ord/OrdQty/@Cash>3000
and $cust/c:CountryOfResidence = "Germany"
and $cust/c:Accounts/c:Account/@id = $ord/@Acct/fn:string(.) return $cust