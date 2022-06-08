declare ordering unordered;
declare variable $schema-file external;
let $schema := rel:parse-schema($schema-file, (), ()) for $l in $schema=>lineitem
where $l=>l_discount le 0.07
and $l=>l_discount ge 0.05
and $l=>l_shipdate ge xs:date("1994-01-01") and $l=>l_shipdate lt xs:date("1994-01-01") +
xs:yearMonthDuration("P1Y") and $l=>l_quantity lt 24
let $revenue := $l=>l_extendedprice * $l=>l_discount group by *
return
  { revenue : sum($revenue) }