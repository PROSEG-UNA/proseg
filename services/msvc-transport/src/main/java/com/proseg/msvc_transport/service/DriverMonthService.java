package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.response.DriverMonthResponseDto;
import com.proseg.msvc_transport.dto.response.DriverMonthSurplusCapResponseDto;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.DriverMonthTotals;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverMonthService {

    private final DriverRepository driverRepository;
    private final TourProcessingDraftRepository draftRepository;
    private final TourProcessingDraftDayRepository dayRepository;

    @Transactional(readOnly = true)
    public List<DriverMonthResponseDto> findHistory(UUID driverId) {
        return draftRepository.findAll().stream()
                .filter(draft -> draft.getYear() != null && draft.getMonth() != null)
                .map(draft -> java.time.YearMonth.of(draft.getYear(), draft.getMonth()))
                .distinct().sorted(java.util.Comparator.reverseOrder())
                .flatMap(period -> findAllForPeriod(period.getYear(), period.getMonthValue()).stream())
                .filter(month -> driverId.equals(month.getDriverId())).toList();
    }

    @Transactional(readOnly = true)
    public DriverMonthResponseDto findCurrent(UUID driverId) {
        LocalDate today = LocalDate.now();
        return findAllForPeriod(today.getYear(), today.getMonthValue()).stream()
                .filter(month -> driverId.equals(month.getDriverId())).findFirst()
                .orElseThrow(() -> com.proseg.msvc_transport.exception.TransportException.notFound("Chofer", driverId.toString()));
    }

    @Transactional(readOnly = true)
    public List<DriverMonthResponseDto> findAllForPeriod(int year, int month) {
        java.time.YearMonth period = java.time.YearMonth.of(year, month);
        Map<UUID, DriverMonthTotals> totalsByDriver = draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(year, month)
                .map(draft -> dayRepository.summarizeMonth(draft.getId(), period.atDay(1), period.atEndOfMonth()).stream()
                        .collect(Collectors.toMap(TourProcessingDraftDayRepository.MonthlyTotals::getDriverId,
                                total -> new DriverMonthTotals(round(total.getOrdinaryHours()), round(total.getSurplusHours()),
                                        round(total.getOvertimeHours()), total.getWeekendsWorked()))))
                .orElse(Map.of());
        return driverRepository.findAll().stream()
                .map(driver -> toResponse(driver, year, month, totalsByDriver.get(driver.getId())))
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

    private DriverMonthResponseDto toResponse(Driver driver, int year, int month, DriverMonthTotals totals) {
        return DriverMonthResponseDto.builder()
                .driverId(driver.getId())
                .driverName(driver.getName())
                .year(year)
                .month(month)
                .ordinaryHoursAccrued(totals == null ? 0D : totals.ordinaryHours())
                .surplusHoursAccrued(totals == null ? 0D : totals.surplusHours())
                .overtimeHoursAccrued(totals == null ? 0D : totals.overtimeHours())
                .weekendsWorked(totals == null ? 0 : totals.weekendsWorked())
                .hasRecord(totals != null)
                .build();
    }

    private static double round(double hours) {
        return Math.round(hours * 100) / 100.0;
    }
}
