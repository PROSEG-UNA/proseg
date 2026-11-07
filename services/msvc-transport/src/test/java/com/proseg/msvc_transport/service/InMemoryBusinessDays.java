package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.entity.DriverMonthBusinessDays;
import com.proseg.msvc_transport.repository.DriverMonthBusinessDaysRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class InMemoryBusinessDays {

    private final Map<String, DriverMonthBusinessDays> store = new HashMap<>();
    private final DriverMonthBusinessDaysRepository repository = mock(DriverMonthBusinessDaysRepository.class);
    private final DriverBusinessDaysService service = new DriverBusinessDaysService(repository);

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

    Integer storedValue(UUID driverId, int year, int month) {
        DriverMonthBusinessDays record = store.get(key(driverId, year, month));
        return record == null ? null : record.getBusinessDays();
    }

    int storedRecords() {
        return store.size();
    }

    private static String key(UUID driverId, Integer year, Integer month) {
        return driverId + "/" + year + "/" + month;
    }
}
