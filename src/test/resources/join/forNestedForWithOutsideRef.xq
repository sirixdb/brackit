let $x := 1
let $y := 1 
let $z := (
for $a in ($x to 5)
for $b in (1 to 2)
let $c:= for $d in (($x+1 to 3),(5 to 8))
         where $a = $d
         let $e := $y         
         return $d
return $c + $x
)
return $z