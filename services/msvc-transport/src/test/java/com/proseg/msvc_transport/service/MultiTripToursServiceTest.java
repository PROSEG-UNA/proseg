package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_transport.dto.tourprocessing.FinalCommissionCheckDto;
import com.proseg.msvc_transport.dto.tourprocessing.FinalCommissionReportDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourControlReportDto;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.repository.DriverMonthBusinessDaysRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftAssignmentRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.service.driverhours.ManualDays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiTripToursServiceTest {

    private final UUID draftId = UUID.randomUUID();
    private final Driver ana = Driver.builder().id(UUID.randomUUID()).name("Ana").status("ACTIVE").build();
    private final Driver beto = Driver.builder().id(UUID.randomUUID()).name("Beto").status("ACTIVE").build();
    private final Vehicle bus = Vehicle.builder().id(UUID.randomUUID()).plate("301-586").capacity(30).available(true).build();
    private final Vehicle van = Vehicle.builder().id(UUID.randomUUID()).plate("301-414").capacity(30).available(true).build();

    private TourProcessingDraft draft;
    private DraftTourStore tourStore;
    private TourProcessingDraftDayRepository dayRepository;
    private DriverRepository driverRepository;
    private VehicleRepository vehicleRepository;
    private TourProcessingDraftRepository draftRepository;
    private InMemoryBusinessDays businessDays;

    @BeforeEach
    void setUp() {
        draft = TourProcessingDraft.builder().id(draftId).status(TourProcessingDraftStatus.DRAFT)
                .year(2026).month(5).businessDays(28).duplicateRows(0).conflictingRows(0).build();
        draftRepository = mock(TourProcessingDraftRepository.class);
        when(draftRepository.findById(draftId)).thenReturn(Optional.of(draft));
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 5)).thenReturn(Optional.of(draft));
        tourStore = mock(DraftTourStore.class);
        dayRepository = mock(TourProcessingDraftDayRepository.class);
        when(dayRepository.findByAssignmentIdIn(any())).thenReturn(List.of());
        driverRepository = mock(DriverRepository.class);
        when(driverRepository.findAll()).thenReturn(List.of(ana, beto));
        vehicleRepository = mock(VehicleRepository.class);
        when(vehicleRepository.findAll()).thenReturn(List.of(bus, van));
        businessDays = new InMemoryBusinessDays();
    }

    private FinalCommissionService commission() {
        DraftHoursService hours = mock(DraftHoursService.class);
        when(hours.manualDays(any())).thenReturn(ManualDays.NONE);
        return new FinalCommissionService(draftRepository, tourStore, dayRepository, driverRepository, vehicleRepository, hours,
                new DriverBusinessDaysService(mock(DriverMonthBusinessDaysRepository.class)));
    }

    private TourProcessingDraftTour tour(String number, String date, String departure, String arrival, int passengers) {
        return TourProcessingDraftTour.builder()
                .id(UUID.randomUUID())
                .rowNumber(Integer.parseInt(number))
                .number(number)
                .priority(1)
                .passengers(passengers)
                .executingUnit("Escuela de Química")
                .responsible("Ana Pérez")
                .destination("San José")
                .durationDays(1)
                .departureDate(LocalDate.parse(date))
                .departureTime(LocalTime.parse(departure))
                .returnDate(LocalDate.parse(date))
                .returnTime(LocalTime.parse(arrival))
                .duplicateRank(1)
                .duplicateGroupSize(1)
                .build();
    }

    private TourProcessingDraftAssignment trip(TourProcessingDraftTour tour, int passengers, Driver driver, Vehicle vehicle) {
        return tour.addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(passengers)
                .resourceType(ResourceType.INTERNAL)
                .driver(driver.getName()).driverId(driver.getId())
                .vehicle(vehicle.getPlate()).vehicleId(vehicle.getId()).build());
    }

    private TourProcessingDraftTour congress(String number, String date, String departure, String arrival, int trips) {
        TourProcessingDraftTour tour = tour(number, date, departure, arrival, 30 * trips);
        for (int index = 0; index < trips; index++) {
            trip(tour, 30, ana, bus);
        }
        return tour;
    }

    private TourProcessingDraftDay day(TourProcessingDraftAssignment assignment, String date, String start, String end, double surplus) {
        return TourProcessingDraftDay.builder().id(UUID.randomUUID()).assignment(assignment).date(LocalDate.parse(date))
                .startTime(LocalTime.parse(start)).endTime(LocalTime.parse(end))
                .ordinaryHours(9.0).surplusHours(surplus).overtimeHours(0.0).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void recalculatesTheMonthCountingTheTourHoursOnceForTheSameDriverAndRemovesOldTripDays() {
        TourProcessingDraftTour tour = congress("1", "2026-05-04", "07:30", "17:00", 5);
        TourProcessingDraftDay leftover = day(tour.getAssignments().get(2), "2026-05-04", "07:30", "17:00", 2.0);
        when(dayRepository.findAllByDraftId(any())).thenReturn(List.of(leftover));
        DraftHoursService service = new DraftHoursService(driverRepository, dayRepository, businessDays.service());

        service.syncAll(draft, tour.getAssignments());

        ArgumentCaptor<List<TourProcessingDraftDay>> saved = ArgumentCaptor.forClass(List.class);
        verify(dayRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(1);
        assertThat(saved.getValue().get(0).getAssignment()).isSameAs(tour.getAssignments().get(0));
        assertThat(saved.getValue().get(0).getSurplusHours()).isEqualTo(0.5);
        assertThat(saved.getValue().get(0).getOvertimeHours()).isZero();
        verify(dayRepository).deleteAllByIdInBatch(List.of(leftover.getId()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void givesEachOfTwoDriversTheTourHoursOnceWhenTheyAlternateTheTrips() {
        TourProcessingDraftTour tour = tour("1", "2026-05-04", "07:30", "17:00", 180);
        for (int index = 0; index < 6; index++) {
            trip(tour, 30, index % 2 == 0 ? ana : beto, index % 2 == 0 ? bus : van);
        }
        when(dayRepository.findAllByDraftId(any())).thenReturn(List.of());
        DraftHoursService service = new DraftHoursService(driverRepository, dayRepository, businessDays.service());

        service.syncAll(draft, tour.getAssignments());

        ArgumentCaptor<List<TourProcessingDraftDay>> saved = ArgumentCaptor.forClass(List.class);
        verify(dayRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(day -> day.getAssignment().getDriverId(), TourProcessingDraftDay::getSurplusHours)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(ana.getId(), 0.5),
                        org.assertj.core.groups.Tuple.tuple(beto.getId(), 0.5));
        assertThat(saved.getValue()).extracting(day -> day.getAssignment().getPart()).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void finalCommissionAcceptsRepeatedDriverAndPlateInTheSameTourWithoutErrorsOrWarnings() {
        TourProcessingDraftTour tour = congress("4001", "2026-05-04", "08:00", "18:00", 5);
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(tour));

        FinalCommissionCheckDto check = commission().check(draftId);

        assertThat(check.getBlockers()).isEmpty();
        assertThat(check.getErrors()).isEmpty();
        assertThat(check.getWarnings()).extracting(FinalCommissionCheckDto.Issue::getCode)
                .doesNotContain("VEHICLE_OVERLAP", "DRIVER_OVERLAP", "DAY_HOURS_EXCEEDED", "VEHICLE_OVER_CAPACITY");
    }

    @Test
    void finalCommissionDoesNotComplainWhenTheTripsDoNotCoverAllPassengers() {
        TourProcessingDraftTour tour = tour("4002", "2026-05-04", "08:00", "12:00", 150);
        for (int index = 0; index < 4; index++) {
            trip(tour, 30, ana, bus);
        }
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(tour));

        FinalCommissionCheckDto check = commission().check(draftId);

        assertThat(check.getErrors()).isEmpty();
        assertThat(check.getBlockers()).isEmpty();
    }

    @Test
    void finalCommissionStillFlagsATripWithMoreThanTheVehicleCapacity() {
        TourProcessingDraftTour tour = tour("4003", "2026-05-04", "08:00", "12:00", 61);
        trip(tour, 31, ana, bus);
        trip(tour, 30, ana, bus);
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(tour));

        FinalCommissionCheckDto check = commission().check(draftId);

        assertThat(check.getWarnings()).extracting(FinalCommissionCheckDto.Issue::getCode).contains("VEHICLE_OVER_CAPACITY");
    }

    @Test
    void finalCommissionStillFlagsTheSamePlateInDifferentToursTheSameDay() {
        TourProcessingDraftTour first = congress("4004", "2026-05-04", "08:00", "12:00", 2);
        TourProcessingDraftTour second = tour("4005", "2026-05-04", "09:00", "11:00", 20);
        trip(second, 20, beto, bus);
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(first, second));

        FinalCommissionCheckDto check = commission().check(draftId);

        assertThat(check.getWarnings()).extracting(FinalCommissionCheckDto.Issue::getCode).contains("VEHICLE_OVERLAP");
        assertThat(check.getWarnings().stream().filter(issue -> issue.getCode().equals("VEHICLE_OVERLAP")).findFirst().orElseThrow().getDetails())
                .containsExactly("Placa 301-586: giras 4004 y 4005 (04/05/2026)");
    }

    @Test
    void finalCommissionStillAppliesTheDailyLimitWhenAnotherTourIsStackedOnAMultiTripTour() {
        TourProcessingDraftTour first = congress("4006", "2026-05-04", "08:00", "18:00", 5);
        TourProcessingDraftTour second = tour("4007", "2026-05-04", "18:30", "21:30", 10);
        trip(second, 10, ana, van);
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(first, second));

        FinalCommissionCheckDto check = commission().check(draftId);

        assertThat(check.getWarnings()).extracting(FinalCommissionCheckDto.Issue::getCode).contains("DAY_HOURS_EXCEEDED");
    }

    @Test
    void finalCommissionMultipliesHoursByTheDistinctDriversAndNotByTheTrips() {
        TourProcessingDraftTour oneDriver = congress("4008", "2026-05-04", "08:00", "12:00", 5);
        TourProcessingDraftTour twoDrivers = tour("4009", "2026-05-05", "08:00", "12:00", 180);
        for (int index = 0; index < 6; index++) {
            trip(twoDrivers, 30, index % 2 == 0 ? ana : beto, index % 2 == 0 ? bus : van);
        }
        TourProcessingDraftTour differentVehicles = tour("4010", "2026-05-06", "08:00", "12:00", 60);
        trip(differentVehicles, 30, ana, bus);
        trip(differentVehicles, 30, beto, van);
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(oneDriver, twoDrivers, differentVehicles));

        FinalCommissionReportDto report = commission().report(draftId);

        assertThat(report.getTours()).extracting(FinalCommissionReportDto.Tour::getNumber, FinalCommissionReportDto.Tour::getVehicles)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("4008", 1),
                        org.assertj.core.groups.Tuple.tuple("4009", 2),
                        org.assertj.core.groups.Tuple.tuple("4010", 2));
    }

    @Test
    void tourControlShowsOneEntryPerDriverWithTheSummedPassengersAndTheHoursOnce() {
        TourProcessingDraftTour tour = congress("5001", "2026-05-04", "07:30", "17:00", 5);
        TourProcessingDraftAssignment anchor = tour.getAssignments().get(0);
        when(dayRepository.findByAssignmentIdIn(any())).thenReturn(List.of(day(anchor, "2026-05-04", "07:30", "17:00", 0.5)));
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(tour));
        TourControlReportService service = new TourControlReportService(draftRepository, tourStore, dayRepository, driverRepository,
                vehicleRepository, new DriverBusinessDaysService(mock(DriverMonthBusinessDaysRepository.class)));

        TourControlReportDto report = service.report(draftId);

        assertThat(report.getEntries()).hasSize(1);
        TourControlReportDto.Entry entry = report.getEntries().get(0);
        assertThat(entry.getSection()).isEqualTo("DRIVER");
        assertThat(entry.getPassengers()).isEqualTo(150);
        assertThat(entry.getPlate()).isEqualTo("301-586");
        assertThat(entry.getHours()).isEqualTo(0.5);
        TourControlReportDto.Driver driver = report.getDrivers().stream().filter(candidate -> candidate.getName().equals("Ana")).findFirst().orElseThrow();
        assertThat(driver.getSurplusHours()).isEqualTo(0.5);
        assertThat(driver.getOvertimeHours()).isZero();
    }

    @Test
    void tourControlShowsOneEntryForEachOfTwoAlternatingDrivers() {
        TourProcessingDraftTour tour = tour("5002", "2026-05-04", "07:30", "17:00", 180);
        for (int index = 0; index < 6; index++) {
            trip(tour, 30, index % 2 == 0 ? ana : beto, index % 2 == 0 ? bus : van);
        }
        when(dayRepository.findByAssignmentIdIn(any())).thenReturn(List.of(
                day(tour.getAssignments().get(0), "2026-05-04", "07:30", "17:00", 0.5),
                day(tour.getAssignments().get(1), "2026-05-04", "07:30", "17:00", 0.5)));
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(tour));
        TourControlReportService service = new TourControlReportService(draftRepository, tourStore, dayRepository, driverRepository,
                vehicleRepository, new DriverBusinessDaysService(mock(DriverMonthBusinessDaysRepository.class)));

        TourControlReportDto report = service.report(draftId);

        assertThat(report.getEntries()).extracting(TourControlReportDto.Entry::getDriver, TourControlReportDto.Entry::getPassengers,
                        TourControlReportDto.Entry::getPlate, TourControlReportDto.Entry::getHours)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Ana", 90, "301-586", 0.5),
                        org.assertj.core.groups.Tuple.tuple("Beto", 90, "301-414", 0.5));
    }

    @Test
    void overtimeRequestListsTheTourOnceWithTheHoursOfTheFirstTripAndTheDriverPassengers() {
        TourProcessingDraftTour tour = congress("6001", "2026-05-04", "07:30", "17:00", 5);
        List<TourProcessingDraftAssignment> trips = new ArrayList<>(tour.getAssignments());
        TourProcessingDraftAssignmentRepository rowRepository = mock(TourProcessingDraftAssignmentRepository.class);
        when(rowRepository.findActiveByDraftId(draftId)).thenReturn(trips);
        when(dayRepository.findByAssignmentIdIn(anyCollection())).thenReturn(List.of(day(trips.get(0), "2026-05-04", "07:30", "17:00", 0.5)));
        OvertimeRequestReportService service = new OvertimeRequestReportService(draftRepository, rowRepository, dayRepository, driverRepository,
                new DriverBusinessDaysService(mock(DriverMonthBusinessDaysRepository.class)));

        OvertimeRequestReportDto report = service.build(2026, 5);

        OvertimeRequestReportDto.DriverBlock anaBlock = report.getDrivers().stream().filter(block -> block.getName().equals("Ana")).findFirst().orElseThrow();
        assertThat(anaBlock.getSurplusHours()).isEqualTo(0.5);
        assertThat(anaBlock.getTours()).hasSize(1);
        assertThat(anaBlock.getTours().get(0).getPassengers()).isEqualTo(150);
        assertThat(anaBlock.getTours().get(0).getDays()).hasSize(1);
    }
}
