DO $migration$
BEGIN
    IF to_regclass('cleaning_draft_row_table') IS NULL THEN
        RETURN;
    END IF;

    IF EXISTS (SELECT 1 FROM tour_processing_draft_tour_table) THEN
        RAISE EXCEPTION 'La migración requiere las tablas nuevas vacías';
    END IF;

    IF EXISTS (
        SELECT 1 FROM cleaning_draft_row_table r
        WHERE r.distribution_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM cleaning_draft_row_table root
            WHERE root.draft_id = r.draft_id AND root.distribution_id = r.distribution_id AND root.distribution_part = 1
        )
    ) THEN
        RAISE EXCEPTION 'Hay distribuciones sin gira principal';
    END IF;

    INSERT INTO tour_processing_draft_tour_table (
        id, created_at, updated_at, created_by, updated_by, is_deleted, draft_id, row_number, number,
        approval_status, passengers, executing_unit, responsible, destination, duration_days, priority,
        modality, departure_time, return_time, departure_date, return_date, observations,
        duplicate_group_size, duplicate_rank, conflicting
    )
    SELECT id, created_at, updated_at, created_by, updated_by, is_deleted, draft_id, row_number, number,
           approval_status, coalesce(distribution_total_passengers, passengers), executing_unit, responsible,
           destination, duration_days, priority, modality, departure_time, return_time, departure_date,
           return_date, observations, duplicate_group_size, duplicate_rank, conflicting
    FROM cleaning_draft_row_table WHERE coalesce(distribution_part, 1) = 1;

    INSERT INTO tour_processing_draft_assignment_table (
        id, created_at, updated_at, created_by, updated_by, is_deleted, tour_id, part, passengers,
        resource_type, driver, driver_id, vehicle, vehicle_id, borrowed_vehicle_id
    )
    SELECT r.id, r.created_at, r.updated_at, r.created_by, r.updated_by, r.is_deleted,
           coalesce(root.id, r.id), coalesce(r.distribution_part, 1), r.passengers,
           r.resource_type, coalesce(d.name, r.driver), d.id, coalesce(v.plate, r.vehicle), v.id, r.borrowed_vehicle_id
    FROM cleaning_draft_row_table r
    LEFT JOIN cleaning_draft_row_table root ON root.draft_id = r.draft_id
        AND root.distribution_id = r.distribution_id AND root.distribution_part = 1
    LEFT JOIN LATERAL (
        SELECT id, name FROM driver_table
        WHERE NOT is_deleted AND r.resource_type <> 'EXTERNAL'
          AND regexp_replace(lower(trim(name)), '\s+', ' ', 'g') = regexp_replace(lower(trim(r.driver)), '\s+', ' ', 'g')
        ORDER BY created_at, id LIMIT 1
    ) d ON true
    LEFT JOIN LATERAL (
        SELECT id, plate FROM vehicle_table
        WHERE NOT is_deleted AND r.resource_type = 'INTERNAL' AND r.vehicle ~ '[0-9]'
          AND left(upper(regexp_replace(plate, '\s+', '', 'g')), 20) = left(upper(regexp_replace(r.vehicle, '\s+', '', 'g')), 20)
        ORDER BY created_at, id LIMIT 1
    ) v ON true;

    INSERT INTO tour_processing_draft_day_table (
        id, created_at, updated_at, created_by, updated_by, is_deleted, assignment_id,
        date, start_time, end_time, ordinary_hours, surplus_hours, overtime_hours, manually_edited
    )
    SELECT d.id, d.created_at, d.updated_at, d.created_by, d.updated_by, d.is_deleted, d.row_id,
           d.date, d.start_time, d.end_time, d.ordinary_hours, d.surplus_hours, d.overtime_hours, d.manually_edited
    FROM cleaning_draft_row_day_table d
    JOIN tour_processing_draft_assignment_table a ON a.id = d.row_id
    JOIN tour_processing_draft_tour_table t ON t.id = a.tour_id
    WHERE t.duplicate_rank = 1;

    IF (SELECT count(*) FROM tour_processing_draft_assignment_table) <> (SELECT count(*) FROM cleaning_draft_row_table) THEN
        RAISE EXCEPTION 'La migración no conservó todas las asignaciones';
    END IF;
END
$migration$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tour_processing_draft_assignment_part ON tour_processing_draft_assignment_table(tour_id, part);
CREATE INDEX IF NOT EXISTS idx_tour_processing_draft_day_date ON tour_processing_draft_day_table(date, assignment_id);
