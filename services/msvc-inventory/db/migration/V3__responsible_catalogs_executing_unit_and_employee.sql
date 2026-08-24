-- ============================================================================
-- Migración: unidad ejecutora y funcionario pasan de texto libre a catálogo
-- BD: PostgreSQL | Tablas: asset_table, executing_unit_table, employee_table
--
-- Contexto:
--   - Las columnas asset_table.executing_unit, .responsible_employee y
--     .responsible_employee_id eran texto libre. Ahora son dos FK reales:
--         executing_unit_id -> executing_unit_table
--         employee_id       -> employee_table
--   - "Identificación Funcionario" y "Nombre Funcionario" son DOS VISTAS de la
--     MISMA entidad Employee. Por eso un funcionario tiene, como máximo, una
--     identificación: es estructural (es una columna), no una regla de negocio.
--   - El proyecto usa Hibernate ddl-auto: update (sin Flyway/Liquibase).
--     Hibernate CREA las tablas nuevas y las columnas FK al arrancar, pero
--     NUNCA elimina las tres columnas de texto ni crea índices únicos
--     funcionales. Este script hace ambas cosas de forma controlada.
--   - Seguro de re-ejecutar (idempotente).
--
-- Uso:
--   1. Hacer backup de la BD (como mínimo de asset_table).
--   2. Ejecutar el PASO 0 (solo lectura) y ARCHIVAR los tres reportes: son la
--      única traza de las divergencias que el backfill resuelve automáticamente.
--   3. Arrancar msvc-inventory con el código nuevo, con el tráfico cortado
--      (crea las tablas, las columnas FK y sus constraints).
--   4. Ejecutar el bloque MIGRACIÓN.
--   5. Ejecutar la VERIFICACIÓN y reabrir el tráfico.
--
-- Criterio de backfill (acordado con el equipo):
--   - Cada valor DISTINCT de executing_unit genera una unidad ejecutora.
--   - Cada valor DISTINCT de responsible_employee genera UN funcionario, con
--     la identificación NO NULA más frecuente asociada a ese nombre.
--   - Si esa identificación la reclaman varios nombres, gana el nombre con más
--     activos; los demás quedan con identificación NULL (ver PASO 0.B).
--   - Los activos con identificación pero SIN nombre no pueden generar
--     funcionario (name es NOT NULL) y quedan sin funcionario (ver PASO 0.C).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- PASO 0.A (PREVIO, solo lectura): mismo nombre con varias identificaciones.
-- Solo la más frecuente sobrevive; el resto se pierde. Revisar a mano después.
-- ----------------------------------------------------------------------------
SELECT btrim(responsible_employee)                                AS funcionario,
       count(DISTINCT lower(btrim(responsible_employee_id)))      AS identificaciones_distintas,
       string_agg(DISTINCT btrim(responsible_employee_id), ' | ') AS valores,
       count(*)                                                   AS activos
FROM asset_table
WHERE responsible_employee    IS NOT NULL AND btrim(responsible_employee)    <> ''
  AND responsible_employee_id IS NOT NULL AND btrim(responsible_employee_id) <> ''
GROUP BY lower(btrim(responsible_employee)), btrim(responsible_employee)
HAVING count(DISTINCT lower(btrim(responsible_employee_id))) > 1
ORDER BY identificaciones_distintas DESC, activos DESC;

-- ----------------------------------------------------------------------------
-- PASO 0.B (PREVIO, solo lectura): misma identificación con varios nombres.
-- Solo uno de esos nombres conservará la identificación tras la migración.
-- ----------------------------------------------------------------------------
SELECT btrim(responsible_employee_id)                          AS identificacion,
       count(DISTINCT lower(btrim(responsible_employee)))      AS nombres_distintos,
       string_agg(DISTINCT btrim(responsible_employee), ' | ') AS valores,
       count(*)                                                AS activos
FROM asset_table
WHERE responsible_employee    IS NOT NULL AND btrim(responsible_employee)    <> ''
  AND responsible_employee_id IS NOT NULL AND btrim(responsible_employee_id) <> ''
GROUP BY lower(btrim(responsible_employee_id)), btrim(responsible_employee_id)
HAVING count(DISTINCT lower(btrim(responsible_employee))) > 1
ORDER BY nombres_distintos DESC, activos DESC;

-- ----------------------------------------------------------------------------
-- PASO 0.C (PREVIO, solo lectura): activos con identificación pero sin nombre.
-- No pueden generar funcionario y quedarán sin responsable asignado.
-- ----------------------------------------------------------------------------
SELECT id,
       asset_number,
       btrim(responsible_employee_id) AS identificacion_huerfana
