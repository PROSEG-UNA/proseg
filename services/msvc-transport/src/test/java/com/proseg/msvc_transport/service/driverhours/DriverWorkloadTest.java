package com.proseg.msvc_transport.service.driverhours;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.sameDayTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.tour;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DriverWorkloadTest {

    private static final double MAY_SURPLUS_CAP = DriverHoursRules.surplusCap(28);
    private static final Set<LocalDate> MAY_WEEKENDS = DriverHoursRules.monthWeekends(2026, 5);
    private static final String[] MAY_WEEKDAYS = {
            "2026-05-04", "2026-05-05", "2026-05-06", "2026-05-07", "2026-05-08",
            "2026-05-11", "2026-05-12", "2026-05-13", "2026-05-14", "2026-05-15",
            "2026-05-18", "2026-05-19", "2026-05-20", "2026-05-21",
    };

    private DriverWorkload withEveningTours(int count) {
        DriverWorkload workload = new DriverWorkload();
        for (int i = 0; i < count; i++) {
            workload.add(sameDayTour("E" + i, MAY_WEEKDAYS[i], "16:30", "19:30"));
        }
        return workload;
    }

    @Test
    void sendsOffScheduleHoursToOvertimeOncePastNinetyFivePercentWithoutRoom() {
        DriverWorkload workload = withEveningTours(12);

        DriverMonthTotals totals = workload.totalsWith(
                sameDayTour("X", "2026-05-22", "16:30", "19:30"),
                MAY_SURPLUS_CAP,
                MAY_WEEKENDS
        );

        assertThat(totals.surplusHours()).isEqualTo(36.0);
        assertThat(totals.overtimeHours()).isEqualTo(3.0);
    }

    @Test
    void fillsRemainingSurplusAndOverflowsToOvertimeBelowNinetyFivePercent() {
        DriverWorkload workload = withEveningTours(11);

        DriverMonthTotals totals = workload.totalsWith(
                sameDayTour("X", "2026-05-22", "16:30", "23:30"),
                MAY_SURPLUS_CAP,
                MAY_WEEKENDS
        );

        assertThat(totals.surplusHours()).isEqualTo(37.5);
        assertThat(totals.overtimeHours()).isEqualTo(2.5);
    }

    @Test
    void weekendHoursAlwaysGoToOvertimeAndCountTheWeekend() {
        DriverWorkload workload = new DriverWorkload();
        workload.add(tour("1", "2026-05-08", "07:30", "2026-05-10", "17:00"));

        DriverMonthTotals totals = workload.totals(MAY_SURPLUS_CAP, MAY_WEEKENDS);

        assertThat(totals.ordinaryHours()).isEqualTo(9.0);
        assertThat(totals.surplusHours()).isEqualTo(4.0);
        assertThat(totals.overtimeHours()).isEqualTo(12.0 + 9.5);
        assertThat(totals.weekendsWorked()).isEqualTo(1);
    }

    @Test
    void spreadsTourSurplusDayByDayAndOverflowsTheRestToOvertime() {
        DriverWorkload workload = withEveningTours(7);
        workload.add(sameDayTour("E7", "2026-05-13", "16:30", "18:30"));
        workload.add(tour("3622", "2026-05-25", "11:00", "2026-05-29", "18:00"));

        TourAllocation lastTour = workload.allocations(MAY_SURPLUS_CAP).get(8);

        assertThat(lastTour.tour().getNumber()).isEqualTo("3622");
        assertThat(lastTour.days())
                .extracting(day -> day.span().start(), day -> day.span().end(), DayAllocation::surplusHours, DayAllocation::overtimeHours)
                .containsExactly(
                        tuple(11.0, 20.5, 4.0, 0.0),
                        tuple(7.5, 20.5, 4.0, 0.0),
                        tuple(7.5, 20.5, 4.0, 0.0),
                        tuple(7.5, 20.5, 2.5, 1.5),
                        tuple(7.5, 18.0, 0.0, 1.5)
                );
        DriverMonthTotals totals = workload.totals(MAY_SURPLUS_CAP, MAY_WEEKENDS);
        assertThat(totals.surplusHours()).isEqualTo(37.5);
        assertThat(totals.overtimeHours()).isEqualTo(3.0);
    }

    @Test
    void detectsOverlappingTours() {
        DriverWorkload workload = new DriverWorkload();
        workload.add(sameDayTour("1", "2026-05-05", "08:00", "12:00"));

        assertThat(workload.overlaps(sameDayTour("2", "2026-05-05", "11:00", "14:00"))).isTrue();
        assertThat(workload.overlaps(sameDayTour("3", "2026-05-05", "12:00", "14:00"))).isFalse();
    }
}
