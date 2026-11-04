package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.config.TransportSchemaMigration;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftCreateRequestDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowRequestDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowDayRequestDto;
import com.proseg.msvc_transport.dto.request.BorrowedVehicleRequestDto;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftImportRow;
import com.proseg.msvc_transport.entity.VehicleType;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.security.CurrentUserResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "spring.jpa.show-sql=false",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.default_batch_fetch_size=50",
        "logging.level.org.hibernate.stat=OFF",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
@EnabledIfEnvironmentVariable(named = "TRANSPORT_TEST_DB_URL", matches = ".*codex_transport_phase2.*")
@Transactional
class TransportPersistenceTest {
    @Autowired TourProcessingDraftService drafts;
    @Autowired TourProcessingService registration;
    @Autowired OvertimeRequestReportService reports;
    @Autowired TourProcessingDraftRepository draftRepository;
    @Autowired DriverRepository driverRepository;
    @Autowired DriverService driverService;
    @Autowired BorrowedVehicleService borrowedVehicles;
    @Autowired DriverMonthService months;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransportSchemaMigration migration;
    @MockBean CurrentUserResolver currentUserResolver;
    @MockBean JwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("TRANSPORT_TEST_DB_URL"));
        properties.add("spring.datasource.username", () -> System.getenv("TRANSPORT_TEST_DB_USER"));
        properties.add("spring.datasource.password", () -> System.getenv("TRANSPORT_TEST_DB_PASSWORD"));
    }

    @Test
    void rerunningMigrationsKeepsEveryDay() {
        when(currentUserResolver.resolve()).thenReturn("test");
        Driver driver = driverRepository.save(Driver.builder().name("Test " + UUID.randomUUID()).status("ACTIVE").build());
        var row = TourProcessingDraftRowRequestDto.builder().rowNumber(1).number("MIGRATION-1")
                .driver(driver.getName()).passengers(3).executingUnit("Unidad").responsible("Responsable")
                .destination("Heredia").durationDays(1).priority(1).departureDate(LocalDate.of(2094, 5, 4))
                .returnDate(LocalDate.of(2094, 5, 4)).departureTime(LocalTime.of(8, 0))
                .returnTime(LocalTime.of(16, 0)).build();
        drafts.create(TourProcessingDraftCreateRequestDto.builder().fileName("migration.xlsx").fileType("XLSX")
                .year(2094).month(5).businessDays(21).rows(List.of(row)).build());
        entityManager.flush();
        long count = jdbc.queryForObject("select count(*) from tour_processing_draft_day_table", Long.class);
        assertThat(count).isPositive();
        migration.afterSingletonsInstantiated();
        assertThat(jdbc.queryForObject("select count(*) from tour_processing_draft_day_table", Long.class)).isEqualTo(count);
    }

    @Test
    void dropsOnlyEmptyLegacyTables() {
        jdbc.execute("drop table if exists assignment_table, tour_table, vehicle_maintenance_table");
        jdbc.execute("create table tour_table (id uuid primary key)");
        jdbc.execute("create table vehicle_maintenance_table (id uuid primary key)");
        jdbc.update("insert into tour_table values (?)", UUID.randomUUID());

        migration.afterSingletonsInstantiated();

        assertThat(jdbc.queryForObject("select to_regclass('tour_table')::text", String.class)).isNotNull();
        assertThat(jdbc.queryForObject("select to_regclass('vehicle_maintenance_table')::text", String.class)).isNull();

        jdbc.update("delete from tour_table");
        migration.afterSingletonsInstantiated();

        assertThat(jdbc.queryForObject("select to_regclass('tour_table')::text", String.class)).isNull();
    }

    @Test
    void readsRealPagesSummariesAndDatesWithoutQueriesPerRow() {
        UUID id = jdbc.queryForObject("select id from tour_processing_draft_table where not is_deleted order by imported_at desc limit 1", UUID.class);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        entityManager.clear();
        statistics.clear();
        var page = drafts.findRows(id, null, Map.of(), PageRequest.of(0, 50));
        assertThat(page.getContent()).isNotEmpty();
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(8);
        var summaries = drafts.getDriverScheduleSummaries(id);
        var draft = draftRepository.findById(id).orElseThrow();
        var monthly = months.findAllForPeriod(draft.getYear(), draft.getMonth());
        assertThat(monthly).isNotEmpty();
        var date = page.getContent().get(0).getDepartureDate();
        var daily = drafts.getSchedule(id, date);
        for (var summary : summaries.getDrivers()) {
            daily.getDrivers().stream().filter(driver -> driver.getId().equals(summary.getId())).findFirst().ifPresent(driver -> {
                assertThat(driver.getSurplusHours()).isEqualTo(summary.getSurplusHours());
                assertThat(driver.getOvertimeHours()).isEqualTo(summary.getOvertimeHours());
            });
        }
        assertThat(daily.getDrivers()).allSatisfy(driver -> assertThat(driver.getTours())
                .allSatisfy(tour -> assertThat(tour.getDays()).allSatisfy(day -> assertThat(day.getDate()).isEqualTo(date))));
        summaries.getDrivers().stream().findFirst().ifPresent(driver -> {
            var driverPage = drafts.getDriverSchedule(id, driver.getId(), PageRequest.of(0, 2));
            assertThat(driverPage.getContent()).hasSizeLessThanOrEqualTo(2);
        });
    }

    @Test
    void keepsDuplicateImportsSeparateAndPromotesAnEditedDuplicateToATour() {
        when(currentUserResolver.resolve()).thenReturn("test");
        var first = TourProcessingDraftRowRequestDto.builder().rowNumber(1).number("RAW-1")
                .passengers(3).executingUnit("Unidad").responsible("Responsable").destination("Heredia")
                .durationDays(1).priority(1).departureDate(LocalDate.of(2098, 5, 4)).returnDate(LocalDate.of(2098, 5, 4))
                .departureTime(LocalTime.of(8, 0)).returnTime(LocalTime.of(16, 0)).build();
        var second = new TourProcessingDraftRowRequestDto();
        org.springframework.beans.BeanUtils.copyProperties(first, second);
        second.setRowNumber(2);
        var created = drafts.create(TourProcessingDraftCreateRequestDto.builder().fileName("duplicates.xlsx").fileType("XLSX")
                .year(2098).month(5).businessDays(21).rows(List.of(first, second)).build());
        entityManager.flush();
        entityManager.clear();
        assertThat(jdbc.queryForObject("select count(*) from tour_processing_draft_tour_table where draft_id = ?", Long.class, created.getDraftId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from tour_processing_draft_import_row_table where draft_id = ?", Long.class, created.getDraftId())).isEqualTo(1);
        var rows = drafts.findRows(created.getDraftId(), null, Map.of(), PageRequest.of(0, 10));
        assertThat(rows.getContent()).hasSize(2);
        var duplicate = rows.getContent().stream().filter(row -> row.getDuplicateRank() == 2).findFirst().orElseThrow();
        second.setNumber("RAW-2");
        drafts.updateRow(created.getDraftId(), duplicate.getId(), second);
        entityManager.flush();
        entityManager.clear();
        assertThat(jdbc.queryForObject("select count(*) from tour_processing_draft_tour_table where draft_id = ?", Long.class, created.getDraftId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from tour_processing_draft_import_row_table where draft_id = ?", Long.class, created.getDraftId())).isZero();
        second.setNumber("RAW-1");
        drafts.updateRow(created.getDraftId(), duplicate.getId(), second);
        entityManager.flush();
        entityManager.clear();
        assertThat(drafts.deleteDuplicateRows(created.getDraftId())).isEqualTo(1);
        assertThat(drafts.findRows(created.getDraftId(), null, Map.of(), PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void doesNotUpdateUnchangedDuplicateImportsWhenEditingAnotherTour() {
        when(currentUserResolver.resolve()).thenReturn("test");
        var unique = TourProcessingDraftRowRequestDto.builder().rowNumber(1).number("UNIQUE-1")
                .passengers(3).executingUnit("Unidad").responsible("Responsable").destination("Heredia")
                .durationDays(1).priority(1).departureDate(LocalDate.of(2098, 5, 4)).returnDate(LocalDate.of(2098, 5, 4))
                .departureTime(LocalTime.of(8, 0)).returnTime(LocalTime.of(16, 0)).build();
        var rows = new ArrayList<TourProcessingDraftRowRequestDto>();
        rows.add(unique);
        for (int rowNumber = 2; rowNumber <= 11; rowNumber++) {
            var duplicate = new TourProcessingDraftRowRequestDto();
            org.springframework.beans.BeanUtils.copyProperties(unique, duplicate);
            duplicate.setRowNumber(rowNumber);
            duplicate.setNumber("DUP-1");
            rows.add(duplicate);
        }
        var created = drafts.create(TourProcessingDraftCreateRequestDto.builder().fileName("duplicate-updates.xlsx")
                .fileType("XLSX").year(2098).month(5).businessDays(21).rows(rows).build());
        entityManager.flush();
        entityManager.clear();
        assertThat(jdbc.queryForObject("select count(*) from tour_processing_draft_import_row_table where draft_id = ?", Long.class,
                created.getDraftId())).isEqualTo(9);
        var uniqueId = drafts.findRows(created.getDraftId(), null, Map.of(), PageRequest.of(0, 20)).getContent().stream()
                .filter(row -> "UNIQUE-1".equals(row.getNumber())).findFirst().orElseThrow().getId();
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        unique.setObservations("Medición");
        drafts.updateRow(created.getDraftId(), uniqueId, unique);
        entityManager.flush();
        assertThat(statistics.getEntityStatistics(TourProcessingDraftImportRow.class.getName()).getUpdateCount()).isZero();
    }

    @Test
    void closedDraftKeepsDriverTotalsAndBlocksAnotherDraftForTheMonth() {
        when(currentUserResolver.resolve()).thenReturn("test");
        var driver = driverRepository.findAll().stream().filter(value -> "ACTIVE".equals(value.getStatus())).findFirst().orElseThrow();
        var row = TourProcessingDraftRowRequestDto.builder().rowNumber(1).number("CLOSE-1")
                .driver(driver.getName()).passengers(3).executingUnit("Unidad").responsible("Responsable")
                .destination("Heredia").durationDays(1).priority(1).departureDate(LocalDate.of(2099, 5, 4))
                .returnDate(LocalDate.of(2099, 5, 4)).departureTime(LocalTime.of(8, 0))
                .returnTime(LocalTime.of(16, 0)).build();
        var request = TourProcessingDraftCreateRequestDto.builder().fileName("closed-month.xlsx").fileType("XLSX")
                .year(2099).month(5).businessDays(21).rows(List.of(row)).build();
        var created = drafts.create(request);
        var before = months.findAllForPeriod(2099, 5).stream()
                .filter(value -> driver.getId().equals(value.getDriverId())).findFirst().orElseThrow();
        assertThat(before.getOrdinaryHoursAccrued()).isPositive();

        assertThat(drafts.finish(created.getDraftId()).getStatus()).isEqualTo(TourProcessingDraftStatus.CLOSED);
        assertThat(drafts.findAll(null, Map.of("status", "CLOSED"), PageRequest.of(0, 20)).getContent())
                .anySatisfy(value -> assertThat(value.getId()).isEqualTo(created.getDraftId()));
        assertThat(drafts.findAll(null, Map.of("status", "DRAFT"), PageRequest.of(0, 20)).getContent())
                .noneSatisfy(value -> assertThat(value.getId()).isEqualTo(created.getDraftId()));
        var closed = months.findAllForPeriod(2099, 5).stream()
                .filter(value -> driver.getId().equals(value.getDriverId())).findFirst().orElseThrow();
        assertThat(closed.getOrdinaryHoursAccrued()).isEqualTo(before.getOrdinaryHoursAccrued());
        assertThatThrownBy(() -> drafts.create(request)).hasMessageContaining("ya fue terminada");
        assertThat(drafts.reopen(created.getDraftId()).getStatus()).isEqualTo(TourProcessingDraftStatus.DRAFT);
    }

    @Test
    void protectsBorrowedVehiclesInOpenDraftsAndKeepsThemReadableAfterRetiring() {
        when(currentUserResolver.resolve()).thenReturn("test");
        var saved = borrowedVehicles.create(BorrowedVehicleRequestDto.builder().plate("SAVED-" + UUID.randomUUID().toString().substring(0, 8))
                .capacity(10).type(VehicleType.MICROBUS).build());
        var imported = borrowedVehicles.create(BorrowedVehicleRequestDto.builder().plate("RAW-" + UUID.randomUUID().toString().substring(0, 8))
                .capacity(10).type(VehicleType.MICROBUS).build());
        var free = borrowedVehicles.create(BorrowedVehicleRequestDto.builder().plate("FREE-" + UUID.randomUUID().toString().substring(0, 8))
                .capacity(10).type(VehicleType.MICROBUS).build());
        var first = TourProcessingDraftRowRequestDto.builder().rowNumber(1).number("BORROWED-1")
                .resourceType(ResourceType.BORROWED_VEHICLE).borrowedVehicleId(saved.getId()).passengers(3)
                .executingUnit("Unidad").responsible("Responsable").destination("Heredia").durationDays(1).priority(1)
                .departureDate(LocalDate.of(2095, 5, 4)).returnDate(LocalDate.of(2095, 5, 4))
                .departureTime(LocalTime.of(8, 0)).returnTime(LocalTime.of(16, 0)).build();
        var second = new TourProcessingDraftRowRequestDto();
        org.springframework.beans.BeanUtils.copyProperties(first, second);
        second.setRowNumber(2);
        second.setBorrowedVehicleId(imported.getId());
        var created = drafts.create(TourProcessingDraftCreateRequestDto.builder().fileName("borrowed.xlsx").fileType("XLSX")
                .year(2095).month(5).businessDays(21).rows(List.of(first, second)).build());
        entityManager.flush();

        assertThatThrownBy(() -> borrowedVehicles.delete(saved.getId())).hasMessageContaining("borrador abierto");
        assertThatThrownBy(() -> borrowedVehicles.delete(imported.getId())).hasMessageContaining("borrador abierto");
        borrowedVehicles.delete(free.getId());

        drafts.finish(created.getDraftId());
        borrowedVehicles.delete(saved.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(drafts.findRows(created.getDraftId(), null, Map.of(), PageRequest.of(0, 10)).getContent())
                .anySatisfy(row -> assertThat(row.getBorrowedVehiclePlate()).isEqualTo(saved.getPlate()));
        assertThat(borrowedVehicles.findAll(null, Map.of(), PageRequest.of(0, 100)).getContent())
                .noneSatisfy(vehicle -> assertThat(vehicle.getId()).isEqualTo(saved.getId()));

        var restored = borrowedVehicles.create(BorrowedVehicleRequestDto.builder().plate(saved.getPlate())
                .capacity(12).type(VehicleType.MICROBUS).build());
        assertThat(restored.getId()).isEqualTo(saved.getId());
        assertThat(restored.getCapacity()).isEqualTo(12);
    }

    @Test
    void filtersApprovalStatusAcrossSavedAndImportedRows() {
        when(currentUserResolver.resolve()).thenReturn("test");
        var approved = TourProcessingDraftRowRequestDto.builder().rowNumber(1).number("FILTER-1")
                .approvalStatus(TourApprovalStatus.APPROVED).passengers(3).executingUnit("Unidad")
                .responsible("Responsable").destination("Heredia").durationDays(1).priority(1)
                .departureDate(LocalDate.of(2098, 5, 4)).returnDate(LocalDate.of(2098, 5, 4))
                .departureTime(LocalTime.of(8, 0)).returnTime(LocalTime.of(16, 0)).build();
        var notApproved = new TourProcessingDraftRowRequestDto();
        org.springframework.beans.BeanUtils.copyProperties(approved, notApproved);
        notApproved.setRowNumber(2);
        notApproved.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        var created = drafts.create(TourProcessingDraftCreateRequestDto.builder().fileName("approval-filter.xlsx")
                .fileType("XLSX").year(2098).month(5).businessDays(21)
                .rows(List.of(approved, notApproved)).build());
        entityManager.flush();
        entityManager.clear();
        assertThat(jdbc.queryForObject("select count(*) from tour_processing_draft_tour_table where draft_id = ?", Long.class, created.getDraftId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from tour_processing_draft_import_row_table where draft_id = ?", Long.class, created.getDraftId())).isEqualTo(1);

        var approvedRows = drafts.findRows(created.getDraftId(), null,
                Map.of("approvalStatus", "APPROVED"), PageRequest.of(0, 10));
        var notApprovedRows = drafts.findRows(created.getDraftId(), null,
                Map.of("approvalStatus", "NOT_APPROVED"), PageRequest.of(0, 10));

        assertThat(approvedRows.getTotalElements()).isEqualTo(1);
        assertThat(approvedRows.getContent()).allSatisfy(row -> assertThat(row.getApprovalStatus()).isEqualTo(TourApprovalStatus.APPROVED));
        assertThat(notApprovedRows.getTotalElements()).isEqualTo(1);
        assertThat(notApprovedRows.getContent()).allSatisfy(row -> assertThat(row.getApprovalStatus()).isEqualTo(TourApprovalStatus.NOT_APPROVED));
    }

    @Test
    void linksNewCatalogDriversAndKeepsRenamesAndDeletionConsistent() {
        when(currentUserResolver.resolve()).thenReturn("test");
        String name = "Chofer nuevo " + UUID.randomUUID();
        var row = TourProcessingDraftRowRequestDto.builder().rowNumber(1).number("CAT-1")
                .driver(name).passengers(3).executingUnit("Unidad").responsible("Responsable").destination("Heredia")
                .durationDays(1).priority(1).departureDate(LocalDate.of(2097, 5, 6)).returnDate(LocalDate.of(2097, 5, 6))
                .departureTime(LocalTime.of(8, 0)).returnTime(LocalTime.of(16, 0)).build();
        var imported = drafts.create(TourProcessingDraftCreateRequestDto.builder().fileName("catalog.xlsx").fileType("XLSX")
                .year(2097).month(5).businessDays(21).rows(List.of(row)).build());
        entityManager.flush();
        entityManager.clear();
        var request = com.proseg.msvc_transport.dto.request.DriverRequestDto.builder().name(name).status("ACTIVE").build();
        var driver = driverService.create(request);
        entityManager.flush();
        entityManager.clear();
        var saved = drafts.findRows(imported.getDraftId(), null, Map.of(), PageRequest.of(0, 10)).getContent().get(0);
        assertThat(saved.getDays()).hasSize(1);
        request.setName(name + " actualizado");
        driverService.update(driver.getId(), request);
        entityManager.flush();
        entityManager.clear();
        assertThat(drafts.findRow(imported.getDraftId(), saved.getId()).getDriver()).isEqualTo(request.getName());
        driverService.delete(driver.getId());
        entityManager.flush();
        entityManager.clear();
        assertThat(drafts.findRow(imported.getDraftId(), saved.getId()).getDays()).isEmpty();
    }

    @Test
    void savesEditsRecalculatesOnlyActiveDaysAndRegistersWithoutCopyingTours() {
        when(currentUserResolver.resolve()).thenReturn("test");
        Driver driver = driverRepository.save(Driver.builder().name("Test " + UUID.randomUUID()).build());
        var row = TourProcessingDraftRowRequestDto.builder().rowNumber(1).number("TEST-1")
                .driver(driver.getName()).passengers(3).executingUnit("Escuela de Química")
                .responsible("Responsable").destination("Heredia").durationDays(1).priority(1)
                .departureDate(LocalDate.of(2099, 5, 4)).returnDate(LocalDate.of(2099, 5, 4))
                .departureTime(LocalTime.of(7, 30)).returnTime(LocalTime.of(19, 30)).build();
        var created = drafts.create(TourProcessingDraftCreateRequestDto.builder().fileName("test.xlsx").fileType("XLSX")
                .year(2099).month(5).businessDays(21).rows(List.of(row)).build());
        entityManager.flush();
        entityManager.clear();
        var saved = drafts.findRows(created.getDraftId(), null, Map.of(), PageRequest.of(0, 10)).getContent().get(0);
        assertThat(saved.getDays()).hasSize(1);
        var day = saved.getDays().get(0);
        drafts.updateRowDay(created.getDraftId(), saved.getId(), day.getId(), TourProcessingDraftRowDayRequestDto.builder()
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(16, 0))
                .ordinaryHours(8D).surplusHours(0D).overtimeHours(0D).build());
        entityManager.flush();
        entityManager.clear();
        assertThat(drafts.findRow(created.getDraftId(), saved.getId()).getDays().get(0).isManuallyEdited()).isTrue();
        drafts.updateRowApprovalStatus(created.getDraftId(), saved.getId(), TourApprovalStatus.NOT_APPROVED);
        entityManager.flush();
        assertThat(drafts.findRow(created.getDraftId(), saved.getId()).getDays()).isEmpty();
        drafts.updateRowApprovalStatus(created.getDraftId(), saved.getId(), TourApprovalStatus.APPROVED);
        var registered = registration.registerDraft(created.getDraftId(), false);
        entityManager.flush();
        assertThat(registered.getImportedRows()).isEqualTo(1);
        assertThat(registered.getTourProcessingExecutionId()).isNotNull();
    }
}
