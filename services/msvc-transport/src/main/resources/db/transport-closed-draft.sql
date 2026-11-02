ALTER TABLE tour_processing_draft_table DROP CONSTRAINT IF EXISTS tour_processing_draft_table_status_check;
ALTER TABLE tour_processing_draft_table ADD CONSTRAINT tour_processing_draft_table_status_check CHECK (status IN ('DRAFT', 'REGISTERED', 'CLOSED'));
