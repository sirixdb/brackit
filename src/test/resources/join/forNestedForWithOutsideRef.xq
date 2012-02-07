for $a in (1 to 5)
for $b in (1 to 2)
let $c:= for $d in ((2 to 3),(5 to 8))
         where $a = $d         
         return $d
return $c