FROM asset_table
WHERE (responsible_employee IS NULL OR btrim(responsible_employee) = '')
  AND responsible_employee_id IS NOT NULL AND btrim(responsible_employee_id) <> ''
ORDER BY asset_number;

-- ============================================================================
-- MIGRACIÓN
-- ============================================================================
BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    unidades_creadas     INTEGER := 0;
    funcionarios_creados INTEGER := 0;
    activos_unidad       INTEGER := 0;
    activos_funcionario  INTEGER := 0;
    huerfanos            INTEGER := 0;
    sin_enlazar          INTEGER := 0;
BEGIN

    -- ------------------------------------------------------------------------
    -- 1. Tablas de catálogo. Con ddl-auto ya existirían; se crean aquí por si
    --    el script se ejecuta antes de arrancar la aplicación.
    -- ------------------------------------------------------------------------
    CREATE TABLE IF NOT EXISTS executing_unit_table (
        id          uuid          PRIMARY KEY,
        name        varchar(255)  NOT NULL,
        created_at  timestamp(6)  NOT NULL,
        updated_at  timestamp(6)  NOT NULL,
        created_by  varchar(100),
        updated_by  varchar(100),
        is_deleted  boolean       NOT NULL DEFAULT false
    );

    CREATE TABLE IF NOT EXISTS employee_table (
        id             uuid          PRIMARY KEY,
        name           varchar(255)  NOT NULL,
        identification varchar(100),
        created_at     timestamp(6)  NOT NULL,
        updated_at     timestamp(6)  NOT NULL,
        created_by     varchar(100),
        updated_by     varchar(100),
        is_deleted     boolean       NOT NULL DEFAULT false
    );

    -- ------------------------------------------------------------------------
    -- 2. Columnas FK en asset_table. Ambas nullable: son opcionales.
    -- ------------------------------------------------------------------------
    ALTER TABLE asset_table ADD COLUMN IF NOT EXISTS executing_unit_id uuid;
    ALTER TABLE asset_table ADD COLUMN IF NOT EXISTS employee_id       uuid;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_asset_table_executing_unit') THEN
        ALTER TABLE asset_table
            ADD CONSTRAINT fk_asset_table_executing_unit
            FOREIGN KEY (executing_unit_id) REFERENCES executing_unit_table (id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_asset_table_employee') THEN
        ALTER TABLE asset_table
            ADD CONSTRAINT fk_asset_table_employee
            FOREIGN KEY (employee_id) REFERENCES employee_table (id);
    END IF;

    -- ------------------------------------------------------------------------
    -- 3. Si las columnas de texto ya no existen, la migración ya se aplicó.
    -- ------------------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'asset_table' AND column_name = 'responsible_employee'
    ) THEN
        RAISE NOTICE 'Las columnas de texto ya no existen; la migracion ya se aplico. Sin cambios.';
        RETURN;
    END IF;

    -- ------------------------------------------------------------------------
    -- 4. Unidades ejecutoras: una por cada valor DISTINCT case-insensitive.
    --    Se conserva la primera grafia alfabetica como nombre canonico.
    -- ------------------------------------------------------------------------
    WITH distintas AS (
        SELECT lower(btrim(executing_unit)) AS name_key,
               min(btrim(executing_unit))   AS name
        FROM asset_table
        WHERE executing_unit IS NOT NULL AND btrim(executing_unit) <> ''
        GROUP BY 1
    )
    INSERT INTO executing_unit_table (id, name, created_at, updated_at, is_deleted)
    SELECT gen_random_uuid(), d.name, now(), now(), false
    FROM distintas d
    WHERE NOT EXISTS (
        SELECT 1 FROM executing_unit_table e WHERE lower(btrim(e.name)) = d.name_key
    );
    GET DIAGNOSTICS unidades_creadas = ROW_COUNT;
    RAISE NOTICE 'Unidades ejecutoras creadas: %', unidades_creadas;

    -- ------------------------------------------------------------------------
    -- 5. Funcionarios: uno por cada responsible_employee DISTINCT.
    --    5.1 identificacion_por_nombre: la identificacion no nula mas frecuente
    --        de cada nombre (desempate alfabetico, para que sea determinista).
    --    5.2 duenio_identificacion: si esa identificacion la reclaman varios
    --        nombres, se queda con ella el nombre con mas activos; los demas
    --        quedan con identificacion NULL (reportados en el PASO 0.B).
    -- ------------------------------------------------------------------------
    WITH nombres AS (
        SELECT lower(btrim(responsible_employee)) AS name_key,
               min(btrim(responsible_employee))   AS name,
               count(*)                           AS activos
        FROM asset_table
        WHERE responsible_employee IS NOT NULL AND btrim(responsible_employee) <> ''
        GROUP BY 1
    ),
    pares AS (
        SELECT lower(btrim(responsible_employee)) AS name_key,
               btrim(responsible_employee_id)     AS identification,
               count(*)                           AS repeticiones
        FROM asset_table
        WHERE responsible_employee    IS NOT NULL AND btrim(responsible_employee)    <> ''
          AND responsible_employee_id IS NOT NULL AND btrim(responsible_employee_id) <> ''
        GROUP BY 1, 2
    ),
    identificacion_por_nombre AS (
        SELECT name_key, identification, repeticiones
        FROM (
            SELECT p.*,
                   row_number() OVER (PARTITION BY name_key
                                      ORDER BY repeticiones DESC, identification) AS rn
            FROM pares p
        ) ranked
        WHERE rn = 1
    ),
    duenio_identificacion AS (
        SELECT name_key,
               identification,
               row_number() OVER (PARTITION BY lower(identification)
                                  ORDER BY repeticiones DESC, name_key) AS rn
        FROM identificacion_por_nombre
    )
    INSERT INTO employee_table (id, name, identification, created_at, updated_at, is_deleted)
    SELECT gen_random_uuid(),
           n.name,
           CASE WHEN d.rn = 1 THEN d.identification ELSE NULL END,
           now(), now(), false
    FROM nombres n
    LEFT JOIN duenio_identificacion d ON d.name_key = n.name_key
    WHERE NOT EXISTS (
        SELECT 1 FROM employee_table e WHERE lower(btrim(e.name)) = n.name_key
    );
    GET DIAGNOSTICS funcionarios_creados = ROW_COUNT;
    RAISE NOTICE 'Funcionarios creados: %', funcionarios_creados;

    -- ------------------------------------------------------------------------
    -- 6. Enlazar los activos por FK, comparando por nombre case-insensitive.
    -- ------------------------------------------------------------------------
    UPDATE asset_table a
    SET executing_unit_id = e.id
    FROM executing_unit_table e
    WHERE a.executing_unit_id IS NULL
      AND a.executing_unit IS NOT NULL AND btrim(a.executing_unit) <> ''
      AND lower(btrim(e.name)) = lower(btrim(a.executing_unit));
    GET DIAGNOSTICS activos_unidad = ROW_COUNT;
    RAISE NOTICE 'Activos enlazados a unidad ejecutora: %', activos_unidad;

    UPDATE asset_table a
    SET employee_id = e.id
    FROM employee_table e
    WHERE a.employee_id IS NULL
      AND a.responsible_employee IS NOT NULL AND btrim(a.responsible_employee) <> ''
      AND lower(btrim(e.name)) = lower(btrim(a.responsible_employee));
    GET DIAGNOSTICS activos_funcionario = ROW_COUNT;
    RAISE NOTICE 'Activos enlazados a funcionario: %', activos_funcionario;

    SELECT count(*) INTO huerfanos
    FROM asset_table
    WHERE (responsible_employee IS NULL OR btrim(responsible_employee) = '')
      AND responsible_employee_id IS NOT NULL AND btrim(responsible_employee_id) <> '';
    IF huerfanos > 0 THEN
        RAISE NOTICE 'Activos con identificacion pero sin nombre (quedan sin funcionario): %. Ver PASO 0.C.', huerfanos;
    END IF;

    -- ------------------------------------------------------------------------
    -- 7. Guarda destructiva: cortar si algun activo con texto no vacio quedo
    --    sin enlazar. No se elimina ninguna columna mientras haya datos que se
    --    perderian.
    -- ------------------------------------------------------------------------
    SELECT count(*) INTO sin_enlazar
    FROM asset_table
    WHERE (executing_unit IS NOT NULL AND btrim(executing_unit) <> '' AND executing_unit_id IS NULL)
       OR (responsible_employee IS NOT NULL AND btrim(responsible_employee) <> '' AND employee_id IS NULL);
    IF sin_enlazar > 0 THEN
        RAISE EXCEPTION
            'Quedan % activos con unidad ejecutora o funcionario en texto sin enlazar por FK. No se eliminan las columnas.',
            sin_enlazar;
    END IF;

    -- ------------------------------------------------------------------------
    -- 8. Indices unicos funcionales, case-insensitive y parciales.
    --    Parciales sobre is_deleted = false para que el borrado logico libere
    --    el nombre; el UNIQUE plano de V1 contaba tambien las filas borradas.
    -- ------------------------------------------------------------------------
    CREATE UNIQUE INDEX IF NOT EXISTS uk_executing_unit_table_name_lower
        ON executing_unit_table (lower(btrim(name)))
        WHERE is_deleted = false;

    CREATE UNIQUE INDEX IF NOT EXISTS uk_employee_table_name_lower
        ON employee_table (lower(btrim(name)))
        WHERE is_deleted = false;

    CREATE UNIQUE INDEX IF NOT EXISTS uk_employee_table_identification_lower
        ON employee_table (lower(btrim(identification)))
        WHERE is_deleted = false AND identification IS NOT NULL;

    -- Indices de apoyo para los joins que genera GenericSpecifications.
    CREATE INDEX IF NOT EXISTS ix_asset_table_executing_unit_id ON asset_table (executing_unit_id);
    CREATE INDEX IF NOT EXISTS ix_asset_table_employee_id       ON asset_table (employee_id);

    -- ------------------------------------------------------------------------
    -- 9. Eliminar las columnas de texto. Hibernate nunca lo hace por su cuenta.
    -- ------------------------------------------------------------------------
    ALTER TABLE asset_table DROP COLUMN IF EXISTS executing_unit;
    ALTER TABLE asset_table DROP COLUMN IF EXISTS responsible_employee;
    ALTER TABLE asset_table DROP COLUMN IF EXISTS responsible_employee_id;
    RAISE NOTICE 'Columnas de texto eliminadas de asset_table.';

