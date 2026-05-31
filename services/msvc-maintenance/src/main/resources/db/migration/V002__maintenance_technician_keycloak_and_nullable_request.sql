-- Add keycloak_user_id column and make maintenance_request_id nullable
ALTER TABLE maintenance_technician_table ADD COLUMN keycloak_user_id VARCHAR(255);
ALTER TABLE maintenance_technician_table ALTER COLUMN maintenance_request_id DROP NOT NULL;

