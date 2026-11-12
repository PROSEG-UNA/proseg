UPDATE driver_table
SET ordinary_end = time '20:00'
WHERE ordinary_start = time '12:00'
  AND ordinary_end = time '21:00';
