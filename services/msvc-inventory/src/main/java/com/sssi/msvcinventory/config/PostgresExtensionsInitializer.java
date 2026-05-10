package com.sssi.msvcinventory.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresExtensionsInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS unaccent");
            log.info("PostgreSQL 'unaccent' extension verificada/creada correctamente");
        } catch (DataAccessException e) {
            log.error("No se pudo crear la extensión 'unaccent'. " +
                    "Las búsquedas accent-insensitive no funcionarán. " +
                    "Solicita al DBA ejecutar: CREATE EXTENSION unaccent; en la base de datos del inventario.", e);
        }
    }
}
