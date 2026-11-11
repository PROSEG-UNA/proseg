package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowResponseDto;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.DriverUnit;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.repository.BorrowedVehicleRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.ObservationKeywordRepository;
import com.proseg.msvc_transport.repository.TourSplitKeywordRepository;
import com.proseg.msvc_transport.repository.TourSplitSettingRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftAssignmentRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.security.CurrentUserResolver;
import com.proseg.msvc_transport.service.driverhours.ManualDays;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourProcessingRuleFilterTest {

    private static final int TOURS = 800;

    private final UUID draftId = UUID.randomUUID();
    private DraftTourStore tours;
    private TourProcessingDraftService service;
    private List<TourProcessingDraftTour> rows;
    private List<Driver> roster;
    private Vehicle bus;
    private TourProcessingDraftTour oversized;
    private TourProcessingDraftTour notApproved;
    private TourProcessingDraftTour incomplete;
    private TourProcessingDraftTour repeatedLeft;
    private TourProcessingDraftTour repeatedRight;

    @BeforeEach
    void setUp() {
        TourProcessingDraftRepository drafts = mock(TourProcessingDraftRepository.class);
        tours = mock(DraftTourStore.class);
        DriverRepository drivers = mock(DriverRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        TourProcessingDraftDayRepository days = mock(TourProcessingDraftDayRepository.class);
        ObservationKeywordRepository keywords = mock(ObservationKeywordRepository.class);
        DraftHoursService hours = mock(DraftHoursService.class);
        when(hours.manualDays(any())).thenReturn(ManualDays.NONE);
        when(days.findByAssignmentIdIn(any())).thenReturn(List.of());
        when(keywords.findAll()).thenReturn(List.of());
        CurrentUserResolver currentUser = mock(CurrentUserResolver.class);
        when(currentUser.resolve()).thenReturn("test");

        roster = new ArrayList<>();
        for (int index = 0; index < 16; index++) {
            roster.add(Driver.builder().id(UUID.randomUUID()).name("Chofer " + index).status("ACTIVE")
                    .organizationalUnit(DriverUnit.GENERAL_SERVICES).build());
        }
        bus = Vehicle.builder().id(UUID.randomUUID()).plate("301-481").capacity(30).available(true).build();
        Vehicle small = Vehicle.builder().id(UUID.randomUUID()).plate("301-100").capacity(12).available(true).build();
        when(drivers.findAll()).thenReturn(roster);
        when(vehicles.findAll()).thenReturn(List.of(bus, small));

        rows = new ArrayList<>();
        for (int index = 0; index < TOURS; index++) {
            rows.add(tour(roster.get(index % roster.size()), bus, 8, TourApprovalStatus.APPROVED, LocalDate.of(2026, 10, 1 + (index % 28)), "8" + index));
        }
        oversized = tour(roster.get(0), small, 40, TourApprovalStatus.APPROVED, LocalDate.of(2026, 10, 3), "9001");
        notApproved = tour(roster.get(0), small, 50, TourApprovalStatus.NOT_APPROVED, LocalDate.of(2026, 10, 3), "9002");
        incomplete = tour(roster.get(1), bus, 6, TourApprovalStatus.APPROVED, LocalDate.of(2026, 10, 4), "9003");
        incomplete.primaryAssignment().setDriver(null);
        incomplete.primaryAssignment().setDriverId(null);
        repeatedLeft = tour(roster.get(2), bus, 10, TourApprovalStatus.APPROVED, LocalDate.of(2026, 10, 5), "9004");
        repeatedRight = tour(roster.get(3), bus, 10, TourApprovalStatus.APPROVED, LocalDate.of(2026, 10, 5), "9005");
        rows.add(oversized);
        rows.add(notApproved);
        rows.add(incomplete);
        rows.add(repeatedLeft);
        rows.add(repeatedRight);

        TourProcessingDraft draft = TourProcessingDraft.builder().id(draftId).status(TourProcessingDraftStatus.DRAFT)
                .year(2026).month(10).businessDays(22).build();
        when(drafts.findById(draftId)).thenReturn(Optional.of(draft));
        when(tours.findPrimariesByDraftId(draftId)).thenReturn(rows);
        when(tours.findPage(any(), any(), any(), nullable(Collection.class), any())).thenAnswer(invocation -> {
            Collection<UUID> requested = invocation.getArgument(3);
            List<TourProcessingDraftTour> page = rows.stream()
                    .filter(tour -> requested == null || requested.contains(tour.getId()))
                    .limit(20)
                    .toList();
            return new PageImpl<>(page, PageRequest.of(0, 20), requested == null ? rows.size() : requested.size());
        });

        service = new TourProcessingDraftService(
                drafts,
                tours,
                mock(TourProcessingDraftAssignmentRepository.class),
                days,
                drivers,
                vehicles,
                mock(BorrowedVehicleRepository.class),
                hours,
                new InMemoryBusinessDays().service(),
                mock(OvertimeRequestReportService.class),
                currentUser,
                Validation.buildDefaultValidatorFactory().getValidator(),
                mock(JdbcTemplate.class),
                keywords,
                new TourSplitModeResolver(mock(TourSplitKeywordRepository.class), mock(TourSplitSettingRepository.class))
        );
    }

    @Test
    void filtersRowsByRuleOnTheServerAndKeepsTheOtherFilters() {
        service.findRows(draftId, null, Map.of(), PageRequest.of(0, 20));
        long plainStart = System.nanoTime();
        service.findRows(draftId, null, Map.of(), PageRequest.of(0, 20));
        double plainMs = elapsedMs(plainStart);

        long ruleStart = System.nanoTime();
        var any = service.findRows(draftId, "bus", Map.of("number", "9004"), "ANY", PageRequest.of(0, 20));
        double anyMs = elapsedMs(ruleStart);
        System.out.printf("findRows tours=%d plainMs=%.2f anyRuleMs=%.2f anyTotal=%d%n", rows.size(), plainMs, anyMs, any.getTotalElements());

        verify(tours, org.mockito.Mockito.times(2)).findPage(eq(draftId), isNull(), eq(Map.of()), isNull(), any());
        verify(tours).findPage(eq(draftId), eq("bus"), eq(Map.of("number", "9004")), any(), any());
        assertThat(any.getTotalElements()).isPositive();

        var multiple = service.findRows(draftId, null, Map.of(), "NEEDS_MULTIPLE_TRIPS", PageRequest.of(0, 20));
        assertThat(multiple.getContent()).extracting(TourProcessingDraftRowResponseDto::getId)
                .contains(oversized.getId())
                .doesNotContain(notApproved.getId());
        assertThat(multiple.getContent()).filteredOn(TourProcessingDraftRowResponseDto::getId, oversized.getId())
                .singleElement()
                .extracting(TourProcessingDraftRowResponseDto::isNeedsMultipleTrips)
                .isEqualTo(true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> ids = ArgumentCaptor.forClass(Collection.class);
        service.findRows(draftId, null, Map.of(), "PLATE_REPEATED_SAME_DAY", PageRequest.of(0, 20));
        service.findRows(draftId, null, Map.of(), "INCOMPLETE", PageRequest.of(0, 20));
        verify(tours, org.mockito.Mockito.atLeastOnce()).findPage(eq(draftId), isNull(), eq(Map.of()), ids.capture(), any());
        assertThat(ids.getAllValues()).filteredOn(matched -> matched != null)
                .anySatisfy(matched -> assertThat(matched).contains(repeatedLeft.getId(), repeatedRight.getId()))
                .anySatisfy(matched -> assertThat(matched).contains(incomplete.getId()).doesNotContain(oversized.getId()));
    }

    @Test
    void multipleTripsRuleIncludesDividedToursAndNeedsMultipleTripsRuleIncludesHalfDividedOnes() {
        TourProcessingDraftTour divided = tour(roster.get(0), bus, 10, TourApprovalStatus.APPROVED, LocalDate.of(2026, 10, 6), "9010");
        divided.addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(10)
                .driver(roster.get(1).getName()).driverId(roster.get(1).getId()).vehicle(bus.getPlate()).vehicleId(bus.getId()).build());
        TourProcessingDraftTour halfDivided = tour(roster.get(2), bus, 10, TourApprovalStatus.APPROVED, LocalDate.of(2026, 10, 7), "9011");
        halfDivided.addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(10).build());
        rows.add(divided);
        rows.add(halfDivided);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> ids = ArgumentCaptor.forClass(Collection.class);
        service.findRows(draftId, null, Map.of(), "MULTIPLE_TRIPS", PageRequest.of(0, 20));
        service.findRows(draftId, null, Map.of(), "NEEDS_MULTIPLE_TRIPS", PageRequest.of(0, 20));
        verify(tours, org.mockito.Mockito.atLeastOnce()).findPage(eq(draftId), isNull(), eq(Map.of()), ids.capture(), any());
        Collection<UUID> multiple = ids.getAllValues().get(0);
        Collection<UUID> needing = ids.getAllValues().get(1);

        assertThat(multiple).contains(divided.getId(), halfDivided.getId(), oversized.getId()).doesNotContain(notApproved.getId());
        assertThat(needing).contains(halfDivided.getId(), oversized.getId()).doesNotContain(divided.getId());
    }

    @Test
    void rowProblemsDescribeEveryProblemTourAndMatchTheAnyRuleFilter() {
        var problems = service.findRowProblems(draftId);

        assertThat(problems.get(oversized.getId())).extracting(problem -> problem.title()).contains("Necesita más de un viaje");
        assertThat(problems.get(incomplete.getId())).extracting(problem -> problem.title()).contains("Sin chofer o sin vehículo");
        assertThat(problems).doesNotContainKey(notApproved.getId());
        assertThat(problems.values().stream().flatMap(List::stream).map(problem -> problem.title()))
                .noneMatch(title -> title.contains("Placas repetidas"));

        service.findRows(draftId, null, Map.of(), "ANY", PageRequest.of(0, 20));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(tours, org.mockito.Mockito.atLeastOnce()).findPage(eq(draftId), isNull(), eq(Map.of()), ids.capture(), any());
        assertThat(ids.getAllValues()).filteredOn(matched -> matched != null)
                .anySatisfy(matched -> assertThat(matched).containsExactlyInAnyOrderElementsOf(problems.keySet()));
    }

    @Test
    void multipleTripFlagUsesThePlateWhenThereIsNoVehicleIdAndSkipsNotApprovedTours() {
        Vehicle bus = Vehicle.builder().id(UUID.randomUUID()).plate("301-481").capacity(30).available(true).build();
        TourProcessingDraftTour byPlate = tour(null, null, 40, TourApprovalStatus.APPROVED, LocalDate.of(2026, 10, 2), "1");
        byPlate.primaryAssignment().setVehicle(" 301-481 ");
        byPlate.primaryAssignment().setVehicleId(null);
        TourProcessingDraftTour rejected = tour(null, bus, 40, TourApprovalStatus.NOT_APPROVED, LocalDate.of(2026, 10, 2), "2");

        assertThat(TourProcessingDraftService.oversizedToursOf(List.of(byPlate, rejected), List.of(bus))).containsExactly(byPlate);
    }

    private static double elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000.0;
    }

    private static TourProcessingDraftTour tour(Driver driver, Vehicle vehicle, int passengers, TourApprovalStatus status, LocalDate date, String number) {
        TourProcessingDraftTour tour = TourProcessingDraftTour.builder()
                .id(UUID.randomUUID())
                .rowNumber(rowsNumber(number))
                .number(number)
                .passengers(passengers)
                .approvalStatus(status)
                .executingUnit("Escuela")
                .responsible("Ana")
                .destination("Limón")
                .durationDays(1)
                .priority(2)
                .departureDate(date)
                .returnDate(date)
                .departureTime(LocalTime.of(8, 0))
                .returnTime(LocalTime.of(12, 0))
                .duplicateRank(1)
                .duplicateGroupSize(1)
                .build();
        TourProcessingDraftAssignment assignment = TourProcessingDraftAssignment.builder()
                .id(UUID.randomUUID())
                .passengers(passengers)
                .driver(driver == null ? null : driver.getName())
                .driverId(driver == null ? null : driver.getId())
                .vehicle(vehicle == null ? null : vehicle.getPlate())
                .vehicleId(vehicle == null ? null : vehicle.getId())
                .build();
        tour.addAssignment(assignment);
        return tour;
    }

    private static int rowsNumber(String number) {
        String digits = number.replaceAll("\\D", "");
        if (digits.isEmpty()) return 1;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return 1;
        }
    }
}
