INSERT INTO external_vehicle_table (id, created_at, updated_at, is_deleted, provider, plate, capacity, available)
SELECT gen_random_uuid(), now(), now(), false, seed.provider, seed.plate, seed.capacity, seed.available
FROM (VALUES
    ('Globa Tours - José Vargas', 'HB4612', 13, true),
    ('Globa Tours - José Vargas', 'HB4619', 13, true),
    ('Globa Tours - José Vargas', 'HB4677', 13, true),
    ('Globa Tours - José Vargas', 'HB4601', 13, true),
    ('Globa Tours - José Vargas', 'SJB16886', 16, true),
    ('Globa Tours - José Vargas', 'HB5157', 16, true),
    ('Globa Tours - José Vargas', 'HB4877', 33, true),
    ('Globa Tours - José Vargas', 'HB4694', 33, true),
    ('Globa Tours - José Vargas', 'HB4393', 33, true),
    ('Globa Tours - José Vargas', 'HB4353', 29, true),
    ('Globa Tours - José Vargas', 'HB5024', 33, true),
    ('Globa Tours - José Vargas', 'HB4891', 33, true),
    ('Globa Tours - José Vargas', 'HB4250', 28, true),
    ('Globa Tours - José Vargas', 'HB4939', 33, true),
    ('Transvanu - Hernan Vargas', 'SJB17742', 14, false),
    ('Transvanu - Hernan Vargas', 'SJB18346', 14, false),
    ('Transvanu - Hernan Vargas', 'GB3177', 33, false),
    ('Transvanu - Hernan Vargas', 'HB4085', 33, true),
    ('Transvanu - Hernan Vargas', 'SJB16318', 33, true),
    ('Anca', 'HB4380', 13, true),
    ('Anca', 'SJB16107', 15, true),
    ('Anca', 'SBJ16442', 15, true),
    ('Anca', 'HB4748', 33, true),
    ('Anca', 'HB4392', 33, true)
) AS seed(provider, plate, capacity, available)
WHERE NOT EXISTS (
    SELECT 1 FROM external_vehicle_table existing WHERE upper(existing.plate) = upper(seed.plate)
);
