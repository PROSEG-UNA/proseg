package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_transport.entity.CleaningDraft;
import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.CleaningDraftRowDay;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.CleaningDraftRepository;
import com.proseg.msvc_transport.repository.CleaningDraftRowDayRepository;
import com.proseg.msvc_transport.repository.CleaningDraftRowRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import lombok.RequiredArgsConstructor;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OvertimeRequestReportService {

    private static final Locale SPANISH = Locale.forLanguageTag("es");
    private static final double HOURS_TOLERANCE = DriverHoursRules.HOURS_TOLERANCE;
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final CleaningDraftRepository draftRepository;
    private final CleaningDraftRowRepository rowRepository;
    private final CleaningDraftRowDayRepository dayRepository;
    private final DriverRepository driverRepository;

    @Transactional(readOnly = true)
    public OvertimeRequestReportDto build(int year, int month) {
        if (month < 1 || month > 12) {
            throw TransportException.badRequest("INVALID_MONTH", "El mes debe estar entre 1 y 12");
        }
        CleaningDraft draft = draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(year, month)
                .orElseThrow(() -> new TransportException(
                        HttpStatus.NOT_FOUND,
                        "CLEANING_DRAFT_MONTH_NOT_FOUND",
                        "No hay un borrador de depuración para " + monthLabel(year, month)
                ));
        if (draft.getBusinessDays() == null) {
            throw TransportException.unprocessable(
                    "CLEANING_DRAFT_WITHOUT_PERIOD",
                    "El borrador de " + monthLabel(year, month) + " no tiene días hábiles definidos"
            );
        }
        return buildReport(draft, true);
    }

    @Transactional(readOnly = true)
    public OvertimeRequestReportDto buildSchedule(CleaningDraft draft) {
        if (draft.getBusinessDays() == null) {
            throw TransportException.unprocessable(
                    "CLEANING_DRAFT_WITHOUT_PERIOD",
                    "El borrador no tiene días hábiles definidos"
            );
        }
        return buildReport(draft, false);
    }

    private OvertimeRequestReportDto buildReport(CleaningDraft draft, boolean onlyExtraHours) {
        double surplusCap = DriverHoursRules.surplusCap(draft.getBusinessDays());
        List<CleaningDraftRow> rows = rowRepository.findAllByDraftIdOrderByRowNumberAsc(draft.getId()).stream()
                .filter(DriverHoursRules::isPrimary)
                .filter(row -> row.getDriver() != null)
                .toList();
        Map<UUID, List<CleaningDraftRowDay>> daysByRowId = dayRepository.findByRowIdIn(rows.stream().map(CleaningDraftRow::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(day -> day.getRow().getId()));
        Map<String, List<CleaningDraftRow>> rowsByDriverKey = rows.stream()
                .collect(Collectors.groupingBy(row -> DriverHoursRules.driverKey(row.getDriver())));

        Collator collator = Collator.getInstance(SPANISH);
        List<OvertimeRequestReportDto.DriverBlock> drivers = driverRepository.findAll().stream()
                .filter(driver -> ACTIVE_STATUS.equalsIgnoreCase(driver.getStatus())
                        || rowsByDriverKey.containsKey(DriverHoursRules.driverKey(driver.getName())))
                .sorted(Comparator.comparing(Driver::getName, collator))
                .map(driver -> toDriverBlock(driver, rowsByDriverKey.getOrDefault(DriverHoursRules.driverKey(driver.getName()), List.of()), daysByRowId, onlyExtraHours))
                .toList();

        return OvertimeRequestReportDto.builder()
                .year(draft.getYear())
                .month(draft.getMonth())
                .businessDays(draft.getBusinessDays())
                .surplusCapPerDriver(surplusCap)
                .drivers(drivers)
                .build();
    }

    private OvertimeRequestReportDto.DriverBlock toDriverBlock(
            Driver driver,
            List<CleaningDraftRow> driverRows,
            Map<UUID, List<CleaningDraftRowDay>> daysByRowId,
            boolean onlyExtraHours
    ) {
        List<OvertimeRequestReportDto.TourBlock> tours = driverRows.stream()
                .sorted(Comparator.comparing(CleaningDraftRow::getDepartureDate).thenComparing(CleaningDraftRow::getNumber))
                .map(row -> toTourBlock(row, daysByRowId.getOrDefault(row.getId(), List.of()), onlyExtraHours))
                .filter(tour -> !tour.getDays().isEmpty())
                .toList();
        return OvertimeRequestReportDto.DriverBlock.builder()
                .name(driver.getName())
                .surplusHours(round(tours.stream().flatMap(tour -> tour.getDays().stream()).mapToDouble(OvertimeRequestReportDto.TourDay::getSurplusHours).sum()))
                .overtimeHours(round(tours.stream().flatMap(tour -> tour.getDays().stream()).mapToDouble(OvertimeRequestReportDto.TourDay::getOvertimeHours).sum()))
                .tours(tours)
                .build();
    }

    private OvertimeRequestReportDto.TourBlock toTourBlock(CleaningDraftRow row, List<CleaningDraftRowDay> days, boolean onlyExtraHours) {
        List<OvertimeRequestReportDto.TourDay> tourDays = days.stream()
                .sorted(Comparator.comparing(CleaningDraftRowDay::getDate))
                .filter(day -> !onlyExtraHours || day.getSurplusHours() + day.getOvertimeHours() > HOURS_TOLERANCE)
                .map(this::toTourDay)
                .toList();
        return OvertimeRequestReportDto.TourBlock.builder()
                .id(row.getId())
                .number(row.getNumber())
                .priority(row.getPriority())
                .executingUnit(row.getExecutingUnit())
                .responsible(row.getResponsible())
                .days(tourDays)
                .build();
    }

    private OvertimeRequestReportDto.TourDay toTourDay(CleaningDraftRowDay day) {
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
