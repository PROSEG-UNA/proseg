
-- Eliminar cualquier CHECK constraint que referencie status (checks heredados
-- del tipo entero que impiden cambiar la columna a varchar).
DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT con.conrelid::regclass AS tabla, con.conname
        FROM pg_constraint con
        WHERE con.contype = 'c'
          AND con.conrelid IN (
              'maintenance_request_table'::regclass,
              'maintenance_register_table'::regclass
          )
          AND pg_get_constraintdef(con.oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', r.tabla, r.conname);
    END LOOP;
END $$;

-- Convertir status a varchar(20). Se castea a text en el CASE para que funcione
-- tanto si la columna es entera como si ya es varchar.
ALTER TABLE maintenance_request_table
    ALTER COLUMN status TYPE varchar(20)
    USING (
        CASE status::text
            WHEN '2' THEN 'COMPLETED'
            WHEN '4' THEN 'CANCELLED'
            WHEN 'COMPLETED' THEN 'COMPLETED'
            WHEN 'CANCELLED' THEN 'CANCELLED'
            ELSE 'PENDING'
        END
    );

ALTER TABLE maintenance_register_table
    ALTER COLUMN status TYPE varchar(20)
    USING (
        CASE status::text
            WHEN '2' THEN 'COMPLETED'
            WHEN '4' THEN 'CANCELLED'
            WHEN 'COMPLETED' THEN 'COMPLETED'
            WHEN 'CANCELLED' THEN 'CANCELLED'
            ELSE 'PENDING'
        END
    );

ALTER TABLE maintenance_request_table
    ADD COLUMN IF NOT EXISTS cancellation_reason TEXT;

-- =====================================================================
-- Verificacion de la migracion: falla si algo no quedo correcto
-- =====================================================================
-- DO $$
-- DECLARE
--     invalid_count integer;
-- BEGIN
--     -- 1. La columna status debe ser varchar(20) en maintenance_request_table
--     IF NOT EXISTS (
--         SELECT 1 FROM information_schema.columns
--         WHERE table_name = 'maintenance_request_table'
--           AND column_name = 'status'
--           AND data_type = 'character varying'
--           AND character_maximum_length = 20
--     ) THEN
--         RAISE EXCEPTION 'maintenance_request_table.status no es varchar(20)';
--     END IF;
--
--     -- 2. La columna status debe ser varchar(20) en maintenance_register_table
--     IF NOT EXISTS (
--         SELECT 1 FROM information_schema.columns
--         WHERE table_name = 'maintenance_register_table'
--           AND column_name = 'status'
--           AND data_type = 'character varying'
--           AND character_maximum_length = 20
--     ) THEN
--         RAISE EXCEPTION 'maintenance_register_table.status no es varchar(20)';
--     END IF;
--
--     -- 3. La columna cancellation_reason debe existir como text
--     IF NOT EXISTS (
--         SELECT 1 FROM information_schema.columns
--         WHERE table_name = 'maintenance_request_table'
--           AND column_name = 'cancellation_reason'
--           AND data_type = 'text'
--     ) THEN
--         RAISE EXCEPTION 'maintenance_request_table.cancellation_reason no existe o no es text';
--     END IF;
--
--     -- 4. status solo puede contener los valores permitidos
--     SELECT COUNT(*) INTO invalid_count
--     FROM (
--         SELECT status FROM maintenance_request_table
--         UNION ALL
--         SELECT status FROM maintenance_register_table
--     ) s
--     WHERE status NOT IN ('PENDING', 'COMPLETED', 'CANCELLED');
--
--     IF invalid_count > 0 THEN
--         RAISE EXCEPTION 'Existen % filas con un status fuera de (PENDING, COMPLETED, CANCELLED)', invalid_count;
--     END IF;
--
--     RAISE NOTICE 'Migracion V006 verificada correctamente';
-- END $$;
