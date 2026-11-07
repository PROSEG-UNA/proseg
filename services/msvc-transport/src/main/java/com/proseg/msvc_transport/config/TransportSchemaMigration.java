package com.proseg.msvc_transport.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@DependsOn("entityManagerFactory")
@RequiredArgsConstructor
@Slf4j
public class TransportSchemaMigration implements SmartInitializingSingleton {
    private static final List<String> LEGACY_TABLES = List.of(
            "assignment_table",
            "tour_table",
            "vehicle_maintenance_table",
            "cleaning_execution_detail_table",
            "cleaning_execution_table",
            "tour_processing_execution_detail_table",
            "tour_processing_execution_table"
    );

    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void afterSingletonsInstantiated() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.execute("select pg_advisory_xact_lock(8675022026)");
            jdbc.execute("create table if not exists transport_schema_migration (version integer primary key, applied_at timestamp not null default current_timestamp)");
            apply(2, "db/transport-phase2.sql");
            apply(3, "db/transport-import-rows.sql");
            apply(5, "db/transport-closed-draft.sql");
            apply(6, "db/transport-driver-ordinary-schedules.sql");
            LEGACY_TABLES.forEach(this::dropIfEmpty);
        });
    }
    private void dropIfEmpty(String table) {
        if (jdbc.queryForObject("select to_regclass(?)::text", String.class, table) == null) return;
        if (Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from " + table + ")", Boolean.class))) {
            log.warn("No se eliminó la tabla {} porque tiene datos", table);
            return;
        }
        jdbc.execute("drop table " + table);
    }

    private void apply(int version, String resource) {
        if (Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from transport_schema_migration where version = ?)", Boolean.class, version))) return;
        try {
            jdbc.execute(new ClassPathResource(resource).getContentAsString(StandardCharsets.UTF_8));
            jdbc.update("insert into transport_schema_migration(version) values (?)", version);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("No se pudo leer la migración de transporte", ex);
        }
    }
}
