package com.proseg.msvc_transport.service.driverhours;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.sameDayTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.tour;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DriverHoursRulesTest {

    @Test
    void surplusCapRoundsUpToNextHalfHour() {
        assertThat(DriverHoursRules.surplusCap(28)).isEqualTo(37.5);
        assertThat(DriverHoursRules.surplusCap(30)).isEqualTo(40.0);
        assertThat(DriverHoursRules.surplusCap(22)).isEqualTo(29.5);
        assertThat(DriverHoursRules.surplusCap(21)).isEqualTo(28.0);
    }

    @Test
    void singleDayWeekdayTourSplitsOrdinaryAndOffScheduleHours() {
        List<TourDaySpan> spans = DriverHoursRules.daySpans(sameDayTour("1", "2026-05-05", "07:30", "19:30"));

        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).ordinaryHours()).isEqualTo(9.0);
        assertThat(spans.get(0).offScheduleWeekdayHours()).isEqualTo(3.0);
        assertThat(spans.get(0).weekendHours()).isZero();
    }

    @Test
    void singleDayWeekendTourIsAllWeekendHours() {
        TourDaySpan span = DriverHoursRules.daySpans(sameDayTour("1", "2026-05-09", "07:30", "19:30")).get(0);

        assertThat(span.ordinaryHours()).isZero();
        assertThat(span.offScheduleWeekdayHours()).isZero();
        assertThat(span.weekendHours()).isEqualTo(12.0);
    }

    @Test
    void multiDayTourBuildsFirstIntermediateAndLastDays() {
        List<TourDaySpan> spans = DriverHoursRules.daySpans(tour("1", "2026-05-07", "13:00", "2026-05-11", "15:00"));

        assertThat(spans)
                .extracting(TourDaySpan::date, TourDaySpan::start, TourDaySpan::end)
                .containsExactly(
                        tuple(LocalDate.parse("2026-05-07"), 13.0, 20.5),
                        tuple(LocalDate.parse("2026-05-08"), 7.5, 20.5),
                        tuple(LocalDate.parse("2026-05-09"), 7.5, 19.5),
                        tuple(LocalDate.parse("2026-05-10"), 7.5, 19.5),
                        tuple(LocalDate.parse("2026-05-11"), 7.5, 15.0)
                );
    }

    @Test
    void firstDayWeekdayStartingEarlyEndsThirteenHoursLater() {
        List<TourDaySpan> spans = DriverHoursRules.daySpans(tour("1", "2026-05-05", "07:30", "2026-05-06", "12:00"));

        assertThat(spans.get(0).end()).isEqualTo(20.5);
        assertThat(spans.get(1).start()).isEqualTo(7.5);
        assertThat(spans.get(1).end()).isEqualTo(12.0);
    }

    @Test
    void firstDayWeekendStartingEarlyEndsTwelveHoursLater() {
        List<TourDaySpan> spans = DriverHoursRules.daySpans(tour("1", "2026-05-09", "05:00", "2026-05-10", "10:00"));

        assertThat(spans.get(0).end()).isEqualTo(17.0);
    }

    @Test
    void firstDayWeekendLateStartStillCapsAtTwentyThirty() {
        List<TourDaySpan> spans = DriverHoursRules.daySpans(tour("1", "2026-05-09", "10:00", "2026-05-10", "10:00"));

        assertThat(spans.get(0).end()).isEqualTo(20.5);
    }

    @Test
    void monthWeekendsIncludesEveryWeekendTouchingTheMonth() {
        assertThat(DriverHoursRules.monthWeekends(2026, 5)).containsExactly(
                LocalDate.parse("2026-05-02"),
                LocalDate.parse("2026-05-09"),
                LocalDate.parse("2026-05-16"),
                LocalDate.parse("2026-05-23"),
                LocalDate.parse("2026-05-30")
        );
    }
}