END $$;

COMMIT;

-- ============================================================================
-- VERIFICACIÓN (post-migración, solo lectura)
-- ============================================================================

-- Esperado: 0 filas (las tres columnas de texto ya no existen).
SELECT column_name
FROM information_schema.columns
WHERE table_name = 'asset_table'
  AND column_name IN ('executing_unit', 'responsible_employee', 'responsible_employee_id');

-- Esperado: executing_unit_id y employee_id, ambos con is_nullable = YES.
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'asset_table'
  AND column_name IN ('executing_unit_id', 'employee_id')
ORDER BY column_name;

-- Esperado: los tres indices unicos funcionales.
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename IN ('executing_unit_table', 'employee_table')
ORDER BY tablename, indexname;

-- Esperado: 0 filas (ningun funcionario duplicado por nombre).
SELECT lower(btrim(name)) AS name_key, count(*) AS total
FROM employee_table
WHERE is_deleted = false
GROUP BY 1
HAVING count(*) > 1;

-- Esperado: 0 filas (ninguna identificacion repetida).
SELECT lower(btrim(identification)) AS ident_key, count(*) AS total
FROM employee_table
WHERE is_deleted = false AND identification IS NOT NULL
GROUP BY 1
HAVING count(*) > 1;

-- Conteos finales.
SELECT (SELECT count(*) FROM executing_unit_table)                            AS unidades,
       (SELECT count(*) FROM employee_table)                                  AS funcionarios,
       (SELECT count(*) FROM employee_table WHERE identification IS NOT NULL) AS con_identificacion,
       (SELECT count(*) FROM asset_table WHERE executing_unit_id IS NOT NULL) AS activos_con_unidad,
       (SELECT count(*) FROM asset_table WHERE employee_id IS NOT NULL)       AS activos_con_funcionario;

