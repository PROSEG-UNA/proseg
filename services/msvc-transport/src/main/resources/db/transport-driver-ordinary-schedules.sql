CREATE TEMP TABLE migration_peripheral_driver ON COMMIT DROP AS
SELECT DISTINCT driver_id FROM driver_ulloa_shift;

WITH classified AS (
    SELECT d.id,
           CASE
               WHEN EXISTS (SELECT 1 FROM driver_ulloa_shift s WHERE s.driver_id = d.id AND s.ulloa_shift = 'AFTERNOON') THEN true
               WHEN EXISTS (SELECT 1 FROM driver_ulloa_shift s WHERE s.driver_id = d.id) THEN false
               ELSE coalesce(d.ordinary_start, time '07:30') >= time '10:00'
           END AS afternoon
    FROM driver_table d
    WHERE d.ordinary_start IS NOT NULL
       OR d.ordinary_end IS NOT NULL
       OR EXISTS (SELECT 1 FROM driver_ulloa_shift s WHERE s.driver_id = d.id)
),
target AS (
    SELECT id,
           CASE WHEN afternoon THEN time '12:00' ELSE time '07:30' END AS ordinary_start,
           CASE WHEN afternoon THEN time '21:00' ELSE time '16:30' END AS ordinary_end
    FROM classified
)
UPDATE driver_table d
SET ordinary_start = t.ordinary_start,
    ordinary_end = t.ordinary_end
FROM target t
WHERE d.id = t.id
  AND (d.ordinary_start IS DISTINCT FROM t.ordinary_start OR d.ordinary_end IS DISTINCT FROM t.ordinary_end);

DELETE FROM driver_ulloa_shift;

INSERT INTO driver_ulloa_shift (driver_id, ulloa_shift)
SELECT d.id, shift.name
FROM driver_table d
JOIN migration_peripheral_driver p ON p.driver_id = d.id
JOIN (VALUES ('SHORT_MORNING', time '07:30'), ('MORNING', time '07:30'), ('AFTERNOON', time '12:00'))
        AS shift(name, window_start) ON shift.window_start = d.ordinary_start;
