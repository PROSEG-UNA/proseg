-- ============================================================================
-- Migración: número de activo YA NO es único
-- BD: PostgreSQL | Tabla: asset_table
--
-- Contexto:
--   - Revierte la unicidad de asset_number introducida en
--     V1__asset_number_unique_serial_optional.sql. En adquisiciones en conjunto
--     (varios equipos de un mismo paquete) los activos comparten número.
--   - La creación/edición individual desde el formulario permite repetirlo
--     previa confirmación del usuario; la importación masiva lo sigue
--     rechazando a nivel de aplicación.
--   - El proyecto usa Hibernate ddl-auto: update (sin Flyway/Liquibase), que
--     NUNCA elimina constraints existentes. Quitar unique = true de la entidad
--     no basta: hay que ejecutar este script.
--   - Se dropea por atributo y no por nombre, porque además de
--     uk_asset_table_asset_number puede existir una constraint o índice único
--     auto-nombrado creado por Hibernate.
--   - Seguro de re-ejecutar (idempotente).
--
-- Uso:
--   1. Ejecutar primero el PASO 0 (solo lectura) para ver qué existe hoy.
--   2. Ejecutar el bloque MIGRACIÓN.
--   3. Ejecutar la VERIFICACIÓN.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- PASO 0 (PREVIO, solo lectura): listar constraints e índices únicos que hoy
-- afectan a asset_table.asset_number.
-- ----------------------------------------------------------------------------
SELECT c.conname AS nombre, 'constraint' AS objeto, pg_get_constraintdef(c.oid) AS definicion
FROM pg_constraint c
JOIN pg_attribute a
  ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
WHERE c.conrelid = 'asset_table'::regclass
  AND c.contype = 'u'
  AND a.attname = 'asset_number'
  AND array_length(c.conkey, 1) = 1
UNION ALL
SELECT i.relname, 'index', pg_get_indexdef(i.oid)
FROM pg_index x
JOIN pg_class i ON i.oid = x.indexrelid
JOIN pg_attribute a ON a.attrelid = x.indrelid AND a.attnum = ANY (x.indkey)
WHERE x.indrelid = 'asset_table'::regclass
  AND x.indisunique
  AND NOT x.indisprimary
  AND a.attname = 'asset_number'
  AND x.indnatts = 1
ORDER BY objeto, nombre;

-- ============================================================================
-- MIGRACIÓN
-- ============================================================================
BEGIN;

DO $$
DECLARE
    objeto RECORD;
    encontrados INTEGER := 0;
BEGIN
    -- 1. Eliminar toda constraint UNIQUE de una sola columna sobre asset_number,
    --    sin importar cómo se llame.
    FOR objeto IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_attribute a
          ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
        WHERE c.conrelid = 'asset_table'::regclass
          AND c.contype = 'u'
          AND a.attname = 'asset_number'
          AND array_length(c.conkey, 1) = 1
    LOOP
        EXECUTE format('ALTER TABLE asset_table DROP CONSTRAINT %I', objeto.conname);
        RAISE NOTICE 'Constraint unica % eliminada.', objeto.conname;
        encontrados := encontrados + 1;
    END LOOP;

    -- 2. Eliminar índices únicos huérfanos (sin constraint asociada) sobre
    --    asset_number: Hibernate puede haberlos creado por su cuenta.
    FOR objeto IN
        SELECT i.relname
        FROM pg_index x
        JOIN pg_class i ON i.oid = x.indexrelid
        JOIN pg_attribute a ON a.attrelid = x.indrelid AND a.attnum = ANY (x.indkey)
        WHERE x.indrelid = 'asset_table'::regclass
          AND x.indisunique
          AND NOT x.indisprimary
          AND a.attname = 'asset_number'
          AND x.indnatts = 1
          AND NOT EXISTS (
              SELECT 1 FROM pg_constraint c WHERE c.conindid = x.indexrelid
          )
    LOOP
        EXECUTE format('DROP INDEX %I', objeto.relname);
        RAISE NOTICE 'Indice unico % eliminado.', objeto.relname;
        encontrados := encontrados + 1;
    END LOOP;

    IF encontrados = 0 THEN
        RAISE NOTICE 'asset_number ya no tenia unicidad; sin cambios.';
    END IF;

    -- 3. asset_number sigue siendo OBLIGATORIO: reafirmar NOT NULL.
    BEGIN
        ALTER TABLE asset_table ALTER COLUMN asset_number SET NOT NULL;
    EXCEPTION WHEN others THEN
        RAISE NOTICE 'asset_number ya era NOT NULL o no se pudo ajustar; sin cambios.';
    END;

    -- 4. serial_number conserva su unicidad: no se toca en esta migracion.
END $$;

COMMIT;

-- ============================================================================
-- VERIFICACIÓN (post-migración, solo lectura)
-- ============================================================================
-- Esperado: uk_asset_table_asset_number ya NO aparece;
--           uk_asset_table_serial_number sí sigue apareciendo.
SELECT conname, contype, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid = 'asset_table'::regclass
  AND contype = 'u'
ORDER BY conname;

-- Esperado: 0 filas (ningún índice único sobre asset_number).
SELECT i.relname, pg_get_indexdef(i.oid) AS definicion
FROM pg_index x
JOIN pg_class i ON i.oid = x.indexrelid
JOIN pg_attribute a ON a.attrelid = x.indrelid AND a.attnum = ANY (x.indkey)
WHERE x.indrelid = 'asset_table'::regclass
  AND x.indisunique
  AND NOT x.indisprimary
  AND a.attname = 'asset_number'
  AND x.indnatts = 1;

-- Nullability esperada: asset_number=NO, serial_number=YES.
SELECT table_name, column_name, is_nullable
FROM information_schema.columns
WHERE table_name = 'asset_table'
  AND column_name IN ('asset_number', 'serial_number')
ORDER BY column_name;

-- ============================================================================
-- ROLLBACK (ejecutar manualmente solo si se necesita revertir)
-- ATENCIÓN: fallará si ya existen asset_number duplicados. Resolverlos antes,
-- usando el PASO 0 de V1__asset_number_unique_serial_optional.sql.
-- ============================================================================
-- BEGIN;
-- ALTER TABLE asset_table
--     ADD CONSTRAINT uk_asset_table_asset_number UNIQUE (asset_number);
-- COMMIT;
