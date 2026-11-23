package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.response.DriverDayScheduleDto;
import com.proseg.msvc_transport.dto.response.DriverMonthResponseDto;
import com.proseg.msvc_transport.dto.response.DriverMonthSurplusCapResponseDto;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.service.driverhours.DriverBusinessDays;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.DriverMonthTotals;
import com.proseg.msvc_transport.service.driverhours.OrdinaryWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverMonthService {

    private final DriverRepository driverRepository;
    private final TourProcessingDraftRepository draftRepository;
    private final TourProcessingDraftDayRepository dayRepository;
    private final DriverBusinessDaysService businessDaysService;
    private final DraftHoursService draftHoursService;
    private final DraftTourStore tourStore;

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
        Optional<TourProcessingDraft> draft = draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(year, month);
        Map<UUID, DriverMonthTotals> totalsByDriver = draft
                .map(current -> dayRepository.summarizeMonth(current.getId(), period.atDay(1), period.atEndOfMonth()).stream()
                        .collect(Collectors.toMap(TourProcessingDraftDayRepository.MonthlyTotals::getDriverId,
                                total -> new DriverMonthTotals(round(total.getOrdinaryHours()), round(total.getSurplusHours()),
                                        round(total.getOvertimeHours()), total.getWeekendsWorked()))))
                .orElse(Map.of());
        DriverBusinessDays businessDays = draft
                .filter(current -> current.getBusinessDays() != null)
                .map(current -> businessDaysService.forMonth(year, month, current.getBusinessDays()))
                .orElse(null);
        Map<UUID, Map<LocalDate, OrdinaryWindow>> schedules = businessDays == null
                ? businessDaysService.schedulesForMonth(year, month)
                : businessDays.customizedSchedules();
        return driverRepository.findAll().stream()
                .map(driver -> toResponse(driver, year, month, totalsByDriver.get(driver.getId()), businessDays, schedules))
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
    public DriverMonthResponseDto updateBusinessDays(UUID driverId, int year, int month, Integer businessDays) {
        TourProcessingDraft draft = editableDraft(driverId, year, month, "los días hábiles");
        businessDaysService.set(driverId, year, month, businessDays);
        return resync(draft, driverId, year, month);
    }

    @Transactional
    public DriverMonthResponseDto updateSchedule(UUID driverId, int year, int month, List<DriverDayScheduleDto> days) {
        Optional<TourProcessingDraft> draft = draftToResync(driverId, year, month);
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> TransportException.notFound("Chofer", driverId.toString()));
        businessDaysService.setSchedule(driverId, year, month, scheduleOverrides(driver, YearMonth.of(year, month), days));
        return draft.map(current -> resync(current, driverId, year, month))
                .orElseGet(() -> monthOf(driverId, year, month));
    }

    private Map<LocalDate, OrdinaryWindow> scheduleOverrides(Driver driver, YearMonth period, List<DriverDayScheduleDto> days) {
        OrdinaryWindow base = OrdinaryWindow.of(driver);
        Map<LocalDate, OrdinaryWindow> overrides = new HashMap<>();
        for (DriverDayScheduleDto day : days == null ? List.<DriverDayScheduleDto>of() : days) {
            if (day.getDate() == null || !YearMonth.from(day.getDate()).equals(period)) {
                throw TransportException.unprocessable(
                        "DRIVER_SCHEDULE_DAY_OUT_OF_MONTH",
                        "Todos los días del horario deben pertenecer al mes seleccionado"
                );
            }
            OrdinaryWindow window = OrdinaryWindow.allowed(day.getOrdinaryStart(), day.getOrdinaryEnd())
                    .orElseThrow(() -> TransportException.unprocessable(
                            "DRIVER_INVALID_ORDINARY_SCHEDULE",
                            "El horario ordinario debe ser de " + OrdinaryWindow.allowedDescription()
                    ));
            if (!DriverHoursRules.isWeekend(day.getDate()) && !window.equals(base)) {
                overrides.put(day.getDate(), window);
            }
        }
        return overrides;
    }

    private DriverMonthResponseDto resync(TourProcessingDraft draft, UUID driverId, int year, int month) {
        List<TourProcessingDraftAssignment> assignments = tourStore.findAllByDraftIdOrderByRowNumberAsc(draft.getId()).stream()
                .flatMap(tour -> tour.getAssignments().stream())
                .toList();
        draftHoursService.syncDriver(draft, assignments, driverId);
        return monthOf(driverId, year, month);
    }

    private DriverMonthResponseDto monthOf(UUID driverId, int year, int month) {
        return findAllForPeriod(year, month).stream()
                .filter(current -> driverId.equals(current.getDriverId()))
                .findFirst()
                .orElseThrow(() -> TransportException.notFound("Chofer", driverId.toString()));
    }

    private void requireValidMonthAndDriver(UUID driverId, int month) {
        if (month < 1 || month > 12) {
            throw TransportException.badRequest("INVALID_MONTH", "El mes debe estar entre 1 y 12");
        }
        driverRepository.findById(driverId)
                .orElseThrow(() -> TransportException.notFound("Chofer", driverId.toString()));
    }

    private void requireOpenDraft(TourProcessingDraft draft) {
        if (draft.getStatus() != TourProcessingDraftStatus.DRAFT) {
            throw TransportException.conflict(
                    draft.getStatus() == TourProcessingDraftStatus.CLOSED
                            ? "TOUR_PROCESSING_DRAFT_CLOSED" : "TOUR_PROCESSING_DRAFT_ALREADY_REGISTERED",
                    draft.getStatus() == TourProcessingDraftStatus.CLOSED
                            ? "No se puede editar una depuración terminada. Devuélvela a borrador"
                            : "No se puede editar un borrador que ya fue registrado"
            );
        }
    }

    private TourProcessingDraft editableDraft(UUID driverId, int year, int month, String subject) {
        requireValidMonthAndDriver(driverId, month);
        TourProcessingDraft draft = draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(year, month)
                .flatMap(current -> draftRepository.findByIdForUpdate(current.getId()))
                .orElseThrow(() -> TransportException.unprocessable(
                        "TOUR_PROCESSING_DRAFT_MONTH_NOT_FOUND",
                        "Importa el archivo del mes antes de cambiar " + subject + " de un chofer"));
        if (draft.getBusinessDays() == null) {
            throw TransportException.unprocessable(
                    "TOUR_PROCESSING_DRAFT_WITHOUT_PERIOD",
                    "El borrador no tiene días hábiles definidos"
            );
        }
        requireOpenDraft(draft);
        return draft;
    }

    private Optional<TourProcessingDraft> draftToResync(UUID driverId, int year, int month) {
        requireValidMonthAndDriver(driverId, month);
        Optional<TourProcessingDraft> draft = draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(year, month)
                .flatMap(current -> draftRepository.findByIdForUpdate(current.getId()));
        draft.ifPresent(this::requireOpenDraft);
        return draft.filter(current -> current.getBusinessDays() != null);
    }

    private DriverMonthResponseDto toResponse(Driver driver, int year, int month, DriverMonthTotals totals, DriverBusinessDays businessDays,
                                              Map<UUID, Map<LocalDate, OrdinaryWindow>> schedules) {
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
                .businessDays(businessDays == null ? null : businessDays.businessDaysOf(driver))
                .surplusCap(businessDays == null ? null : businessDays.surplusCapOf(driver))
                .businessDaysCustomized(businessDays != null && businessDays.isCustomized(driver.getId()))
                .scheduleDays(scheduleDaysOf(driver, schedules))
                .build();
    }

    private static List<DriverDayScheduleDto> scheduleDaysOf(Driver driver, Map<UUID, Map<LocalDate, OrdinaryWindow>> schedules) {
        return schedules.getOrDefault(driver.getId(), Map.of()).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DriverDayScheduleDto(entry.getKey(), entry.getValue().startTime(), entry.getValue().endTime()))
                .toList();
    }

    private static double round(double hours) {
        return Math.round(hours * 100) / 100.0;
    }
}
