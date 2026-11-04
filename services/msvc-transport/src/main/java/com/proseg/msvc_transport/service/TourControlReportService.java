package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.tourprocessing.TourControlReportDto;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.DriverUnit;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.OrdinaryWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class TourControlReportService {

    public static final String DRIVER_SECTION = "DRIVER";
    public static final String OTHER_UNIT_SECTION = "OTHER_UNIT";
    public static final String EXTERNAL_SECTION = "EXTERNAL";
    public static final String UNATTENDED_SECTION = "UNATTENDED";

    private static final String DRAFT_RESOURCE = "Borrador de depuración";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final Locale SPANISH = Locale.forLanguageTag("es");
    private static final Comparator<TourProcessingDraftTour> TOUR_ORDER = Comparator
            .comparing(TourProcessingDraftTour::getDepartureDate)
            .thenComparing(TourProcessingDraftTour::getDepartureTime)
            .thenComparing(TourProcessingDraftTour::getRowNumber);

    private final TourProcessingDraftRepository draftRepository;
    private final DraftTourStore tourStore;
    private final TourProcessingDraftDayRepository dayRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public TourControlReportDto report(UUID draftId) {
        return build(draftRepository.findById(draftId)
                .orElseThrow(() -> TransportException.notFound(DRAFT_RESOURCE, draftId.toString())));
    }

    @Transactional(readOnly = true)
    public TourControlReportDto reportForMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw TransportException.badRequest("INVALID_MONTH", "El mes debe estar entre 1 y 12");
        }
        return build(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(year, month)
                .orElseThrow(() -> new TransportException(
                        HttpStatus.NOT_FOUND,
                        "TOUR_PROCESSING_DRAFT_MONTH_NOT_FOUND",
                        "No hay un borrador de depuración para "
                                + Month.of(month).getDisplayName(TextStyle.FULL, SPANISH) + " de " + year
                )));
    }

    private TourControlReportDto build(TourProcessingDraft draft) {
        if (draft.getYear() == null || draft.getMonth() == null || draft.getBusinessDays() == null) {
            throw TransportException.unprocessable("TOUR_PROCESSING_DRAFT_WITHOUT_PERIOD", "El borrador no tiene mes ni días hábiles definidos");
        }
        List<Driver> catalog = driverRepository.findAll();
        Map<UUID, Driver> driversById = DriverHoursRules.driversById(catalog);
        Map<UUID, Vehicle> vehiclesById = DriverHoursRules.vehiclesById(vehicleRepository.findAll());
        List<TourProcessingDraftTour> tours = tourStore.findPrimariesByDraftId(draft.getId()).stream().sorted(TOUR_ORDER).toList();
        List<UUID> attendedIds = tours.stream()
                .filter(TourProcessingDraftTour::isApproved)
                .flatMap(tour -> tour.getAssignments().stream())
                .map(TourProcessingDraftAssignment::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<UUID, List<TourProcessingDraftDay>> daysByAssignment = attendedIds.isEmpty() ? Map.of()
                : dayRepository.findByAssignmentIdIn(attendedIds).stream()
                .collect(Collectors.groupingBy(day -> day.getAssignment().getId()));

        List<Placement> placements = new ArrayList<>();
        Map<Driver, HoursTotals> scheduledDrivers = new LinkedHashMap<>();
        for (TourProcessingDraftTour tour : tours) {
            if (!tour.isApproved() || tour.getAssignments().isEmpty()) {
                placements.add(new Placement(tourEntry(UNATTENDED_SECTION, tour, null, null), null));
                continue;
            }
            for (TourProcessingDraftAssignment part : tour.getAssignments()) {
                placements.addAll(place(tour, part, driversById, vehiclesById, daysByAssignment, scheduledDrivers));
            }
        }

        Collator collator = Collator.getInstance(SPANISH);
        List<Driver> roster = catalog.stream()
                .filter(driver -> driver.getOrganizationalUnit() == DriverUnit.GENERAL_SERVICES)
                .filter(driver -> ACTIVE_STATUS.equalsIgnoreCase(driver.getStatus()) || scheduledDrivers.containsKey(driver))
                .sorted(Comparator.comparing(Driver::getName, collator))
                .toList();
        Map<Driver, Integer> rosterIndex = IntStream.range(0, roster.size()).boxed()
                .collect(Collectors.toMap(roster::get, index -> index));
        List<TourControlReportDto.Entry> entries = placements.stream()
                .map(placement -> {
                    if (placement.driver() != null) {
                        placement.entry().setDriverIndex(rosterIndex.get(placement.driver()));
                    }
                    return placement.entry();
                })
                .toList();

        return TourControlReportDto.builder()
                .year(draft.getYear())
                .month(draft.getMonth())
                .businessDays(draft.getBusinessDays())
                .weekdays(weekdays(draft.getYear(), draft.getMonth()))
                .drivers(roster.stream().map(driver -> toDriver(driver, scheduledDrivers.get(driver))).toList())
                .entries(entries)
                .build();
    }

    private List<Placement> place(
            TourProcessingDraftTour tour,
            TourProcessingDraftAssignment part,
            Map<UUID, Driver> driversById,
            Map<UUID, Vehicle> vehiclesById,
            Map<UUID, List<TourProcessingDraftDay>> daysByAssignment,
            Map<Driver, HoursTotals> scheduledDrivers
    ) {
        String plate = plateOf(part, vehiclesById);
        if (part.getResourceType() == ResourceType.EXTERNAL) {
            return List.of(new Placement(tourEntry(EXTERNAL_SECTION, tour, part, plate), null));
        }
        Driver driver = DriverHoursRules.assignedDriver(part, driversById);
        if (driver != null && driver.getOrganizationalUnit() == DriverUnit.GENERAL_SERVICES) {
            scheduledDrivers.computeIfAbsent(driver, ignored -> new HoursTotals()).add(storedDays(part, daysByAssignment));
            return dayEntries(DRIVER_SECTION, tour, part, driver.getName(), plate, daysByAssignment).stream()
                    .map(entry -> new Placement(entry, driver))
                    .toList();
        }
        String driverName = driver != null ? driver.getName() : trimToNull(part.getDriver());
        if (driverName != null) {
            return dayEntries(OTHER_UNIT_SECTION, tour, part, driverName, plate, daysByAssignment).stream()
                    .map(entry -> new Placement(entry, null))
                    .toList();
        }
        return List.of(new Placement(tourEntry(UNATTENDED_SECTION, tour, part, plate), null));
    }

    private List<TourControlReportDto.Entry> dayEntries(
            String section,
            TourProcessingDraftTour tour,
            TourProcessingDraftAssignment part,
            String driverName,
            String plate,
            Map<UUID, List<TourProcessingDraftDay>> daysByAssignment
    ) {
        List<TourProcessingDraftDay> stored = storedDays(part, daysByAssignment);
        if (!stored.isEmpty()) {
            return stored.stream()
                    .sorted(Comparator.comparing(TourProcessingDraftDay::getDate))
                    .map(day -> {
                        double start = DriverHoursRules.hourOf(day.getStartTime());
                        double end = DriverHoursRules.hourOf(day.getEndTime());
                        return dayEntry(section, tour, part, driverName, plate, day.getDate(), start, end < start ? end + 24 : end,
                                hoursOf(day));
                    })
                    .toList();
        }
        return DriverHoursRules.daySpans(part).stream()
                .map(span -> dayEntry(section, tour, part, driverName, plate, span.date(), span.start(), span.end(), null))
                .toList();
    }

    private TourControlReportDto.Entry dayEntry(
            String section,
            TourProcessingDraftTour tour,
            TourProcessingDraftAssignment part,
            String driverName,
            String plate,
            LocalDate date,
            double start,
            double end,
            Double hours
    ) {
        TourControlReportDto.Entry entry = tourEntry(section, tour, part, plate);
        entry.setDate(date);
        entry.setDriver(driverName);
        entry.setStart(start);
        entry.setEnd(end);
        entry.setHours(hours);
        return entry;
    }

    private TourControlReportDto.Entry tourEntry(String section, TourProcessingDraftTour tour, TourProcessingDraftAssignment part, String plate) {
        return TourControlReportDto.Entry.builder()
                .section(section)
                .date(tour.getDepartureDate())
                .driver(part == null ? null : trimToNull(part.getDriver()))
                .plate(plate)
                .destination(tour.getDestination())
                .responsible(tour.getResponsible())
                .departureDate(tour.getDepartureDate())
                .returnDate(tour.getReturnDate())
                .start(tour.getDepartureTime() == null ? null : DriverHoursRules.hourOf(tour.getDepartureTime()))
                .end(tour.getReturnTime() == null ? null : DriverHoursRules.hourOf(tour.getReturnTime()))
                .passengers(part == null ? tour.getPassengers() : part.getPassengers())
                .number(tour.getNumber())
                .priority(tour.getPriority())
                .build();
    }

    private String plateOf(TourProcessingDraftAssignment part, Map<UUID, Vehicle> vehiclesById) {
        if (part.getResourceType() == ResourceType.BORROWED_VEHICLE) {
            return part.getBorrowedVehicle() == null ? null : part.getBorrowedVehicle().getPlate();
        }
        Vehicle vehicle = part.getResourceType() == ResourceType.EXTERNAL ? null : DriverHoursRules.assignedVehicle(part, vehiclesById);
        return vehicle != null ? vehicle.getPlate() : trimToNull(part.getVehicle());
    }

    private List<TourProcessingDraftDay> storedDays(TourProcessingDraftAssignment part, Map<UUID, List<TourProcessingDraftDay>> daysByAssignment) {
        return part.getId() == null ? List.of() : daysByAssignment.getOrDefault(part.getId(), List.of());
    }

    private Double hoursOf(TourProcessingDraftDay day) {
        return round(surplusOf(day) + overtimeOf(day));
    }

    private static double surplusOf(TourProcessingDraftDay day) {
        return day.getSurplusHours() == null ? 0 : day.getSurplusHours();
    }

    private static double overtimeOf(TourProcessingDraftDay day) {
        return day.getOvertimeHours() == null ? 0 : day.getOvertimeHours();
    }

    private static double round(double hours) {
        return Math.round(hours * 100) / 100.0;
    }

    private TourControlReportDto.Driver toDriver(Driver driver, HoursTotals totals) {
        OrdinaryWindow window = OrdinaryWindow.of(driver);
        boolean known = totals == null || totals.complete;
        return TourControlReportDto.Driver.builder()
                .name(driver.getName())
                .ordinaryStart(window.start())
                .ordinaryEnd(window.end())
                .surplusHours(known ? round(totals == null ? 0 : totals.surplus) : null)
                .overtimeHours(known ? round(totals == null ? 0 : totals.overtime) : null)
                .build();
    }

    private int weekdays(int year, int month) {
        YearMonth period = YearMonth.of(year, month);
        return (int) IntStream.rangeClosed(1, period.lengthOfMonth())
                .mapToObj(period::atDay)
                .filter(day -> day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Placement(TourControlReportDto.Entry entry, Driver driver) {
    }

    private static final class HoursTotals {
        private double surplus;
        private double overtime;
        private boolean complete = true;

        private void add(List<TourProcessingDraftDay> days) {
            if (days.isEmpty()) {
                complete = false;
            }
            days.forEach(day -> {
                surplus += surplusOf(day);
                overtime += overtimeOf(day);
            });
        }
    }
}
