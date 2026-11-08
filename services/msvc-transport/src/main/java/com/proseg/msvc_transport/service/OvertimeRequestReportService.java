package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftAssignmentRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.service.driverhours.DriverBusinessDays;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OvertimeRequestReportService {

    private static final Locale SPANISH = Locale.forLanguageTag("es");
    private static final double HOURS_TOLERANCE = DriverHoursRules.HOURS_TOLERANCE;
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final TourProcessingDraftRepository draftRepository;
    private final TourProcessingDraftAssignmentRepository assignmentRepository;
    private final TourProcessingDraftDayRepository dayRepository;
    private final DriverRepository driverRepository;
    private final DriverBusinessDaysService businessDaysService;

    @Transactional(readOnly = true)
    public OvertimeRequestReportDto build(int year, int month) {
        if (month < 1 || month > 12) {
            throw TransportException.badRequest("INVALID_MONTH", "El mes debe estar entre 1 y 12");
        }
        TourProcessingDraft draft = draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(year, month)
                .orElseThrow(() -> new TransportException(
                        HttpStatus.NOT_FOUND,
                        "TOUR_PROCESSING_DRAFT_MONTH_NOT_FOUND",
                        "No hay un borrador de depuración para " + monthLabel(year, month)
                ));
        if (draft.getBusinessDays() == null) {
            throw TransportException.unprocessable(
                    "TOUR_PROCESSING_DRAFT_WITHOUT_PERIOD",
                    "El borrador de " + monthLabel(year, month) + " no tiene días hábiles definidos"
            );
        }
        return buildReport(draft, true, null);
    }

    @Transactional(readOnly = true)
    public OvertimeRequestReportDto buildSchedule(TourProcessingDraft draft) {
        requireBusinessDays(draft);
        return buildReport(draft, false, null);
    }

    private void requireBusinessDays(TourProcessingDraft draft) {
        if (draft.getBusinessDays() == null) {
            throw TransportException.unprocessable(
                    "TOUR_PROCESSING_DRAFT_WITHOUT_PERIOD",
                    "El borrador no tiene días hábiles definidos"
            );
        }
    }

    @Transactional(readOnly = true)
    public OvertimeRequestReportDto listDriverSummaries(TourProcessingDraft draft) {
        requireBusinessDays(draft);
        DriverBusinessDays businessDays = businessDaysService.forDraft(draft);
        Map<UUID, TourProcessingDraftDayRepository.DriverTotals> totals = dayRepository.summarizeByDriver(draft.getId()).stream()
                .collect(Collectors.toMap(TourProcessingDraftDayRepository.DriverTotals::getDriverId, value -> value));
        Collator collator = Collator.getInstance(SPANISH);
        List<OvertimeRequestReportDto.DriverBlock> drivers = driverRepository.findAll().stream()
                .sorted(Comparator.comparing(Driver::getName, collator))
                .map(driver -> {
                    var total = totals.get(driver.getId());
                    return OvertimeRequestReportDto.DriverBlock.builder().id(driver.getId()).name(driver.getName())
                            .organizationalUnit(driver.getOrganizationalUnit().name())
                            .status(ACTIVE_STATUS.equalsIgnoreCase(driver.getStatus()) ? ACTIVE_STATUS : "INACTIVE")
                            .businessDays(businessDays.businessDaysOf(driver))
                            .surplusCap(businessDays.surplusCapOf(driver))
                            .surplusHours(total == null ? 0 : round(total.getSurplusHours()))
                            .overtimeHours(total == null ? 0 : round(total.getOvertimeHours())).tours(List.of()).build();
                }).toList();
        return OvertimeRequestReportDto.builder().year(draft.getYear()).month(draft.getMonth())
                .businessDays(draft.getBusinessDays()).surplusCapPerDriver(businessDays.defaultSurplusCap())
                .drivers(drivers).build();
    }

    @Transactional(readOnly = true)
    public Page<OvertimeRequestReportDto.TourBlock> buildDriverPage(TourProcessingDraft draft, UUID driverId, Pageable pageable) {
        requireBusinessDays(draft);
        Page<TourProcessingDraftAssignment> page = assignmentRepository.findDriverPage(draft.getId(), driverId, pageable);
        if (page.getTotalElements() == 0 && !driverRepository.existsById(driverId)) {
            throw TransportException.notFound("Chofer", driverId.toString());
        }
        Map<UUID, List<TourProcessingDraftDay>> days = page.isEmpty() ? Map.of()
                : dayRepository.findByAssignmentIdIn(page.map(TourProcessingDraftAssignment::getId).getContent()).stream()
                        .collect(Collectors.groupingBy(day -> day.getAssignment().getId()));
        return page.map(assignment -> toTourBlock(assignment, days.getOrDefault(assignment.getId(), List.of()), false));
    }

    @Transactional(readOnly = true)
    public OvertimeRequestReportDto buildDailySchedule(TourProcessingDraft draft, java.time.LocalDate date) {
        requireBusinessDays(draft);
        DriverBusinessDays businessDays = businessDaysService.forDraft(draft);
        List<TourProcessingDraftDay> days = dayRepository.findByDraftIdAndDate(draft.getId(), date);
        Map<UUID, List<TourProcessingDraftDay>> byAssignment = days.stream()
                .collect(Collectors.groupingBy(day -> day.getAssignment().getId()));
        Map<UUID, List<TourProcessingDraftAssignment>> byDriver = days.stream().map(TourProcessingDraftDay::getAssignment).distinct()
                .collect(Collectors.groupingBy(TourProcessingDraftAssignment::getDriverId));
        Collator collator = Collator.getInstance(SPANISH);
        List<OvertimeRequestReportDto.DriverBlock> drivers = driverRepository.findAll().stream()
                .filter(driver -> ACTIVE_STATUS.equalsIgnoreCase(driver.getStatus()) || byDriver.containsKey(driver.getId()))
                .sorted(Comparator.comparing(Driver::getName, collator))
                .map(driver -> toDriverBlock(driver, byDriver.getOrDefault(driver.getId(), List.of()), byAssignment, false, businessDays)).toList();
        Map<UUID, TourProcessingDraftDayRepository.DriverTotals> monthlyTotals = dayRepository.summarizeByDriver(draft.getId()).stream()
                .collect(Collectors.toMap(TourProcessingDraftDayRepository.DriverTotals::getDriverId, value -> value));
        drivers.forEach(driver -> {
            var totals = monthlyTotals.get(driver.getId());
            driver.setSurplusHours(totals == null ? 0 : round(totals.getSurplusHours()));
            driver.setOvertimeHours(totals == null ? 0 : round(totals.getOvertimeHours()));
        });
        return OvertimeRequestReportDto.builder().year(draft.getYear()).month(draft.getMonth())
                .businessDays(draft.getBusinessDays()).surplusCapPerDriver(businessDays.defaultSurplusCap())
                .drivers(drivers).build();
    }

    private OvertimeRequestReportDto buildReport(TourProcessingDraft draft, boolean onlyExtraHours, UUID onlyDriverId) {
        DriverBusinessDays businessDays = businessDaysService.forDraft(draft);
        List<Driver> catalog = driverRepository.findAll().stream()
                .filter(driver -> onlyDriverId == null || onlyDriverId.equals(driver.getId()))
                .toList();
        Set<UUID> catalogIds = catalog.stream().map(Driver::getId).collect(Collectors.toSet());
        List<TourProcessingDraftAssignment> rows = assignmentRepository.findActiveByDraftId(draft.getId()).stream()
                .filter(TourProcessingDraftAssignment::isActive)
                .filter(row -> row.getDriverId() != null && catalogIds.contains(row.getDriverId()))
                .toList();
        Map<UUID, List<TourProcessingDraftDay>> daysByRowId = rows.isEmpty()
                ? Map.of()
                : dayRepository.findByAssignmentIdIn(rows.stream().map(TourProcessingDraftAssignment::getId).toList()).stream()
                .collect(Collectors.groupingBy(day -> day.getAssignment().getId()));
        Map<UUID, List<TourProcessingDraftAssignment>> rowsByDriverId = rows.stream()
                .collect(Collectors.groupingBy(TourProcessingDraftAssignment::getDriverId));

        Collator collator = Collator.getInstance(SPANISH);
        List<OvertimeRequestReportDto.DriverBlock> drivers = catalog.stream()
                .filter(driver -> ACTIVE_STATUS.equalsIgnoreCase(driver.getStatus()) || rowsByDriverId.containsKey(driver.getId()))
                .sorted(Comparator.comparing(Driver::getName, collator))
                .map(driver -> toDriverBlock(driver, rowsByDriverId.getOrDefault(driver.getId(), List.of()), daysByRowId, onlyExtraHours, businessDays))
                .toList();

        return OvertimeRequestReportDto.builder()
                .year(draft.getYear())
                .month(draft.getMonth())
                .businessDays(draft.getBusinessDays())
                .surplusCapPerDriver(businessDays.defaultSurplusCap())
                .drivers(drivers)
                .build();
    }

    private OvertimeRequestReportDto.DriverBlock toDriverBlock(
            Driver driver,
            List<TourProcessingDraftAssignment> driverRows,
            Map<UUID, List<TourProcessingDraftDay>> daysByRowId,
            boolean onlyExtraHours,
            DriverBusinessDays businessDays
    ) {
        List<OvertimeRequestReportDto.TourBlock> tours = driverRows.stream()
                .sorted(Comparator.comparing(TourProcessingDraftAssignment::getDepartureDate).thenComparing(TourProcessingDraftAssignment::getNumber))
                .map(row -> toTourBlock(row, daysByRowId.getOrDefault(row.getId(), List.of()), onlyExtraHours))
                .filter(tour -> !tour.getDays().isEmpty())
                .toList();
        return OvertimeRequestReportDto.DriverBlock.builder()
                .id(driver.getId())
                .name(driver.getName())
                .businessDays(businessDays.businessDaysOf(driver))
                .surplusCap(businessDays.surplusCapOf(driver))
                .surplusHours(round(tours.stream().flatMap(tour -> tour.getDays().stream()).mapToDouble(OvertimeRequestReportDto.TourDay::getSurplusHours).sum()))
                .overtimeHours(round(tours.stream().flatMap(tour -> tour.getDays().stream()).mapToDouble(OvertimeRequestReportDto.TourDay::getOvertimeHours).sum()))
                .tours(tours)
                .build();
    }

    private OvertimeRequestReportDto.TourBlock toTourBlock(TourProcessingDraftAssignment row, List<TourProcessingDraftDay> days, boolean onlyExtraHours) {
        TourProcessingDraftTour tour = row.getTour();
        List<OvertimeRequestReportDto.TourDay> tourDays = days.stream()
                .sorted(Comparator.comparing(TourProcessingDraftDay::getDate))
                .filter(day -> !onlyExtraHours || day.getSurplusHours() + day.getOvertimeHours() > HOURS_TOLERANCE)
                .map(this::toTourDay)
                .toList();
        return OvertimeRequestReportDto.TourBlock.builder()
                .id(tour.getId())
                .number(tour.getNumber())
                .priority(tour.getPriority())
                .executingUnit(tour.getExecutingUnit())
                .responsible(tour.getResponsible())
                .departureDate(tour.getDepartureDate())
                .returnDate(tour.getReturnDate())
                .destination(tour.getDestination())
                .vehicle(row.getResourceType() == ResourceType.BORROWED_VEHICLE
                        ? (row.getBorrowedVehicle() == null ? null : row.getBorrowedVehicle().getPlate())
                        : row.getVehicle())
                .passengers(passengersOfDriver(row))
                .days(tourDays)
                .build();
    }

    private static Integer passengersOfDriver(TourProcessingDraftAssignment row) {
        if (row.getDriverId() == null || row.getTour().getAssignments().size() < 2) {
            return row.getPassengers();
        }
        return row.getTour().getAssignments().stream()
                .filter(part -> row.getDriverId().equals(part.getDriverId()))
                .mapToInt(part -> part.getPassengers() == null ? 0 : part.getPassengers())
                .sum();
    }

    private OvertimeRequestReportDto.TourDay toTourDay(TourProcessingDraftDay day) {
        return OvertimeRequestReportDto.TourDay.builder()
                .date(day.getDate())
                .start(DriverHoursRules.hourOf(day.getStartTime()))
                .end(DriverHoursRules.hourOf(day.getEndTime()))
                .ordinaryHours(round(day.getOrdinaryHours()))
                .surplusHours(round(day.getSurplusHours()))
                .overtimeHours(round(day.getOvertimeHours()))
                .build();
    }

    private String monthLabel(int year, int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, SPANISH) + " de " + year;
    }

    private double round(double hours) {
        return Math.round(hours * 100) / 100.0;
    }
}
