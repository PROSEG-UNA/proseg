package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.entity.DriverDaySchedule;
import com.proseg.msvc_transport.entity.DriverMonthBusinessDays;
import com.proseg.msvc_transport.repository.DriverDayScheduleRepository;
import com.proseg.msvc_transport.repository.DriverMonthBusinessDaysRepository;
import com.proseg.msvc_transport.service.driverhours.OrdinaryWindow;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class InMemoryBusinessDays {

    private final Map<String, DriverMonthBusinessDays> store = new HashMap<>();
    private final Map<String, DriverDaySchedule> schedules = new HashMap<>();
    private final DriverMonthBusinessDaysRepository repository = mock(DriverMonthBusinessDaysRepository.class);
    private final DriverDayScheduleRepository scheduleRepository = mock(DriverDayScheduleRepository.class);
    private final DriverBusinessDaysService service = new DriverBusinessDaysService(repository, scheduleRepository);

    InMemoryBusinessDays() {
        when(repository.findAllByYearAndMonth(any(), any())).thenAnswer(invocation -> store.values().stream()
                .filter(record -> record.getYear().equals(invocation.getArgument(0)) && record.getMonth().equals(invocation.getArgument(1)))
                .toList());
        when(repository.findByDriverIdAndYearAndMonth(any(), any(), any())).thenAnswer(invocation ->
                Optional.ofNullable(store.get(key(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)))));
        when(repository.save(any(DriverMonthBusinessDays.class))).thenAnswer(invocation -> {
            DriverMonthBusinessDays record = invocation.getArgument(0);
            store.put(key(record.getDriverId(), record.getYear(), record.getMonth()), record);
            return record;
        });
        doAnswer(invocation -> {
            DriverMonthBusinessDays record = invocation.getArgument(0);
            store.remove(key(record.getDriverId(), record.getYear(), record.getMonth()));
            return null;
        }).when(repository).delete(any(DriverMonthBusinessDays.class));

        when(scheduleRepository.findAllByDateBetween(any(), any())).thenAnswer(invocation -> schedulesBetween(
                null, invocation.getArgument(0), invocation.getArgument(1)));
        when(scheduleRepository.findAllByDriverIdAndDateBetween(any(), any(), any())).thenAnswer(invocation -> schedulesBetween(
                invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
        when(scheduleRepository.save(any(DriverDaySchedule.class))).thenAnswer(invocation -> {
            DriverDaySchedule record = invocation.getArgument(0);
            schedules.put(dayKey(record.getDriverId(), record.getDate()), record);
            return record;
        });
        doAnswer(invocation -> {
            DriverDaySchedule record = invocation.getArgument(0);
            schedules.remove(dayKey(record.getDriverId(), record.getDate()));
            return null;
        }).when(scheduleRepository).delete(any(DriverDaySchedule.class));
    }

    DriverMonthBusinessDaysRepository repository() {
        return repository;
    }

    DriverBusinessDaysService service() {
        return service;
    }

    void customize(UUID driverId, int year, int month, int businessDays) {
        service.set(driverId, year, month, businessDays);
    }

    void customizeSchedule(UUID driverId, int year, int month, Map<LocalDate, OrdinaryWindow> windows) {
        service.setSchedule(driverId, year, month, windows);
    }

    Integer storedValue(UUID driverId, int year, int month) {
        DriverMonthBusinessDays record = store.get(key(driverId, year, month));
        return record == null ? null : record.getBusinessDays();
    }

    DriverDaySchedule storedSchedule(UUID driverId, LocalDate date) {
        return schedules.get(dayKey(driverId, date));
    }

    int storedScheduleDays() {
        return schedules.size();
    }

    int storedRecords() {
        return store.size();
    }

    private List<DriverDaySchedule> schedulesBetween(UUID driverId, LocalDate from, LocalDate to) {
        return schedules.values().stream()
                .filter(record -> driverId == null || driverId.equals(record.getDriverId()))
                .filter(record -> !record.getDate().isBefore(from) && !record.getDate().isAfter(to))
                .toList();
    }

    private static String key(UUID driverId, Integer year, Integer month) {
        return driverId + "/" + year + "/" + month;
    }

    private static String dayKey(UUID driverId, LocalDate date) {
        return driverId + "/" + date;
    }
}
