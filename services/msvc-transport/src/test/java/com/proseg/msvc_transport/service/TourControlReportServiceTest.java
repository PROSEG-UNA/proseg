package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.tourprocessing.TourControlReportDto;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.DriverUnit;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.DriverMonthBusinessDaysRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TourControlReportServiceTest {

    private final UUID draftId = UUID.randomUUID();
    private final Driver zoe = Driver.builder().id(UUID.randomUUID()).name("Zoe Mora").status("ACTIVE")
            .ordinaryStart(LocalTime.of(12, 0)).ordinaryEnd(LocalTime.of(21, 0)).build();
    private final Driver ana = Driver.builder().id(UUID.randomUUID()).name("Ana Pérez").status("ACTIVE").build();
    private final Driver retired = Driver.builder().id(UUID.randomUUID()).name("Luis Rojas").status("INACTIVE").build();
    private final Driver guest = Driver.builder().id(UUID.randomUUID()).name("Jorge Mena").status("ACTIVE")
            .organizationalUnit(DriverUnit.OTHER_UNIT).build();
    private final Vehicle bus = Vehicle.builder().id(UUID.randomUUID()).plate("301-617").capacity(43).available(true).build();

    private DraftTourStore tourStore;
    private TourProcessingDraftDayRepository dayRepository;
    private TourControlReportService service;

    @BeforeEach
    void setUp() {
        TourProcessingDraftRepository draftRepository = mock(TourProcessingDraftRepository.class);
        TourProcessingDraft draft = TourProcessingDraft.builder().id(draftId)
                .status(TourProcessingDraftStatus.DRAFT).year(2026).month(5).businessDays(28).build();
        when(draftRepository.findById(draftId)).thenReturn(Optional.of(draft));
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 5)).thenReturn(Optional.of(draft));
        tourStore = mock(DraftTourStore.class);
        dayRepository = mock(TourProcessingDraftDayRepository.class);
        when(dayRepository.findByAssignmentIdIn(any())).thenReturn(List.of());
        DriverRepository driverRepository = mock(DriverRepository.class);
        when(driverRepository.findAll()).thenReturn(List.of(zoe, ana, retired, guest));
        VehicleRepository vehicleRepository = mock(VehicleRepository.class);
        when(vehicleRepository.findAll()).thenReturn(List.of(bus));
        service = new TourControlReportService(draftRepository, tourStore, dayRepository, driverRepository, vehicleRepository,
                new DriverBusinessDaysService(mock(DriverMonthBusinessDaysRepository.class)));
    }

    @Test
    void buildsTheReportOfAMonthFromItsLatestDraft() {
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of());

        TourControlReportDto report = service.reportForMonth(2026, 5);

        assertThat(report.getYear()).isEqualTo(2026);
        assertThat(report.getMonth()).isEqualTo(5);
    }

    @Test
    void rejectsMonthsWithoutADraftAndInvalidMonths() {
        assertThatThrownBy(() -> service.reportForMonth(2026, 6)).isInstanceOfSatisfying(TransportException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_DRAFT_MONTH_NOT_FOUND");
            assertThat(exception.getMessage()).contains("junio de 2026");
        });
        assertThatThrownBy(() -> service.reportForMonth(2026, 13)).isInstanceOfSatisfying(TransportException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("INVALID_MONTH"));
    }

    @Test
    void listsActiveGeneralServicesDriversSortedByNameWithTheirOrdinaryWindow() {
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of());

        TourControlReportDto report = service.report(draftId);

        assertThat(report.getDrivers()).extracting(TourControlReportDto.Driver::getName).containsExactly("Ana Pérez", "Zoe Mora");
        assertThat(report.getDrivers().get(1).getOrdinaryStart()).isEqualTo(12.0);
        assertThat(report.getDrivers().get(1).getOrdinaryEnd()).isEqualTo(21.0);
        assertThat(report.getDrivers().get(0).getOrdinaryEnd()).isEqualTo(16.5);
        assertThat(report.getWeekdays()).isEqualTo(21);
    }

    @Test
    void placesEachAssignmentInItsSectionAndSplitsDriverToursByDay() {
        TourProcessingDraftTour multiDay = tour("3625", "2026-05-02", "05:00", "2026-05-03", "10:00");
        TourProcessingDraftAssignment driven = multiDay.addAssignment(part(ana, bus));
        TourProcessingDraftTour lent = tour("4040", "2026-05-02", "07:00", "2026-05-02", "19:00");
        lent.addAssignment(part(guest, bus));
        TourProcessingDraftTour hired = tour("3471", "2026-05-06", "06:00", "2026-05-06", "18:00");
        hired.addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(30)
                .resourceType(ResourceType.EXTERNAL).driver("TRANSVANU").vehicle("HB4085").build());
        TourProcessingDraftTour rejected = tour("3531", "2026-05-02", "07:30", "2026-05-02", "10:30");
        rejected.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        rejected.addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(22).build());
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(hired, rejected, lent, multiDay));
        when(dayRepository.findByAssignmentIdIn(any())).thenReturn(List.of(
                day(driven, "2026-05-02", "05:00", "18:30", 13.5),
                day(driven, "2026-05-03", "05:30", "10:00", 4.5)
        ));

        TourControlReportDto report = service.report(draftId);
        List<TourControlReportDto.Entry> entries = report.getEntries();

        assertThat(entries).extracting(TourControlReportDto.Entry::getSection, TourControlReportDto.Entry::getNumber)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("DRIVER", "3625"),
                        org.assertj.core.groups.Tuple.tuple("DRIVER", "3625"),
                        org.assertj.core.groups.Tuple.tuple("OTHER_UNIT", "4040"),
                        org.assertj.core.groups.Tuple.tuple("UNATTENDED", "3531"),
                        org.assertj.core.groups.Tuple.tuple("EXTERNAL", "3471"));
        TourControlReportDto.Entry secondDay = entries.get(1);
        assertThat(secondDay.getDate()).isEqualTo(LocalDate.parse("2026-05-03"));
        assertThat(secondDay.getDepartureDate()).isEqualTo(LocalDate.parse("2026-05-02"));
        assertThat(secondDay.getDriverIndex()).isZero();
        assertThat(secondDay.getPlate()).isEqualTo("301-617");
        assertThat(secondDay.getStart()).isEqualTo(5.5);
        assertThat(secondDay.getHours()).isEqualTo(4.5);
        assertThat(entries.get(2).getDriver()).isEqualTo("Jorge Mena");
        assertThat(entries.get(2).getDriverIndex()).isNull();
        assertThat(entries.get(4).getDriver()).isEqualTo("TRANSVANU");
        assertThat(entries.get(4).getPlate()).isEqualTo("HB4085");
        assertThat(entries.get(4).getEnd()).isEqualTo(18.0);
        assertThat(report.getDrivers().get(0).getSurplusHours()).isEqualTo(18.0);
        assertThat(report.getDrivers().get(1).getSurplusHours()).isZero();
    }

    private TourProcessingDraftTour tour(String number, String departureDate, String departureTime, String returnDate, String returnTime) {
        return TourProcessingDraftTour.builder()
                .id(UUID.randomUUID())
                .rowNumber(Integer.parseInt(number))
                .number(number)
                .priority(1)
                .passengers(20)
                .executingUnit("Escuela de Química")
                .responsible("Selvin Fallas")
                .destination("Tayutic")
                .durationDays(1)
                .departureDate(LocalDate.parse(departureDate))
                .departureTime(LocalTime.parse(departureTime))
                .returnDate(LocalDate.parse(returnDate))
                .returnTime(LocalTime.parse(returnTime))
                .duplicateRank(1)
                .duplicateGroupSize(1)
                .build();
    }

    private TourProcessingDraftAssignment part(Driver driver, Vehicle vehicle) {
        return TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(20)
                .driver(driver.getName()).driverId(driver.getId())
                .vehicle(vehicle.getPlate()).vehicleId(vehicle.getId()).build();
    }

    private TourProcessingDraftDay day(TourProcessingDraftAssignment assignment, String date, String start, String end, double hours) {
        return TourProcessingDraftDay.builder().id(UUID.randomUUID()).assignment(assignment).date(LocalDate.parse(date))
                .startTime(LocalTime.parse(start)).endTime(LocalTime.parse(end))
                .ordinaryHours(0.0).surplusHours(hours).overtimeHours(0.0).build();
    }
}
