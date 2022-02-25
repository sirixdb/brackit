module namespace sort = "https://sirix.io/ns/sort";

declare function sort:qsort($values) {
  $len := count($values);
  if ($len <= 1) then (
    $values
  ) else (
    $pivot := $values[$len idiv 2];
    $less := $values[. < $pivot];
    $greater := $values[. > $pivot];
    (sort:qsort($less), $pivot, sort:qsort($greater))
  )
};