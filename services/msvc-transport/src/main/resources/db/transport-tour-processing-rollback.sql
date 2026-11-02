DO $migration$
DECLARE
    item record;
BEGIN
    FOR item IN
        SELECT * FROM (VALUES
            ('tour_processing_draft_table', 'cleaning_draft_table'),
            ('tour_processing_draft_tour_table', 'cleaning_draft_tour_table'),
            ('tour_processing_draft_assignment_table', 'cleaning_draft_assignment_table'),
            ('tour_processing_draft_day_table', 'cleaning_draft_day_table'),
            ('tour_processing_draft_import_row_table', 'cleaning_draft_import_row_table'),
            ('tour_processing_execution_table', 'cleaning_execution_table'),
            ('tour_processing_execution_detail_table', 'cleaning_execution_detail_table')
        ) AS table_names(new_name, old_name)
    LOOP
        IF to_regclass(item.new_name) IS NOT NULL THEN
            IF to_regclass(item.old_name) IS NOT NULL THEN
                RAISE EXCEPTION 'Existen ambas tablas: % y %', item.new_name, item.old_name;
            END IF;
            EXECUTE format('ALTER TABLE %I RENAME TO %I', item.new_name, item.old_name);
        END IF;
    END LOOP;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'cleaning_draft_table' AND column_name = 'tour_processing_execution_id'
    ) THEN
        ALTER TABLE cleaning_draft_table RENAME COLUMN tour_processing_execution_id TO cleaning_execution_id;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'cleaning_execution_detail_table' AND column_name = 'tour_processing_execution_id'
    ) THEN
        ALTER TABLE cleaning_execution_detail_table RENAME COLUMN tour_processing_execution_id TO cleaning_execution_id;
    END IF;

    FOR item IN
        SELECT relation.relname AS table_name, constraint_name.conname AS name
        FROM pg_constraint constraint_name
        JOIN pg_class relation ON relation.oid = constraint_name.conrelid
        LEFT JOIN pg_class referenced_relation ON referenced_relation.oid = constraint_name.confrelid
        WHERE (relation.relname LIKE 'cleaning_%' OR referenced_relation.relname LIKE 'cleaning_%')
          AND constraint_name.conname LIKE '%tour_processing%'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I RENAME CONSTRAINT %I TO %I',
            item.table_name,
            item.name,
            replace(item.name, 'tour_processing', 'cleaning')
        );
    END LOOP;

    FOR item IN
        SELECT index_relation.relname AS name
        FROM pg_index table_index
        JOIN pg_class index_relation ON index_relation.oid = table_index.indexrelid
        JOIN pg_class table_relation ON table_relation.oid = table_index.indrelid
        WHERE table_relation.relname LIKE 'cleaning_%'
          AND index_relation.relname LIKE '%tour_processing%'
    LOOP
        EXECUTE format(
            'ALTER INDEX %I RENAME TO %I',
            item.name,
            replace(item.name, 'tour_processing', 'cleaning')
        );
    END LOOP;
END
$migration$;

DELETE FROM transport_schema_migration WHERE version = 4;
