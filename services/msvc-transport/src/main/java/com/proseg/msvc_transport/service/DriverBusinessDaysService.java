package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.entity.DriverDaySchedule;
import com.proseg.msvc_transport.entity.DriverMonthBusinessDays;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.DriverDayScheduleRepository;
import com.proseg.msvc_transport.repository.DriverMonthBusinessDaysRepository;
import com.proseg.msvc_transport.service.driverhours.DriverBusinessDays;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.OrdinaryWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverBusinessDaysService {

    private final DriverMonthBusinessDaysRepository repository;
    private final DriverDayScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public DriverBusinessDays forMonth(int year, int month, int defaultBusinessDays) {
        Map<UUID, Integer> customized = repository.findAllByYearAndMonth(year, month).stream()
                .collect(Collectors.toMap(DriverMonthBusinessDays::getDriverId, DriverMonthBusinessDays::getBusinessDays, (first, second) -> second));
        return new DriverBusinessDays(defaultBusinessDays, customized, schedulesForMonth(year, month));
    }

    @Transactional(readOnly = true)
    public Map<UUID, Map<LocalDate, OrdinaryWindow>> schedulesForMonth(int year, int month) {
        YearMonth period = YearMonth.of(year, month);
        Map<UUID, Map<LocalDate, OrdinaryWindow>> schedules = new HashMap<>();
        scheduleRepository.findAllByDateBetween(period.atDay(1), period.atEndOfMonth()).forEach(day ->
                schedules.computeIfAbsent(day.getDriverId(), id -> new HashMap<>()).put(day.getDate(),
                        new OrdinaryWindow(DriverHoursRules.hourOf(day.getOrdinaryStart()), DriverHoursRules.hourOf(day.getOrdinaryEnd()))));
        return schedules;
    }

    @Transactional
    public void setSchedule(UUID driverId, int year, int month, Map<LocalDate, OrdinaryWindow> windows) {
        YearMonth period = YearMonth.of(year, month);
        Map<LocalDate, DriverDaySchedule> existing = scheduleRepository
                .findAllByDriverIdAndDateBetween(driverId, period.atDay(1), period.atEndOfMonth()).stream()
                .collect(Collectors.toMap(DriverDaySchedule::getDate, Function.identity(), (first, second) -> first));
        existing.values().stream()
                .filter(day -> !windows.containsKey(day.getDate()))
                .forEach(scheduleRepository::delete);
        windows.forEach((date, window) -> {
            DriverDaySchedule record = existing.getOrDefault(date,
                    DriverDaySchedule.builder().driverId(driverId).date(date).build());
            record.setOrdinaryStart(window.startTime());
            record.setOrdinaryEnd(window.endTime());
            scheduleRepository.save(record);
        });
    }

    @Transactional(readOnly = true)
    public DriverBusinessDays forDraft(TourProcessingDraft draft) {
        if (draft.getBusinessDays() == null) {
            throw TransportException.unprocessable(
                    "TOUR_PROCESSING_DRAFT_WITHOUT_PERIOD",
                    "El borrador no tiene mes ni días hábiles definidos"
            );
        }
        if (draft.getYear() == null || draft.getMonth() == null) {
            return DriverBusinessDays.uniform(draft.getBusinessDays());
        }
        return forMonth(draft.getYear(), draft.getMonth(), draft.getBusinessDays());
    }

    @Transactional(readOnly = true)
    public double surplusCap(TourProcessingDraft draft, UUID driverId) {
        return forDraft(draft).surplusCapOf(driverId);
    }

    @Transactional
    public void set(UUID driverId, int year, int month, Integer businessDays) {
        if (businessDays == null) {
            repository.findByDriverIdAndYearAndMonth(driverId, year, month).ifPresent(repository::delete);
            return;
        }
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        if (businessDays < 0 || businessDays > daysInMonth) {
            throw TransportException.unprocessable(
                    "DRIVER_MONTH_BUSINESS_DAYS_INVALID",
                    "Los días hábiles deben estar entre 0 y " + daysInMonth
            );
        }
        DriverMonthBusinessDays record = repository.findByDriverIdAndYearAndMonth(driverId, year, month)
                .orElseGet(() -> DriverMonthBusinessDays.builder().driverId(driverId).year(year).month(month).build());
        record.setBusinessDays(businessDays);
        repository.save(record);
    }
}
