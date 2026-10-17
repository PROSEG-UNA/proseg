package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.DriverMonthAccrualRequestDto;
import com.proseg.msvc_transport.dto.response.DriverMonthResponseDto;
import com.proseg.msvc_transport.dto.response.DriverMonthSurplusCapResponseDto;
import com.proseg.msvc_transport.entity.CleaningDraft;
import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.CleaningDraftRowDay;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.DriverMonth;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.CleaningDraftRepository;
import com.proseg.msvc_transport.repository.CleaningDraftRowDayRepository;
import com.proseg.msvc_transport.repository.DriverMonthRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.service.driverhours.DayAllocation;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.DriverMonthTotals;
import com.proseg.msvc_transport.service.driverhours.DriverWorkload;
import com.proseg.msvc_transport.service.driverhours.TourAllocation;
import com.proseg.msvc_transport.service.driverhours.TourDaySpan;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monthly, per-driver counters (ordinary/surplus/overtime hours, weekends worked).
 * Rows are created lazily: a given (driver, year, month) only gets a row the
 * first time hours are accrued for that period, never upfront for every driver.
 *
 * {@link #syncWithDraft} also owns the per-day breakdown ({@link CleaningDraftRowDay}):
 * it recomputes the automatic ones and aggregates the monthly counters from whatever
 * is currently persisted, so a manual correction on a day is reflected here too.
 */
@Service
@RequiredArgsConstructor
public class DriverMonthService {

    private final DriverMonthRepository driverMonthRepository;
    private final DriverRepository driverRepository;
    private final CleaningDraftRepository draftRepository;
    private final CleaningDraftRowDayRepository dayRepository;

    @Transactional(readOnly = true)
    public List<DriverMonthResponseDto> findHistory(UUID driverId) {
        return driverMonthRepository.findByDriverIdOrderByYearAscMonthAsc(driverId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DriverMonthResponseDto findCurrent(UUID driverId) {
        LocalDate now = LocalDate.now();
        return driverMonthRepository.findByDriverIdAndYearAndMonth(driverId, now.getYear(), now.getMonthValue())
                .map(this::toResponse)
                .orElseThrow(() -> TransportException.notFound("Registro mensual del chofer", driverId.toString()));
    }

    @Transactional
    public DriverMonthResponseDto accrue(UUID driverId, int year, int month, DriverMonthAccrualRequestDto request) {
        DriverMonth driverMonth = getOrCreate(driverId, year, month);
        driverMonth.setOrdinaryHoursAccrued(driverMonth.getOrdinaryHoursAccrued() + request.getOrdinaryHoursDelta());
        driverMonth.setSurplusHoursAccrued(driverMonth.getSurplusHoursAccrued() + request.getSurplusHoursDelta());
        driverMonth.setOvertimeHoursAccrued(driverMonth.getOvertimeHoursAccrued() + request.getOvertimeHoursDelta());
        driverMonth.setWeekendsWorked(driverMonth.getWeekendsWorked() + request.getWeekendsWorkedDelta());
        return toResponse(driverMonthRepository.save(driverMonth));
    }

    /**
     * One row per active driver for the given period: a real DriverMonth when it exists,
     * otherwise a zeroed placeholder (hasRecord = false) — no row is created by reading this.
     */
    @Transactional(readOnly = true)
    public List<DriverMonthResponseDto> findAllForPeriod(int year, int month) {
        Map<UUID, DriverMonth> byDriverId = driverMonthRepository.findByYearAndMonth(year, month).stream()
                .collect(Collectors.toMap(dm -> dm.getDriver().getId(), Function.identity()));
        return driverRepository.findAll().stream()
                .map(driver -> {
                    DriverMonth driverMonth = byDriverId.get(driver.getId());
                    return driverMonth != null ? toResponse(driverMonth) : emptyResponse(driver, year, month);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public DriverMonthSurplusCapResponseDto findSurplusCap(int year, int month) {
        return draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(year, month)
                .filter(draft -> draft.getBusinessDays() != null)
                .map(draft -> DriverMonthSurplusCapResponseDto.builder()
                        .businessDays(draft.getBusinessDays())
                        .surplusHoursPerDriver(DriverHoursRules.surplusCap(draft.getBusinessDays()))
                        .build())
                .orElseGet(() -> DriverMonthSurplusCapResponseDto.builder().build());
    }

    @Transactional
    public void syncWithDraft(CleaningDraft draft, List<CleaningDraftRow> rows) {
        if (draft.getYear() == null || draft.getMonth() == null || draft.getBusinessDays() == null) {
            return;
        }
        int year = draft.getYear();
        int month = draft.getMonth();
        double surplusCap = DriverHoursRules.surplusCap(draft.getBusinessDays());
        Set<LocalDate> monthWeekends = DriverHoursRules.monthWeekends(year, month);
        List<Driver> catalog = driverRepository.findAll();
        Map<UUID, Driver> driversById = catalog.stream().collect(Collectors.toMap(Driver::getId, Function.identity()));
        Map<UUID, DriverWorkload> workloads = DriverWorkload.fromDraftRows(rows, DriverHoursRules.catalogByKey(catalog));
        Map<UUID, DriverMonth> existingByDriverId = driverMonthRepository.findByYearAndMonth(year, month).stream()
                .collect(Collectors.toMap(dm -> dm.getDriver().getId(), Function.identity()));
        Map<String, List<CleaningDraftRow>> siblingsByNumber = rows.stream()
                .collect(Collectors.groupingBy(this::numberKey));

        clearDaysForRowsWithoutDriver(rows, workloads);

        List<DriverMonth> changed = new ArrayList<>();
        workloads.forEach((driverId, workload) -> {
            List<CleaningDraftRowDay> primaryDays = syncDays(workload, surplusCap, siblingsByNumber);
            DriverMonth driverMonth = existingByDriverId.remove(driverId);
            if (driverMonth == null) {
                driverMonth = DriverMonth.builder().driver(driversById.get(driverId)).year(year).month(month).build();
            }
            applyTotals(driverMonth, totalsFromDays(primaryDays, monthWeekends));
            changed.add(driverMonth);
        });
        existingByDriverId.values().forEach(driverMonth -> {
            applyTotals(driverMonth, new DriverMonthTotals(0, 0, 0, 0));
            changed.add(driverMonth);
        });
        driverMonthRepository.saveAll(changed);
    }

    @Transactional
    public void recomputeDriverMonth(CleaningDraft draft, List<CleaningDraftRow> rows, String driverName) {
        if (draft.getYear() == null || draft.getMonth() == null) {
            return;
        }
        String key = DriverHoursRules.driverKey(driverName);
        Driver driver = driverRepository.findAll().stream()
                .filter(candidate -> DriverHoursRules.driverKey(candidate.getName()).equals(key))
                .findFirst()
                .orElse(null);
        if (driver == null) {
            return;
        }
        List<UUID> primaryRowIds = rows.stream()
                .filter(DriverHoursRules::isPrimary)
                .filter(row -> DriverHoursRules.driverKey(row.getDriver()).equals(key))
                .map(CleaningDraftRow::getId)
                .toList();
        List<CleaningDraftRowDay> days = dayRepository.findByRowIdIn(primaryRowIds);
        Set<LocalDate> monthWeekends = DriverHoursRules.monthWeekends(draft.getYear(), draft.getMonth());
        DriverMonth driverMonth = getOrCreate(driver.getId(), draft.getYear(), draft.getMonth());
        applyTotals(driverMonth, totalsFromDays(days, monthWeekends));
        driverMonthRepository.save(driverMonth);
    }

    private void clearDaysForRowsWithoutDriver(List<CleaningDraftRow> rows, Map<UUID, DriverWorkload> workloads) {
        Set<UUID> rowIdsWithDriver = workloads.values().stream()
                .flatMap(workload -> workload.tours().stream())
                .map(CleaningDraftRow::getId)
                .collect(Collectors.toSet());
        List<UUID> rowIdsWithoutDriver = rows.stream()
                .map(CleaningDraftRow::getId)
                .filter(id -> !rowIdsWithDriver.contains(id))
                .toList();
        if (!rowIdsWithoutDriver.isEmpty()) {
            dayRepository.deleteByRowIdIn(rowIdsWithoutDriver);
        }
    }

    private List<CleaningDraftRowDay> syncDays(
            DriverWorkload workload,
            double surplusCap,
            Map<String, List<CleaningDraftRow>> siblingsByNumber
    ) {
        List<CleaningDraftRowDay> primaryDays = new ArrayList<>();
        for (TourAllocation allocation : workload.allocations(surplusCap)) {
            CleaningDraftRow primary = allocation.tour();
            primaryDays.addAll(syncRowDays(primary, allocation.days()));
            for (CleaningDraftRow sibling : siblingsByNumber.getOrDefault(numberKey(primary), List.of())) {
                if (!sibling.getId().equals(primary.getId())) {
                    syncRowDays(sibling, allocation.days());
                }
            }
        }
        return primaryDays;
    }

    private List<CleaningDraftRowDay> syncRowDays(CleaningDraftRow row, List<DayAllocation> freshDays) {
        Map<LocalDate, CleaningDraftRowDay> existingByDate = dayRepository.findByRowIdOrderByDateAsc(row.getId()).stream()
                .collect(Collectors.toMap(CleaningDraftRowDay::getDate, Function.identity(), (a, b) -> a, HashMap::new));

        Set<LocalDate> freshDates = new HashSet<>();
        List<CleaningDraftRowDay> toSave = new ArrayList<>();
        for (DayAllocation allocation : freshDays) {
            TourDaySpan span = allocation.span();
            freshDates.add(span.date());
            CleaningDraftRowDay existing = existingByDate.get(span.date());
            if (existing != null && existing.isManuallyEdited()) {
                toSave.add(existing);
                continue;
            }
            CleaningDraftRowDay day = existing != null ? existing : CleaningDraftRowDay.builder().row(row).date(span.date()).build();
            day.setStartTime(DriverHoursRules.timeOf(span.start()));
            day.setEndTime(DriverHoursRules.timeOf(span.end()));
            day.setOrdinaryHours(round(span.ordinaryHours()));
            day.setSurplusHours(round(allocation.surplusHours()));
            day.setOvertimeHours(round(allocation.overtimeHours()));
            day.setManuallyEdited(false);
            toSave.add(day);
        }

        List<UUID> staleIds = existingByDate.values().stream()
                .filter(day -> !freshDates.contains(day.getDate()))
                .map(CleaningDraftRowDay::getId)
                .toList();
        if (!staleIds.isEmpty()) {
            dayRepository.deleteAllById(staleIds);
        }
        return dayRepository.saveAll(toSave);
    }

    private DriverMonthTotals totalsFromDays(List<CleaningDraftRowDay> days, Set<LocalDate> monthWeekends) {
        double ordinary = 0;
        double surplus = 0;
        double overtime = 0;
        Set<LocalDate> weekendsWorked = new HashSet<>();
        for (CleaningDraftRowDay day : days) {
            ordinary += day.getOrdinaryHours();
            surplus += day.getSurplusHours();
            overtime += day.getOvertimeHours();
            if (DriverHoursRules.isWeekend(day.getDate())) {
                boolean worked = day.getOrdinaryHours() + day.getSurplusHours() + day.getOvertimeHours() > DriverHoursRules.HOURS_TOLERANCE;
                LocalDate weekend = DriverHoursRules.weekendKey(day.getDate());
                if (worked && monthWeekends.contains(weekend)) {
                    weekendsWorked.add(weekend);
                }
            }
        }
        return new DriverMonthTotals(round(ordinary), round(surplus), round(overtime), weekendsWorked.size());
    }

    private String numberKey(CleaningDraftRow row) {
        return row.getNumber().toLowerCase(Locale.ROOT);
    }

    private double round(double hours) {
        return Math.round(hours * 100) / 100.0;
    }

    private void applyTotals(DriverMonth driverMonth, DriverMonthTotals totals) {
        driverMonth.setOrdinaryHoursAccrued(totals.ordinaryHours());
        driverMonth.setSurplusHoursAccrued(totals.surplusHours());
        driverMonth.setOvertimeHoursAccrued(totals.overtimeHours());
        driverMonth.setWeekendsWorked(totals.weekendsWorked());
    }

    private DriverMonth getOrCreate(UUID driverId, int year, int month) {
        return driverMonthRepository.findByDriverIdAndYearAndMonth(driverId, year, month)
                .orElseGet(() -> createNew(driverId, year, month));
    }

    private DriverMonth createNew(UUID driverId, int year, int month) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> TransportException.notFound("Chofer", driverId.toString()));
        DriverMonth driverMonth = DriverMonth.builder()
                .driver(driver)
                .year(year)
                .month(month)
                .build();
        try {
            return driverMonthRepository.save(driverMonth);
        } catch (DataIntegrityViolationException alreadyCreatedConcurrently) {
            return driverMonthRepository.findByDriverIdAndYearAndMonth(driverId, year, month)
                    .orElseThrow(() -> alreadyCreatedConcurrently);
        }
    }

    private DriverMonthResponseDto toResponse(DriverMonth entity) {
        return DriverMonthResponseDto.builder()
                .id(entity.getId())
                .driverId(entity.getDriver().getId())
                .driverName(entity.getDriver().getName())
                .year(entity.getYear())
                .month(entity.getMonth())
                .ordinaryHoursAccrued(entity.getOrdinaryHoursAccrued())
                .surplusHoursAccrued(entity.getSurplusHoursAccrued())
                .overtimeHoursAccrued(entity.getOvertimeHoursAccrued())
                .weekendsWorked(entity.getWeekendsWorked())
                .hasRecord(true)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private DriverMonthResponseDto emptyResponse(Driver driver, int year, int month) {
        return DriverMonthResponseDto.builder()
                .driverId(driver.getId())
                .driverName(driver.getName())
                .year(year)
                .month(month)
                .ordinaryHoursAccrued(0D)
                .surplusHoursAccrued(0D)
                .overtimeHoursAccrued(0D)
                .weekendsWorked(0)
                .hasRecord(false)
                .build();
    }
}
