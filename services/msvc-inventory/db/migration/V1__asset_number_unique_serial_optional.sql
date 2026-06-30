-- ============================================================================
-- Migración: número de activo único + número de serie único/opcional
--            + IP/MAC opcionales en network_interface
-- BD: PostgreSQL | Tablas: asset_table, network_interface
--
-- Contexto:
--   - El proyecto usa Hibernate ddl-auto: update (sin Flyway/Liquibase).
--     Hibernate intentaría crear la constraint única solo, pero falla en
--     silencio (solo log) si existen duplicados. Este script lo hace de forma
--     controlada: valida duplicados antes de aplicar y corre en transacción.
--   - Seguro de re-ejecutar (idempotente).
--
-- Uso:
--   1. Ejecutar primero el PASO 0 (solo lectura) y confirmar 0 filas.
--   2. Ejecutar el bloque MIGRACIÓN.
--   3. Ejecutar la VERIFICACIÓN.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- PASO 0 (PREVIO, solo lectura): detectar duplicados de asset_number.
-- El índice único físico cuenta TODAS las filas, incluidas las borradas
-- lógicamente (is_deleted = true). Debe devolver 0 filas antes de continuar.
-- ----------------------------------------------------------------------------
SELECT asset_number, COUNT(*) AS total
FROM asset_table
GROUP BY asset_number
HAVING COUNT(*) > 1
ORDER BY total DESC;

-- ============================================================================
-- MIGRACIÓN
-- ============================================================================
BEGIN;

DO $$
BEGIN
    -- 1. Cortar si todavía existen asset_number duplicados (no romper la BD).
    IF EXISTS (
        SELECT 1
        FROM asset_table
        GROUP BY asset_number
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Existen asset_number duplicados (incluyendo filas con is_deleted=true). Resuelvelos antes de aplicar la constraint unica.';
    END IF;

    -- 2. asset_number: crear constraint unica si no existe.
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_asset_table_asset_number'
    ) THEN
        ALTER TABLE asset_table
            ADD CONSTRAINT uk_asset_table_asset_number UNIQUE (asset_number);
        RAISE NOTICE 'Constraint uk_asset_table_asset_number creada.';
    ELSE
        RAISE NOTICE 'Constraint uk_asset_table_asset_number ya existia; sin cambios.';
    END IF;

    -- 3. asset_number: asegurar que NO sea nullable (sigue siendo obligatorio).
    BEGIN
        ALTER TABLE asset_table ALTER COLUMN asset_number SET NOT NULL;
    EXCEPTION WHEN others THEN
        RAISE NOTICE 'asset_number ya era NOT NULL o no se pudo ajustar; sin cambios.';
    END;

    -- 4. serial_number: garantizar que sea OPCIONAL (nullable).
    --    (El cambio es a nivel de validacion de la app; aqui solo aseguramos la BD.)
    BEGIN
        ALTER TABLE asset_table ALTER COLUMN serial_number DROP NOT NULL;
    EXCEPTION WHEN others THEN
        RAISE NOTICE 'serial_number ya era nullable; sin cambios.';
    END;

    -- 5. serial_number: asegurar constraint unica (deberia existir ya).
    --    NOTA: en Postgres, UNIQUE permite multiples NULL, asi que la serie
    --    puede quedar vacia en muchos activos sin colisionar.
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_attribute a
          ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
        WHERE c.conrelid = 'asset_table'::regclass
          AND c.contype = 'u'
          AND a.attname = 'serial_number'
          AND array_length(c.conkey, 1) = 1
    ) THEN
        ALTER TABLE asset_table
            ADD CONSTRAINT uk_asset_table_serial_number UNIQUE (serial_number);
        RAISE NOTICE 'Constraint unica de serial_number creada.';
    ELSE
        RAISE NOTICE 'serial_number ya tiene constraint unica; sin cambios.';
    END IF;

    -- 6. network_interface.ip_address: garantizar que sea OPCIONAL (nullable).
    --    La app y la entidad ahora permiten IP nula; sin este DROP NOT NULL,
    --    insertar un activo de red sin IP fallaria a nivel de BD.
    --    La constraint UNIQUE se conserva (Postgres permite multiples NULL).
    BEGIN
        ALTER TABLE network_interface ALTER COLUMN ip_address DROP NOT NULL;
    EXCEPTION WHEN others THEN
        RAISE NOTICE 'network_interface.ip_address ya era nullable o no se pudo ajustar; sin cambios.';
    END;

    -- 7. network_interface.mac_address: garantizar que sea OPCIONAL (nullable).
    BEGIN
        ALTER TABLE network_interface ALTER COLUMN mac_address DROP NOT NULL;
    EXCEPTION WHEN others THEN
        RAISE NOTICE 'network_interface.mac_address ya era nullable o no se pudo ajustar; sin cambios.';
    END;
END $$;

COMMIT;

-- ============================================================================
-- VERIFICACIÓN (post-migración, solo lectura)
-- ============================================================================
SELECT conname, contype, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid = 'asset_table'::regclass
  AND contype = 'u'
ORDER BY conname;

-- Nullability esperada: asset_number=NO, serial_number=YES,
-- network_interface.ip_address=YES, network_interface.mac_address=YES.
SELECT table_name, column_name, is_nullable
FROM information_schema.columns
WHERE (table_name = 'asset_table'        AND column_name IN ('asset_number', 'serial_number'))
   OR (table_name = 'network_interface'  AND column_name IN ('ip_address', 'mac_address'))
ORDER BY table_name, column_name;

-- ============================================================================
-- ROLLBACK (ejecutar manualmente solo si se necesita revertir)
-- ============================================================================
-- BEGIN;
-- ALTER TABLE asset_table DROP CONSTRAINT IF EXISTS uk_asset_table_asset_number;
-- -- Reversa opcional de la unicidad de serie (solo si quieres deshacerla):
-- -- ALTER TABLE asset_table DROP CONSTRAINT IF EXISTS uk_asset_table_serial_number;
-- COMMIT;
