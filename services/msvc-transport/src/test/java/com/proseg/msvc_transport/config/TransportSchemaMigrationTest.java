package com.proseg.msvc_transport.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransportSchemaMigrationTest {

    private JdbcTemplate jdbc;
    private TransportSchemaMigration migration;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(jdbc.queryForObject(startsWith("select exists(select 1 from transport_schema_migration"), eq(Boolean.class), any(Object.class)))
                .thenReturn(true);
        migration = new TransportSchemaMigration(jdbc, transactionManager);
    }

    private void tableExists(String table) {
        when(jdbc.queryForObject("select to_regclass(?)::text", String.class, table)).thenReturn(table);
    }

    @Test
    void dropsEmptyLegacyTablesStartingWithTheDependentOne() {
        tableExists("assignment_table");
        tableExists("tour_table");
        when(jdbc.queryForObject("select exists(select 1 from assignment_table)", Boolean.class)).thenReturn(false);
        when(jdbc.queryForObject("select exists(select 1 from tour_table)", Boolean.class)).thenReturn(false);

        migration.afterSingletonsInstantiated();

        InOrder order = inOrder(jdbc);
        order.verify(jdbc).execute("drop table assignment_table");
        order.verify(jdbc).execute("drop table tour_table");
    }

    @Test
    void dropsExecutionDetailsBeforeTheirExecutions() {
        tableExists("cleaning_execution_detail_table");
        tableExists("cleaning_execution_table");
        tableExists("tour_processing_execution_detail_table");
        tableExists("tour_processing_execution_table");
        when(jdbc.queryForObject("select exists(select 1 from cleaning_execution_detail_table)", Boolean.class)).thenReturn(false);
        when(jdbc.queryForObject("select exists(select 1 from cleaning_execution_table)", Boolean.class)).thenReturn(false);
        when(jdbc.queryForObject("select exists(select 1 from tour_processing_execution_detail_table)", Boolean.class)).thenReturn(false);
        when(jdbc.queryForObject("select exists(select 1 from tour_processing_execution_table)", Boolean.class)).thenReturn(false);

        migration.afterSingletonsInstantiated();

        InOrder order = inOrder(jdbc);
        order.verify(jdbc).execute("drop table cleaning_execution_detail_table");
        order.verify(jdbc).execute("drop table cleaning_execution_table");
        order.verify(jdbc).execute("drop table tour_processing_execution_detail_table");
        order.verify(jdbc).execute("drop table tour_processing_execution_table");
    }

    @Test
    void keepsLegacyTablesWithData() {
        tableExists("tour_table");
        when(jdbc.queryForObject("select exists(select 1 from tour_table)", Boolean.class)).thenReturn(true);

        migration.afterSingletonsInstantiated();

        verify(jdbc, never()).execute("drop table tour_table");
    }

    @Test
    void ignoresLegacyTablesThatDoNotExist() {
        migration.afterSingletonsInstantiated();

        verify(jdbc, never()).execute(startsWith("drop table"));
    }
}
