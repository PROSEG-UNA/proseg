package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.Driver;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.assigned;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.notApproved;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driver;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driverWithSchedule;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.sameDayTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.tour;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DriverWorkloadTest {

    @Test
    void reservesLaterManualSurplusBeforeAssigningEarlierAutomaticHours() {
        Driver ana = driver("Ana");
        var earlier = assigned(sameDayTour("1", "2026-05-04", "16:30", "20:30"), ana);
        var later = assigned(sameDayTour("2", "2026-05-05", "16:30", "20:30"), ana);
        var manual = com.proseg.msvc_transport.entity.TourProcessingDraftDay.builder().assignment(later)
                .date(LocalDate.of(2026, 5, 5)).startTime(java.time.LocalTime.of(16, 30))
                .endTime(java.time.LocalTime.of(20, 30)).ordinaryHours(0D).surplusHours(4D).overtimeHours(0D)
                .manuallyEdited(true).build();
        DriverWorkload workload = new DriverWorkload(ana, ManualDays.of(List.of(manual)));
        workload.add(earlier);
        workload.add(later);

        var allocations = workload.allocations(5);

        assertThat(allocations.get(0).days().get(0).surplusHours()).isEqualTo(1);
        assertThat(allocations.get(0).days().get(0).overtimeHours()).isEqualTo(3);
        assertThat(allocations.get(1).days().get(0).surplusHours()).isEqualTo(4);
        assertThat(workload.totals(5, MAY_WEEKENDS).surplusHours()).isEqualTo(5);
    }

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
    void ignoresNotApprovedToursWhenBuildingWorkloadsFromDraftRows() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment approved = assigned(sameDayTour("1", "2026-05-05", "08:00", "10:00"), ana);
        TourProcessingDraftAssignment notApproved = notApproved(assigned(sameDayTour("2", "2026-05-06", "08:00", "10:00"), ana));

        Map<UUID, DriverWorkload> workloads = DriverWorkload.fromAssignments(
                List.of(approved, notApproved),
                DriverHoursRules.driversById(List.of(ana)), ManualDays.NONE
        );

        assertThat(workloads.get(ana.getId()).tours()).containsExactly(approved);
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

    @Test
    void classifiesHoursAgainstTheDriversOwnOrdinarySchedule() {
        DriverWorkload standard = new DriverWorkload();
        DriverWorkload lateShift = new DriverWorkload(driverWithSchedule("Ricardo", "11:00", "20:00"));
        standard.add(sameDayTour("A", "2026-05-05", "12:00", "22:00"));
        lateShift.add(sameDayTour("A", "2026-05-05", "12:00", "22:00"));

        DriverMonthTotals standardTotals = standard.totals(MAY_SURPLUS_CAP, MAY_WEEKENDS);
        DriverMonthTotals lateShiftTotals = lateShift.totals(MAY_SURPLUS_CAP, MAY_WEEKENDS);

        assertThat(standardTotals.ordinaryHours()).isEqualTo(4.5);
        assertThat(standardTotals.surplusHours()).isEqualTo(5.5);
        assertThat(lateShiftTotals.ordinaryHours()).isEqualTo(8.0);
        assertThat(lateShiftTotals.surplusHours()).isEqualTo(2.0);
        assertThat(lateShiftTotals.overtimeHours()).isZero();
    }

    @Test
    void usesTheStandardScheduleWhenTheDriverHasNoneConfigured() {
        DriverWorkload withoutSchedule = new DriverWorkload(driver("Ana"));
        withoutSchedule.add(sameDayTour("A", "2026-05-05", "07:30", "16:30"));

        DriverMonthTotals totals = withoutSchedule.totals(MAY_SURPLUS_CAP, MAY_WEEKENDS);

        assertThat(totals.ordinaryHours()).isEqualTo(9.0);
        assertThat(totals.surplusHours()).isZero();
    }

    @Test
    void weekendHoursStayOvertimeWhateverTheDriversSchedule() {
        DriverWorkload lateShift = new DriverWorkload(driverWithSchedule("Ricardo", "11:00", "20:00"));
        lateShift.add(sameDayTour("A", "2026-05-09", "12:00", "20:00"));

        DriverMonthTotals totals = lateShift.totals(MAY_SURPLUS_CAP, MAY_WEEKENDS);

        assertThat(totals.ordinaryHours()).isZero();
        assertThat(totals.overtimeHours()).isEqualTo(8.0);
    }
}
