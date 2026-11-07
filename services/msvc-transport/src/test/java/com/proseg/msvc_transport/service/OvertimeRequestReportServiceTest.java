package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.BorrowedVehicle;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.DriverMonthBusinessDaysRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftAssignmentRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OvertimeRequestReportServiceTest {

    private static final UUID DRAFT_ID = UUID.randomUUID();

    private TourProcessingDraftRepository draftRepository;
    private TourProcessingDraftAssignmentRepository rowRepository;
    private TourProcessingDraftDayRepository dayRepository;
    private DriverRepository driverRepository;
    private OvertimeRequestReportService service;

    @BeforeEach
    void setUp() {
        draftRepository = mock(TourProcessingDraftRepository.class);
        rowRepository = mock(TourProcessingDraftAssignmentRepository.class);
        dayRepository = mock(TourProcessingDraftDayRepository.class);
        driverRepository = mock(DriverRepository.class);
        service = new OvertimeRequestReportService(draftRepository, rowRepository, dayRepository, driverRepository,
                new DriverBusinessDaysService(mock(DriverMonthBusinessDaysRepository.class)));
    }

    @Test
    void buildsAlphabeticalActiveDriversWithTheirSurplusAndOvertimeDays() {
        stubDraft(28);
        when(driverRepository.findAll()).thenReturn(List.of(
                driver("Zacarías Mora", "ACTIVE"),
                driver("Álvaro Parada", "ACTIVE"),
                driver("Bruno Inactivo", "INACTIVE"),
                driver("Carla Inactiva Con Gira", "INACTIVE")
        ));
        TourProcessingDraftAssignment alvaroFirstTour = row("3622", "álvaro  parada", "2026-05-08");
        TourProcessingDraftAssignment alvaroSecondTour = row("3700", "Álvaro Parada", "2026-05-12");
        TourProcessingDraftAssignment carlaTour = row("3800", "Carla Inactiva Con Gira", "2026-05-13");
        when(rowRepository.findActiveByDraftId(DRAFT_ID)).thenReturn(List.of(
                alvaroFirstTour, alvaroSecondTour, carlaTour
        ));
        when(dayRepository.findByAssignmentIdIn(anyCollection())).thenReturn(List.of(
                day(alvaroFirstTour, "2026-05-08", "11:00", "20:30", 5.5, 4.0, 0.0),
                day(alvaroFirstTour, "2026-05-09", "07:30", "14:00", 0.0, 0.0, 6.5),
                day(alvaroSecondTour, "2026-05-12", "08:00", "15:00", 7.0, 0.0, 0.0),
                day(carlaTour, "2026-05-13", "06:30", "17:30", 9.0, 2.0, 0.0)
        ));

        OvertimeRequestReportDto report = service.build(2026, 5);

        assertThat(report.getBusinessDays()).isEqualTo(28);
        assertThat(report.getSurplusCapPerDriver()).isEqualTo(37.5);
        assertThat(report.getDrivers())
                .extracting(OvertimeRequestReportDto.DriverBlock::getName,
                        OvertimeRequestReportDto.DriverBlock::getSurplusHours,
                        OvertimeRequestReportDto.DriverBlock::getOvertimeHours)
                .containsExactly(
                        tuple("Álvaro Parada", 4.0, 6.5),
                        tuple("Carla Inactiva Con Gira", 2.0, 0.0),
                        tuple("Zacarías Mora", 0.0, 0.0)
                );

        OvertimeRequestReportDto.DriverBlock alvaro = report.getDrivers().get(0);
        assertThat(alvaro.getTours()).hasSize(1);
        assertThat(alvaro.getTours().get(0).getNumber()).isEqualTo("3622");
        assertThat(alvaro.getTours().get(0).getDays())
                .extracting(OvertimeRequestReportDto.TourDay::getDate,
                        OvertimeRequestReportDto.TourDay::getStart,
                        OvertimeRequestReportDto.TourDay::getEnd,
                        OvertimeRequestReportDto.TourDay::getSurplusHours,
                        OvertimeRequestReportDto.TourDay::getOvertimeHours)
                .containsExactly(
                        tuple(LocalDate.parse("2026-05-08"), 11.0, 20.5, 4.0, 0.0),
                        tuple(LocalDate.parse("2026-05-09"), 7.5, 14.0, 0.0, 6.5)
                );
        assertThat(report.getDrivers().get(2).getTours()).isEmpty();
    }

    @Test
    void buildScheduleLeavesOutNotApprovedTours() {
        TourProcessingDraft draft = TourProcessingDraft.builder().id(DRAFT_ID).year(2026).month(5).businessDays(28).build();
        when(driverRepository.findAll()).thenReturn(List.of(driver("Álvaro Parada", "ACTIVE")));
        TourProcessingDraftAssignment approvedTour = row("3622", "Álvaro Parada", "2026-05-08");
        TourProcessingDraftAssignment notApprovedTour = row("3700", "Álvaro Parada", "2026-05-12");
        notApprovedTour.getTour().setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        when(rowRepository.findActiveByDraftId(DRAFT_ID)).thenReturn(List.of(approvedTour, notApprovedTour));
        when(dayRepository.findByAssignmentIdIn(anyCollection())).thenReturn(List.of(
                day(approvedTour, "2026-05-08", "07:30", "16:00", 8.5, 0.0, 0.0)
        ));

        OvertimeRequestReportDto schedule = service.buildSchedule(draft);

        assertThat(schedule.getDrivers().get(0).getTours())
                .extracting(OvertimeRequestReportDto.TourBlock::getNumber)
                .containsExactly("3622");
        verify(dayRepository).findByAssignmentIdIn(List.of(approvedTour.getId()));
    }

    @Test
    void failsWhenTheMonthHasNoDraft() {
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 6)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.build(2026, 6))
                .isInstanceOf(TransportException.class)
                .hasMessage("No hay un borrador de depuración para junio de 2026");
    }

    @Test
    void failsWhenTheDraftHasNoBusinessDays() {
        stubDraft(null);

        assertThatThrownBy(() -> service.build(2026, 5))
                .isInstanceOf(TransportException.class)
                .hasMessage("El borrador de mayo de 2026 no tiene días hábiles definidos");
    }

    @Test
    void buildScheduleIncludesOrdinaryOnlyDaysAndTheRowId() {
        TourProcessingDraft draft = TourProcessingDraft.builder().id(DRAFT_ID).year(2026).month(5).businessDays(28).build();
        when(driverRepository.findAll()).thenReturn(List.of(driver("Álvaro Parada", "ACTIVE")));
        TourProcessingDraftAssignment tour = row("3622", "Álvaro Parada", "2026-05-08");
        tour.getTour().setReturnDate(LocalDate.parse("2026-05-09"));
        tour.getTour().setDestination("Turrialba");
        tour.setVehicle("301-627");
        when(rowRepository.findActiveByDraftId(DRAFT_ID)).thenReturn(List.of(tour));
        when(dayRepository.findByAssignmentIdIn(anyCollection())).thenReturn(List.of(
                day(tour, "2026-05-08", "07:30", "16:00", 8.5, 0.0, 0.0)
        ));

        OvertimeRequestReportDto schedule = service.buildSchedule(draft);

        assertThat(schedule.getDrivers()).hasSize(1);
        OvertimeRequestReportDto.TourBlock tourBlock = schedule.getDrivers().get(0).getTours().get(0);
        assertThat(tourBlock.getId()).isEqualTo(tour.getId());
        assertThat(tourBlock.getDepartureDate()).isEqualTo(LocalDate.parse("2026-05-08"));
        assertThat(tourBlock.getReturnDate()).isEqualTo(LocalDate.parse("2026-05-09"));
        assertThat(tourBlock.getDestination()).isEqualTo("Turrialba");
        assertThat(tourBlock.getVehicle()).isEqualTo("301-627");
        assertThat(tourBlock.getResponsible()).isEqualTo("Ana Mora");
        assertThat(tourBlock.getPassengers()).isEqualTo(3);
        assertThat(tourBlock.getPriority()).isEqualTo(1);
        assertThat(tourBlock.getDays())
                .extracting(OvertimeRequestReportDto.TourDay::getOrdinaryHours,
                        OvertimeRequestReportDto.TourDay::getSurplusHours,
                        OvertimeRequestReportDto.TourDay::getOvertimeHours)
                .containsExactly(tuple(8.5, 0.0, 0.0));
    }

    @Test
    void buildScheduleUsesBorrowedVehiclePlate() {
        TourProcessingDraft draft = TourProcessingDraft.builder().id(DRAFT_ID).year(2026).month(5).businessDays(28).build();
        when(driverRepository.findAll()).thenReturn(List.of(driver("Álvaro Parada", "ACTIVE")));
        TourProcessingDraftAssignment tour = row("3622", "Álvaro Parada", "2026-05-08");
        tour.setResourceType(ResourceType.BORROWED_VEHICLE);
        tour.setVehicle("Vehículo prestado");
        tour.setBorrowedVehicle(BorrowedVehicle.builder().plate("ABC-123").build());
        when(rowRepository.findActiveByDraftId(DRAFT_ID)).thenReturn(List.of(tour));
        when(dayRepository.findByAssignmentIdIn(anyCollection())).thenReturn(List.of(
                day(tour, "2026-05-08", "07:30", "16:00", 8.5, 0.0, 0.0)
        ));

        assertThat(service.buildSchedule(draft).getDrivers().get(0).getTours().get(0).getVehicle()).isEqualTo("ABC-123");
    }

    @Test
    void buildScheduleFailsWhenTheDraftHasNoBusinessDays() {
        TourProcessingDraft draft = TourProcessingDraft.builder().id(DRAFT_ID).year(2026).month(5).businessDays(null).build();

        assertThatThrownBy(() -> service.buildSchedule(draft))
                .isInstanceOf(TransportException.class)
                .hasMessage("El borrador no tiene días hábiles definidos");
    }

    @Test
    void listDriverSummariesKeepsTotalsButDropsTours() {
        TourProcessingDraft draft = TourProcessingDraft.builder().id(DRAFT_ID).year(2026).month(5).businessDays(28).build();
        Driver alvaro = driver("Álvaro Parada", "ACTIVE");
        when(driverRepository.findAll()).thenReturn(List.of(alvaro));
        TourProcessingDraftAssignment tour = row("3622", "Álvaro Parada", "2026-05-08");
        when(rowRepository.findActiveByDraftId(DRAFT_ID)).thenReturn(List.of(tour));
        when(dayRepository.findByAssignmentIdIn(anyCollection())).thenReturn(List.of(
                day(tour, "2026-05-08", "11:00", "20:30", 5.5, 4.0, 0.0)
        ));

        var total = mock(TourProcessingDraftDayRepository.DriverTotals.class);
        when(total.getDriverId()).thenReturn(alvaro.getId());
        when(total.getSurplusHours()).thenReturn(4.0);
        when(dayRepository.summarizeByDriver(DRAFT_ID)).thenReturn(List.of(total));

        OvertimeRequestReportDto summaries = service.listDriverSummaries(draft);

        assertThat(summaries.getDrivers()).hasSize(1);
        OvertimeRequestReportDto.DriverBlock summary = summaries.getDrivers().get(0);
        assertThat(summary.getId()).isEqualTo(alvaro.getId());
        assertThat(summary.getSurplusHours()).isEqualTo(4.0);
        assertThat(summary.getTours()).isEmpty();
    }

    @Test
    void buildDriverPagePaginatesThatDriversToursAndTotalsCoverAllOfThem() {
        TourProcessingDraft draft = TourProcessingDraft.builder().id(DRAFT_ID).year(2026).month(5).businessDays(28).build();
        Driver alvaro = driver("Álvaro Parada", "ACTIVE");
        when(driverRepository.findAll()).thenReturn(List.of(alvaro));
        TourProcessingDraftAssignment firstTour = row("3622", "Álvaro Parada", "2026-05-08");
        TourProcessingDraftAssignment secondTour = row("3700", "Álvaro Parada", "2026-05-12");
        when(rowRepository.findActiveByDraftId(DRAFT_ID)).thenReturn(List.of(firstTour, secondTour));
        when(dayRepository.findByAssignmentIdIn(anyCollection())).thenReturn(List.of(
                day(firstTour, "2026-05-08", "11:00", "20:30", 5.5, 4.0, 0.0),
                day(secondTour, "2026-05-12", "08:00", "15:00", 7.0, 0.0, 2.5)
        ));

        when(rowRepository.findDriverPage(DRAFT_ID, alvaro.getId(), PageRequest.of(0, 1)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(firstTour), PageRequest.of(0, 1), 2));
        when(rowRepository.findDriverPage(DRAFT_ID, alvaro.getId(), PageRequest.of(1, 1)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(secondTour), PageRequest.of(1, 1), 2));

        Page<OvertimeRequestReportDto.TourBlock> firstPage = service.buildDriverPage(draft, alvaro.getId(), PageRequest.of(0, 1));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(OvertimeRequestReportDto.TourBlock::getNumber).containsExactly("3622");

        Page<OvertimeRequestReportDto.TourBlock> secondPage = service.buildDriverPage(draft, alvaro.getId(), PageRequest.of(1, 1));
        assertThat(secondPage.getContent()).extracting(OvertimeRequestReportDto.TourBlock::getNumber).containsExactly("3700");
    }

    @Test
    void buildDriverPageFailsWhenTheDriverHasNoToursInTheDraft() {
        TourProcessingDraft draft = TourProcessingDraft.builder().id(DRAFT_ID).year(2026).month(5).businessDays(28).build();
        Driver alvaro = driver("Álvaro Parada", "ACTIVE");
        when(driverRepository.findAll()).thenReturn(List.of(alvaro));
        when(rowRepository.findActiveByDraftId(DRAFT_ID)).thenReturn(List.of());
        when(dayRepository.findByAssignmentIdIn(anyCollection())).thenReturn(List.of());

        when(rowRepository.findDriverPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Page.empty());
        assertThatThrownBy(() -> service.buildDriverPage(draft, UUID.randomUUID(), PageRequest.of(0, 10)))
                .isInstanceOf(TransportException.class)
                .hasMessageContaining("Chofer");
    }

    private void stubDraft(Integer businessDays) {
        TourProcessingDraft draft = TourProcessingDraft.builder().id(DRAFT_ID).year(2026).month(5).businessDays(businessDays).build();
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 5)).thenReturn(Optional.of(draft));
    }

    private Driver driver(String name, String status) {
        return Driver.builder().id(driverId(name)).name(name).status(status).build();
    }

    private UUID driverId(String name) {
        return UUID.nameUUIDFromBytes(com.proseg.msvc_transport.service.driverhours.DriverHoursRules.driverKey(name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private TourProcessingDraftAssignment row(String number, String driver, String departureDate) {
        TourProcessingDraftTour tour = TourProcessingDraftTour.builder()
                .id(UUID.randomUUID())
                .number(number)
                .priority(1)
                .passengers(3)
                .executingUnit("Escuela de Química")
                .responsible("Ana Mora")
                .duplicateRank(1)
                .departureDate(LocalDate.parse(departureDate))
                .build();
        return tour.addAssignment(TourProcessingDraftAssignment.builder().id(tour.getId()).driver(driver).driverId(driverId(driver)).passengers(3).build());
    }

    private TourProcessingDraftDay day(TourProcessingDraftAssignment row, String date, String startTime, String endTime,
                                     double ordinaryHours, double surplusHours, double overtimeHours) {
        return TourProcessingDraftDay.builder()
                .assignment(row)
                .date(LocalDate.parse(date))
                .startTime(LocalTime.parse(startTime))
                .endTime(LocalTime.parse(endTime))
                .ordinaryHours(ordinaryHours)
                .surplusHours(surplusHours)
                .overtimeHours(overtimeHours)
                .build();
    }
}
