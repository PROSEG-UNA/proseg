UPDATE maintenance_request_table
SET status = 'PENDING'
WHERE status = 'ACCEPTED';

UPDATE maintenance_register_table
SET status = 'PENDING'
WHERE status = 'ACCEPTED';
