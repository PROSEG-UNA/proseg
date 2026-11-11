package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.tourprocessing.FinalCommissionCheckDto;
import com.proseg.msvc_transport.dto.tourprocessing.FinalCommissionReportDto;
import com.proseg.msvc_transport.entity.BorrowedVehicle;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.DriverUnit;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.entity.VehicleType;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.DriverMonthBusinessDaysRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.service.driverhours.ManualDays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinalCommissionServiceTest {

    private final UUID draftId = UUID.randomUUID();
    private final Driver ana = Driver.builder().id(UUID.randomUUID()).name("Ana").status("ACTIVE").build();
    private final Vehicle microbus = Vehicle.builder().id(UUID.randomUUID()).plate("301-586").capacity(30).available(true).build();
    private final Vehicle sedan = Vehicle.builder().id(UUID.randomUUID()).plate("301-414").capacity(30).available(true).build();

    private DraftTourStore tourStore;
    private DriverRepository driverRepository;
    private VehicleRepository vehicleRepository;
    private DraftHoursService draftHoursService;
    private FinalCommissionService service;

    private TourProcessingDraft draft;

    @BeforeEach
    void setUp() {
        TourProcessingDraftRepository draftRepository = mock(TourProcessingDraftRepository.class);
        draft = TourProcessingDraft.builder().id(draftId).status(TourProcessingDraftStatus.DRAFT)
                .year(2026).month(5).businessDays(28).duplicateRows(0).conflictingRows(0).build();
        when(draftRepository.findById(draftId)).thenReturn(Optional.of(draft));
        when(draftRepository.findByIdForUpdate(draftId)).thenReturn(Optional.of(draft));
        tourStore = mock(DraftTourStore.class);
        TourProcessingDraftDayRepository dayRepository = mock(TourProcessingDraftDayRepository.class);
        when(dayRepository.findByAssignmentIdIn(any())).thenReturn(List.of());
        driverRepository = mock(DriverRepository.class);
        when(driverRepository.findAll()).thenReturn(List.of(ana));
        vehicleRepository = mock(VehicleRepository.class);
        when(vehicleRepository.findAll()).thenReturn(List.of(microbus, sedan));
        draftHoursService = mock(DraftHoursService.class);
        when(draftHoursService.manualDays(any())).thenReturn(ManualDays.NONE);
        service = new FinalCommissionService(draftRepository, tourStore, dayRepository, driverRepository, vehicleRepository, draftHoursService,
                new DriverBusinessDaysService(mock(DriverMonthBusinessDaysRepository.class)));
    }

    @Test
    void blocksToursSplitIntoTripsWhereOnlySomeTripsHaveDriverAndVehicle() {
        TourProcessingDraftTour split = tour("4001", 1, 40, "2026-05-04");
        assign(split.addAssignment(part(20)), ana, microbus);
        split.addAssignment(part(20));
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(split));

        FinalCommissionCheckDto check = service.check(draftId);

        assertThat(check.getBlockers()).singleElement().satisfies(issue -> {
            assertThat(issue.getCode()).isEqualTo("PARTIAL_DISTRIBUTION");
            assertThat(issue.getDetails()).containsExactly("Gira 4001 (04/05/2026): viajes sin chofer o vehículo: 2 de 2");
        });
        assertThatThrownBy(() -> service.report(draftId)).isInstanceOf(TransportException.class);
        assertThatThrownBy(() -> service.rejectIncompleteTours(draftId)).isInstanceOf(TransportException.class);
    }

    @Test
    void blocksClosedDraftsWithIncompleteToursUntilTheyReturnToDraft() {
        draft.setStatus(TourProcessingDraftStatus.CLOSED);
        TourProcessingDraftTour withoutVehicle = tour("4010", 1, 3, "2026-05-05");
        assign(withoutVehicle.addAssignment(part(3)), ana, null);
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(withoutVehicle));

        FinalCommissionCheckDto check = service.check(draftId);

        assertThat(check.getBlockers()).singleElement().satisfies(issue -> {
            assertThat(issue.getCode()).isEqualTo("CLOSED_WITH_INCOMPLETE_TOURS");
            assertThat(issue.getDetails()).containsExactly("Gira 4010 (05/05/2026), prioridad 1, 3 pasajeros: sin vehículo");
        });
        assertThatThrownBy(() -> service.rejectIncompleteTours(draftId))
                .isInstanceOf(TransportException.class)
                .hasMessageContaining("Devuélvela a borrador");
    }

    @Test
    void doesNotAskToReopenRegisteredDrafts() {
        draft.setStatus(TourProcessingDraftStatus.REGISTERED);
        TourProcessingDraftTour withoutVehicle = tour("4011", 1, 3, "2026-05-05");
        assign(withoutVehicle.addAssignment(part(3)), ana, null);
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(withoutVehicle));

        assertThat(service.check(draftId).getBlockers()).singleElement()
                .satisfies(issue -> assertThat(issue.getTitle()).doesNotContain("Devuélvela a borrador"));
        assertThatThrownBy(() -> service.rejectIncompleteTours(draftId))
                .isInstanceOf(TransportException.class)
                .hasMessageContaining("ya fue registrado");
    }

    @Test
    void listsToursWithoutDriverOrVehicleAsRejectionsAndMarksThemNotApproved() {
        TourProcessingDraftTour complete = tour("4002", 1, 10, "2026-05-04");
        assign(complete.addAssignment(part(10)), ana, microbus);
        TourProcessingDraftTour withoutVehicle = tour("4003", 2, 3, "2026-05-05");
        assign(withoutVehicle.addAssignment(part(3)), ana, null);
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(complete, withoutVehicle));

        FinalCommissionCheckDto check = service.check(draftId);

        assertThat(check.getBlockers()).isEmpty();
        assertThat(check.getErrors()).isEmpty();
        assertThat(check.getRejections().getDetails()).containsExactly("Gira 4003 (05/05/2026), prioridad 2, 3 pasajeros: sin vehículo");
        assertThatThrownBy(() -> service.report(draftId)).isInstanceOf(TransportException.class);

        assertThat(service.rejectIncompleteTours(draftId)).isEqualTo(1);
        assertThat(withoutVehicle.getApprovalStatus()).isEqualTo(TourApprovalStatus.NOT_APPROVED);
        assertThat(complete.getApprovalStatus()).isEqualTo(TourApprovalStatus.APPROVED);
        verify(tourStore).saveAll(List.of(withoutVehicle));
        verify(draftHoursService).syncChanged(any(), any(), any());
    }

    @Test
    void reportsMissingDriversAndVehiclesAsErrors() {
        TourProcessingDraftTour empty = tour("4004", 1, 10, "2026-05-04");
        empty.addAssignment(part(10));
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(empty));

        assertThat(service.check(draftId).getErrors())
                .extracting(FinalCommissionCheckDto.Issue::getCode)
                .containsExactly("NO_DRIVERS", "NO_VEHICLES");
    }

    @Test
    void warnsWhenADriverAndVehicleCoverOverlappingToursAndExternalContractingHasFewPassengers() {
        TourProcessingDraftTour first = tour("4005", 1, 10, "2026-05-04");
        assign(first.addAssignment(part(10)), ana, microbus);
        TourProcessingDraftTour second = tour("4006", 1, 10, "2026-05-04");
        assign(second.addAssignment(part(10)), ana, microbus);
        TourProcessingDraftTour external = tour("4007", 1, 3, "2026-05-06");
        external.addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(3)
                .resourceType(ResourceType.EXTERNAL).driver("GLODA").vehicle("HB4085").build());
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(first, second, external));

        FinalCommissionCheckDto check = service.check(draftId);

        assertThat(check.getWarnings()).extracting(FinalCommissionCheckDto.Issue::getCode)
                .contains("DRIVER_OVERLAP", "VEHICLE_OVERLAP", "EXTERNAL_UNDER_MINIMUM");
    }

    @Test
    void buildsTheReportSortedByPriorityWithCategoriesAndDaySpans() {
        TourProcessingDraftTour lowPriority = tour("4008", 3, 10, "2026-05-04");
        lowPriority.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        lowPriority.addAssignment(part(10));
        TourProcessingDraftTour multiDay = tour("4009", 1, 10, "2026-05-29");
        multiDay.setReturnDate(LocalDate.parse("2026-05-30"));
        multiDay.setReturnTime(LocalTime.parse("14:00"));
        assign(multiDay.addAssignment(part(5)), ana, microbus);
        assign(multiDay.addAssignment(part(5)), ana, sedan);
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(lowPriority, multiDay));

        FinalCommissionReportDto report = service.report(draftId);

        assertThat(report.getActiveDrivers()).isEqualTo(1);
        assertThat(report.getSurplusCapPerDriver()).isEqualTo(37.5);
        assertThat(report.getTours()).extracting(FinalCommissionReportDto.Tour::getNumber).containsExactly("4009", "4008");
        FinalCommissionReportDto.Tour first = report.getTours().get(0);
        assertThat(first.getCategory()).isEqualTo("INTERNAL");
        assertThat(first.getVehicles()).isEqualTo(1);
        assertThat(first.getDays()).extracting(FinalCommissionReportDto.Day::getEnd).containsExactly(20.5, 14.0);
        assertThat(report.getTours().get(1).getCategory()).isEqualTo("NOT_APPROVED");
    }

    @Test
    void includesOtherUnitDriversHoursInTheFinalCommissionReport() {
        ana.setOrganizationalUnit(DriverUnit.OTHER_UNIT);
        ana.setOrdinaryStart(LocalTime.parse("12:00"));
        ana.setOrdinaryEnd(LocalTime.parse("21:00"));
        BorrowedVehicle borrowedVehicle = BorrowedVehicle.builder().id(UUID.randomUUID()).plate("P-123").build();
        TourProcessingDraftTour weekday = tour("4012", 1, 10, "2026-05-29");
        weekday.setDepartureTime(LocalTime.parse("06:00"));
        assign(weekday.addAssignment(part(10)), ana, null);
        weekday.primaryAssignment().setResourceType(ResourceType.BORROWED_VEHICLE);
        weekday.primaryAssignment().setBorrowedVehicle(borrowedVehicle);
        TourProcessingDraftTour weekend = tour("4013", 1, 10, "2026-05-30");
        weekend.setDepartureTime(LocalTime.parse("08:00"));
        weekend.setReturnTime(LocalTime.parse("12:00"));
        assign(weekend.addAssignment(part(10)), ana, null);
        weekend.primaryAssignment().setResourceType(ResourceType.BORROWED_VEHICLE);
        weekend.primaryAssignment().setBorrowedVehicle(borrowedVehicle);
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(weekday, weekend));

        FinalCommissionReportDto report = service.report(draftId);

        assertThat(report.getActiveDrivers()).isZero();
        assertThat(report.getActiveDriversSurplusCap()).isZero();
        assertThat(report.getTours()).extracting(FinalCommissionReportDto.Tour::getCategory)
                .containsExactly("BORROWED_VEHICLE", "BORROWED_VEHICLE");
        assertThat(report.getTours().get(0).getOrdinaryStart()).isEqualTo(12);
        assertThat(report.getTours().get(0).getOrdinaryEnd()).isEqualTo(21);
        assertThat(report.getTours().get(0).getDays()).singleElement().satisfies(day -> {
            assertThat(day.getStart()).isEqualTo(6);
            assertThat(day.getEnd()).isEqualTo(18);
        });
        assertThat(report.getTours().get(1).getDays()).singleElement().satisfies(day -> {
            assertThat(day.getStart()).isEqualTo(8);
            assertThat(day.getEnd()).isEqualTo(12);
        });
    }

    @Test
    void keepsThePreviousFinalCommissionWarningsExceptWeekendCapAndRepeatedPlates() {
        Driver beto = named("Beto");
        Driver carlos = named("Carlos");
        Driver diana = named("Diana");
        Driver elena = named("Elena");
        elena.setStatus("INACTIVE");
        Driver fabio = named("Fabio");
        fabio.setAllowedVehicleTypes(EnumSet.of(VehicleType.SEDAN));
        Driver gabi = named("Gabi");
        Driver hugo = named("Hugo");
        Driver ivan = named("Ivan");
        Vehicle anaBus = fleet("ANA-1", 30, true, VehicleType.MICROBUS);
        Vehicle anaVan = fleet("ANA-2", 30, true, VehicleType.SEDAN);
        Vehicle plain = fleet("PLAIN", 30, true, VehicleType.SEDAN);
        Vehicle micro = fleet("FAB-1", 12, true, VehicleType.MICROBUS);
        Vehicle closed = fleet("GAB-1", 12, false, VehicleType.SEDAN);
        Vehicle huge = fleet("HUG-01", 30, true, VehicleType.MICROBUS);
        Vehicle ownPlate = fleet("ABC-123", 12, true, VehicleType.SEDAN);
        when(driverRepository.findAll()).thenReturn(List.of(ana, beto, carlos, diana, elena, fabio, gabi, hugo, ivan));
        when(vehicleRepository.findAll()).thenReturn(List.of(microbus, sedan, anaBus, anaVan, plain, micro, closed, huge, ownPlate));

        TourProcessingDraftTour anaFirst = scheduled("100", 2, 10, "2026-05-06", "13:00", "18:00", ana, anaBus);
        TourProcessingDraftTour anaSecond = scheduled("101", 2, 10, "2026-05-06", "13:00", "18:00", ana, anaVan);
        TourProcessingDraftTour betoTour = scheduled("110", 2, 8, "2026-05-07", "08:00", "12:00", beto, plain);
        TourProcessingDraftTour carlosTour = scheduled("120", 2, 8, "2026-05-08", "08:00", "12:00", carlos, plain);
        TourProcessingDraftTour dianaWeekday = scheduled("130", 2, 8, "2026-05-04", "08:00", "12:00", diana, plain);
        TourProcessingDraftTour weekendA = scheduled("131", 2, 4, "2026-05-02", "08:00", "09:00", diana, plain);
        TourProcessingDraftTour weekendB = scheduled("132", 2, 4, "2026-05-09", "08:00", "09:00", diana, plain);
        TourProcessingDraftTour weekendC = scheduled("133", 2, 4, "2026-05-16", "08:00", "09:00", diana, plain);
        TourProcessingDraftTour weekendD = scheduled("134", 2, 4, "2026-05-23", "08:00", "09:00", diana, plain);
        TourProcessingDraftTour elenaTour = scheduled("140", 2, 6, "2026-05-11", "08:00", "12:00", elena, plain);
        TourProcessingDraftTour fabioTour = scheduled("150", 2, 6, "2026-05-12", "08:00", "12:00", fabio, micro);
        TourProcessingDraftTour gabiTour = scheduled("160", 2, 6, "2026-05-13", "08:00", "12:00", gabi, closed);
        TourProcessingDraftTour hugoTour = scheduled("170", 2, 41, "2026-05-14", "08:00", "12:00", hugo, huge);
        assign(hugoTour.addAssignment(part(10)), hugo, huge);
        hugoTour.primaryAssignment().setPassengers(31);
        TourProcessingDraftTour beyondFleet = scheduled("175", 2, 40, "2026-05-19", "08:00", "12:00", ivan, huge);
        TourProcessingDraftTour fitsAnotherVehicle = scheduled("176", 2, 20, "2026-05-20", "08:00", "12:00", ivan, micro);
        TourProcessingDraftTour ivanTour = scheduled("180", 2, 6, "2026-05-15", "08:00", "10:00", ivan, ownPlate);
        TourProcessingDraftTour external = tour("181", 2, 3, "2026-05-15");
        external.setDepartureTime(LocalTime.parse("12:00"));
        external.setReturnTime(LocalTime.parse("14:00"));
        external.addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(3)
                .resourceType(ResourceType.EXTERNAL).driver("Contratado").vehicle("abc 123").build());
        TourProcessingDraftTour priority = tour("190", 1, 3, "2026-05-18");
        priority.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        assign(priority.addAssignment(part(3)), ivan, plain);

        when(draftHoursService.manualDays(draftId)).thenReturn(ManualDays.of(List.of(
                manualDay(betoTour, "2026-05-07", 40, 0),
                manualDay(carlosTour, "2026-05-08", 36, 61),
                manualDay(dianaWeekday, "2026-05-04", 36, 0)
        )));
        when(tourStore.findPrimariesByDraftId(draftId)).thenReturn(List.of(
                anaFirst, anaSecond, betoTour, carlosTour, dianaWeekday, weekendA, weekendB, weekendC, weekendD,
                elenaTour, fabioTour, gabiTour, hugoTour, ivanTour, external, priority, beyondFleet, fitsAnotherVehicle));

        FinalCommissionCheckDto check = service.check(draftId);

        assertThat(check.getBlockers()).isEmpty();
        assertThat(check.getErrors()).extracting(FinalCommissionCheckDto.Issue::getCode).containsExactly("OVERSIZED");
        assertThat(check.getErrors().get(0).getDetails()).containsExactly(
                "Gira 175 (19/05/2026): 40 pasajeros para una capacidad de 30",
                "Gira 176 (20/05/2026): 20 pasajeros para una capacidad de 12");
        assertThat(check.getRejections()).isNull();
        assertThat(check.getWarnings()).extracting(FinalCommissionCheckDto.Issue::getCode)
                .containsExactly(
                        "DRIVER_OVERLAP",
                        "SURPLUS_CAP_EXCEEDED",
                        "OVERTIME_CAP_EXCEEDED",
                        "WEEKEND_CAP_EXCEEDED",
                        "INACTIVE_DRIVER",
                        "PLATE_REPEATED_SAME_DAY",
                        "VEHICLE_OVER_CAPACITY",
                        "INCOMPATIBLE_VEHICLE",
                        "VEHICLE_UNAVAILABLE",
                        "EXTERNAL_UNDER_MINIMUM",
                        "PRIORITY_ONE_UNATTENDED");
        assertThat(check.getWarnings()).extracting(FinalCommissionCheckDto.Issue::getCode).doesNotContain("NO_FREE_WEEKEND");
        assertThat(check.getWarnings()).filteredOn(issue -> issue.getCode().equals("PLATE_REPEATED_SAME_DAY"))
                .singleElement()
                .satisfies(issue -> assertThat(issue.getDetails()).contains("Placa ABC123: giras 180 y 181 (15/05/2026)"));
        assertThat(check.getWarnings()).filteredOn(issue -> issue.getCode().equals("WEEKEND_CAP_EXCEEDED"))
                .singleElement()
                .satisfies(issue -> assertThat(issue.getDetails()).contains("Diana: trabaja 4 fines de semana, tope de 3"));
        assertThat(check.getWarnings()).filteredOn(issue -> issue.getCode().equals("VEHICLE_OVER_CAPACITY"))
                .singleElement()
                .satisfies(issue -> {
                    assertThat(issue.getDetails()).contains(
                            "Gira 170 (14/05/2026): vehículo HUG-01 lleva 31 pasajeros y su capacidad es 30",
                            "Gira 176 (20/05/2026): vehículo FAB-1 lleva 20 pasajeros y su capacidad es 12");
                    assertThat(issue.getDetails()).noneMatch(detail -> detail.contains("175"));
                });
    }

    private Driver named(String name) {
        return Driver.builder().id(UUID.randomUUID()).name(name).status("ACTIVE").build();
    }

    private Vehicle fleet(String plate, int capacity, boolean available, VehicleType type) {
        return Vehicle.builder().id(UUID.randomUUID()).plate(plate).capacity(capacity).available(available).type(type).build();
    }

    private TourProcessingDraftTour scheduled(String number, int priority, int passengers, String date, String start, String end, Driver driver, Vehicle vehicle) {
        TourProcessingDraftTour scheduled = tour(number, priority, passengers, date);
        scheduled.setDepartureTime(LocalTime.parse(start));
        scheduled.setReturnTime(LocalTime.parse(end));
        assign(scheduled.addAssignment(part(passengers)), driver, vehicle);
        return scheduled;
    }

    private TourProcessingDraftDay manualDay(TourProcessingDraftTour scheduled, String date, double surplus, double overtime) {
        return TourProcessingDraftDay.builder()
                .assignment(scheduled.primaryAssignment())
                .date(LocalDate.parse(date))
                .startTime(scheduled.getDepartureTime())
                .endTime(scheduled.getReturnTime())
                .ordinaryHours(4.0)
                .surplusHours(surplus)
                .overtimeHours(overtime)
                .manuallyEdited(true)
                .build();
    }

    private TourProcessingDraftTour tour(String number, int priority, int passengers, String date) {
        return TourProcessingDraftTour.builder()
                .id(UUID.randomUUID())
                .rowNumber(Integer.parseInt(number))
                .number(number)
                .priority(priority)
                .passengers(passengers)
                .executingUnit("Escuela de Química")
                .responsible("Ana Pérez")
                .destination("Limón")
                .durationDays(1)
                .departureDate(LocalDate.parse(date))
                .departureTime(LocalTime.parse("13:00"))
                .returnDate(LocalDate.parse(date))
                .returnTime(LocalTime.parse("18:00"))
                .duplicateRank(1)
                .duplicateGroupSize(1)
                .build();
    }

    private TourProcessingDraftAssignment part(int passengers) {
        return TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(passengers).build();
    }

    private void assign(TourProcessingDraftAssignment part, Driver driver, Vehicle vehicle) {
        part.setDriver(driver.getName());
        part.setDriverId(driver.getId());
        if (vehicle != null) {
            part.setVehicle(vehicle.getPlate());
            part.setVehicleId(vehicle.getId());
        }
    }
}