-- ============================================================================
-- ROLLBACK (ejecutar manualmente solo si se necesita revertir)
-- ATENCIÓN: recrea las columnas y las repuebla desde las FK. No restaura el
-- estado original exacto: los activos recuperan la identificación canónica del
-- funcionario, no la que tenían escrita si divergía (ver PASO 0.A y 0.B).
-- Tampoco recupera a los activos huérfanos del PASO 0.C.
-- ============================================================================
-- BEGIN;
-- ALTER TABLE asset_table ADD COLUMN IF NOT EXISTS executing_unit          varchar(255);
-- ALTER TABLE asset_table ADD COLUMN IF NOT EXISTS responsible_employee    varchar(255);
-- ALTER TABLE asset_table ADD COLUMN IF NOT EXISTS responsible_employee_id varchar(100);
-- UPDATE asset_table a SET executing_unit = e.name
--   FROM executing_unit_table e WHERE a.executing_unit_id = e.id;
-- UPDATE asset_table a SET responsible_employee = e.name, responsible_employee_id = e.identification
--   FROM employee_table e WHERE a.employee_id = e.id;
-- ALTER TABLE asset_table DROP CONSTRAINT IF EXISTS fk_asset_table_executing_unit;
-- ALTER TABLE asset_table DROP CONSTRAINT IF EXISTS fk_asset_table_employee;
-- ALTER TABLE asset_table DROP COLUMN IF EXISTS executing_unit_id;
-- ALTER TABLE asset_table DROP COLUMN IF EXISTS employee_id;
-- COMMIT;
