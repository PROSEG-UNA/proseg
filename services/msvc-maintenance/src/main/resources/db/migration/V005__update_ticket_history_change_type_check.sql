DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ticket_history_change_table_type_check'
          AND conrelid = 'ticket_history_change_table'::regclass
    ) THEN
        ALTER TABLE ticket_history_change_table
            DROP CONSTRAINT ticket_history_change_table_type_check;
    END IF;

    ALTER TABLE ticket_history_change_table
        ADD CONSTRAINT ticket_history_change_table_type_check
        CHECK (
            type IN (
                'ATTACHMENT_ADDED',
                'ATTACHMENT_REMOVED',
                'PRIORITY_CHANGED',
                'STATUS_CHANGED',
                'ASSIGNED_ROLE_CHANGED',
                'EDITED',
                'COMMENT_ADDED',
                'COMMENT_EDITED',
                'COMMENT_REMOVED',
                'RESOLVED',
                'OTHER'
            )
        );
END $$;
