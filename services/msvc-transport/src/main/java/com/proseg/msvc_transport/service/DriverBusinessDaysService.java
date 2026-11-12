package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.entity.DriverMonthBusinessDays;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.DriverMonthBusinessDaysRepository;
import com.proseg.msvc_transport.service.driverhours.DriverBusinessDays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverBusinessDaysService {

    private final DriverMonthBusinessDaysRepository repository;

    @Transactional(readOnly = true)
    public DriverBusinessDays forMonth(int year, int month, int defaultBusinessDays) {
        Map<UUID, Integer> customized = repository.findAllByYearAndMonth(year, month).stream()
                .collect(Collectors.toMap(DriverMonthBusinessDays::getDriverId, DriverMonthBusinessDays::getBusinessDays, (first, second) -> second));
        return new DriverBusinessDays(defaultBusinessDays, customized);
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
