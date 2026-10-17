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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OvertimeRequestReportServiceTest {

    private static final UUID DRAFT_ID = UUID.randomUUID();

    private CleaningDraftRepository draftRepository;
    private CleaningDraftRowRepository rowRepository;
    private CleaningDraftRowDayRepository dayRepository;
    private DriverRepository driverRepository;
    private OvertimeRequestReportService service;

    @BeforeEach
    void setUp() {
        draftRepository = mock(CleaningDraftRepository.class);
        rowRepository = mock(CleaningDraftRowRepository.class);
        dayRepository = mock(CleaningDraftRowDayRepository.class);
        driverRepository = mock(DriverRepository.class);
        service = new OvertimeRequestReportService(draftRepository, rowRepository, dayRepository, driverRepository);
    }

    @Test
    void buildsAlphabeticalActiveDriversWithTheirSurplusAndOvertimeDays() {
        stubDraft(28);
        when(driverRepository.findAll()).thenReturn(List.of(
                driver("Zacarías Mora", "ACTIVE"),
                driver("Álvaro Parada", "ACTIVE"),
                driver("Bruno Inactivo", "INACTIVE"),
                driver("Carla Inactiva Con Gira", "INACTIVE")
        ));
        CleaningDraftRow alvaroFirstTour = row("3622", "álvaro  parada", "2026-05-08");
        CleaningDraftRow alvaroSecondTour = row("3700", "Álvaro Parada", "2026-05-12");
        CleaningDraftRow carlaTour = row("3800", "Carla Inactiva Con Gira", "2026-05-13");
        when(rowRepository.findAllByDraftIdOrderByRowNumberAsc(DRAFT_ID)).thenReturn(List.of(
                alvaroFirstTour, alvaroSecondTour, carlaTour
        ));
        when(dayRepository.findByRowIdIn(anyCollection())).thenReturn(List.of(
                day(alvaroFirstTour, "2026-05-08", "11:00", "20:30", 5.5, 4.0, 0.0),
                day(alvaroFirstTour, "2026-05-09", "07:30", "14:00", 0.0, 0.0, 6.5),
                day(alvaroSecondTour, "2026-05-12", "08:00", "15:00", 7.0, 0.0, 0.0),
                day(carlaTour, "2026-05-13", "06:30", "17:30", 9.0, 2.0, 0.0)
        ));

        OvertimeRequestReportDto report = service.build(2026, 5);

        assertThat(report.getBusinessDays()).isEqualTo(28);
        assertThat(report.getSurplusCapPerDriver()).isEqualTo(37.5);
        assertThat(report.getDrivers())
                .extracting(OvertimeRequestReportDto.DriverBlock::getName,
                        OvertimeRequestReportDto.DriverBlock::getSurplusHours,
                        OvertimeRequestReportDto.DriverBlock::getOvertimeHours)
                .containsExactly(
                        tuple("Álvaro Parada", 4.0, 6.5),
                        tuple("Carla Inactiva Con Gira", 2.0, 0.0),
                        tuple("Zacarías Mora", 0.0, 0.0)
                );

        OvertimeRequestReportDto.DriverBlock alvaro = report.getDrivers().get(0);
        assertThat(alvaro.getTours()).hasSize(1);
        assertThat(alvaro.getTours().get(0).getNumber()).isEqualTo("3622");
        assertThat(alvaro.getTours().get(0).getDays())
                .extracting(OvertimeRequestReportDto.TourDay::getDate,
                        OvertimeRequestReportDto.TourDay::getStart,
                        OvertimeRequestReportDto.TourDay::getEnd,
                        OvertimeRequestReportDto.TourDay::getSurplusHours,
                        OvertimeRequestReportDto.TourDay::getOvertimeHours)
                .containsExactly(
                        tuple(LocalDate.parse("2026-05-08"), 11.0, 20.5, 4.0, 0.0),
                        tuple(LocalDate.parse("2026-05-09"), 7.5, 14.0, 0.0, 6.5)
                );
        assertThat(report.getDrivers().get(2).getTours()).isEmpty();
    }

    @Test
    void failsWhenTheMonthHasNoDraft() {
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 6)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.build(2026, 6))
                .isInstanceOf(TransportException.class)
                .hasMessage("No hay un borrador de depuración para junio de 2026");
    }

    @Test
    void failsWhenTheDraftHasNoBusinessDays() {
        stubDraft(null);

        assertThatThrownBy(() -> service.build(2026, 5))
                .isInstanceOf(TransportException.class)
                .hasMessage("El borrador de mayo de 2026 no tiene días hábiles definidos");
    }

    @Test
    void buildScheduleIncludesOrdinaryOnlyDaysAndTheRowId() {
        CleaningDraft draft = CleaningDraft.builder().id(DRAFT_ID).year(2026).month(5).businessDays(28).build();
        when(driverRepository.findAll()).thenReturn(List.of(driver("Álvaro Parada", "ACTIVE")));
        CleaningDraftRow tour = row("3622", "Álvaro Parada", "2026-05-08");
        when(rowRepository.findAllByDraftIdOrderByRowNumberAsc(DRAFT_ID)).thenReturn(List.of(tour));
        when(dayRepository.findByRowIdIn(anyCollection())).thenReturn(List.of(
                day(tour, "2026-05-08", "07:30", "16:00", 8.5, 0.0, 0.0)
        ));

        OvertimeRequestReportDto schedule = service.buildSchedule(draft);

        assertThat(schedule.getDrivers()).hasSize(1);
        OvertimeRequestReportDto.TourBlock tourBlock = schedule.getDrivers().get(0).getTours().get(0);
        assertThat(tourBlock.getId()).isEqualTo(tour.getId());
        assertThat(tourBlock.getDays())
                .extracting(OvertimeRequestReportDto.TourDay::getOrdinaryHours,
                        OvertimeRequestReportDto.TourDay::getSurplusHours,
                        OvertimeRequestReportDto.TourDay::getOvertimeHours)
                .containsExactly(tuple(8.5, 0.0, 0.0));
    }

    @Test
    void buildScheduleFailsWhenTheDraftHasNoBusinessDays() {
        CleaningDraft draft = CleaningDraft.builder().id(DRAFT_ID).year(2026).month(5).businessDays(null).build();

        assertThatThrownBy(() -> service.buildSchedule(draft))
                .isInstanceOf(TransportException.class)
                .hasMessage("El borrador no tiene días hábiles definidos");
    }

    private void stubDraft(Integer businessDays) {
        CleaningDraft draft = CleaningDraft.builder().id(DRAFT_ID).year(2026).month(5).businessDays(businessDays).build();
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 5)).thenReturn(Optional.of(draft));
    }

    private Driver driver(String name, String status) {
        return Driver.builder().id(UUID.randomUUID()).name(name).status(status).build();
    }

    private CleaningDraftRow row(String number, String driver, String departureDate) {
        return CleaningDraftRow.builder()
                .id(UUID.randomUUID())
                .number(number)
                .driver(driver)
                .priority(1)
                .passengers(3)
                .executingUnit("Escuela de Química")
                .responsible("Ana Mora")
                .duplicateRank(1)
                .departureDate(LocalDate.parse(departureDate))
                .build();
    }

    private CleaningDraftRowDay day(CleaningDraftRow row, String date, String startTime, String endTime,
                                     double ordinaryHours, double surplusHours, double overtimeHours) {
        return CleaningDraftRowDay.builder()
                .row(row)
                .date(LocalDate.parse(date))
                .startTime(LocalTime.parse(startTime))
                .endTime(LocalTime.parse(endTime))
                .ordinaryHours(ordinaryHours)
                .surplusHours(surplusHours)
                .overtimeHours(overtimeHours)
                .build();
    }
}
