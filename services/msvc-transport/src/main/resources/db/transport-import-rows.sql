INSERT INTO tour_processing_draft_import_row_table(id, draft_id, created_at, updated_at, created_by, updated_by, is_deleted, payload)
SELECT t.id, t.draft_id, t.created_at, t.updated_at, t.created_by, t.updated_by, t.is_deleted,
       jsonb_build_object('id', t.id, 'rowNumber', t.row_number, 'number', t.number, 'approvalStatus', t.approval_status, 'passengers', t.passengers, 'executingUnit', t.executing_unit, 'responsible', t.responsible, 'destination', t.destination, 'durationDays', t.duration_days, 'priority', t.priority, 'modality', t.modality, 'departureTime', t.departure_time, 'returnTime', t.return_time, 'departureDate', t.departure_date, 'returnDate', t.return_date, 'observations', t.observations, 'duplicateGroupSize', t.duplicate_group_size, 'duplicateRank', t.duplicate_rank, 'conflicting', t.conflicting,
           'driver', (SELECT a.driver FROM tour_processing_draft_assignment_table a WHERE a.tour_id = t.id AND a.part = 1),
           'vehicle', (SELECT a.vehicle FROM tour_processing_draft_assignment_table a WHERE a.tour_id = t.id AND a.part = 1),
           'assignments', (SELECT jsonb_agg(jsonb_build_object(
               'rowId', a.id, 'driver', a.driver, 'driverId', a.driver_id,
               'vehicle', a.vehicle, 'vehicleId', a.vehicle_id, 'passengers', a.passengers,
               'resourceType', a.resource_type, 'borrowedVehicleId', a.borrowed_vehicle_id
           ) ORDER BY a.part) FROM tour_processing_draft_assignment_table a WHERE a.tour_id = t.id))
FROM tour_processing_draft_tour_table t WHERE t.duplicate_rank > 1;

DELETE FROM tour_processing_draft_day_table WHERE assignment_id IN (
    SELECT a.id FROM tour_processing_draft_assignment_table a JOIN tour_processing_draft_tour_table t ON t.id = a.tour_id WHERE t.duplicate_rank > 1
);
DELETE FROM tour_processing_draft_assignment_table WHERE tour_id IN (SELECT id FROM tour_processing_draft_tour_table WHERE duplicate_rank > 1);
DELETE FROM tour_processing_draft_tour_table WHERE duplicate_rank > 1;
