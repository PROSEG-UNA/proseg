package com.proseg.msvc_transport.service.driverhours;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeekendCapTest {

    @Test
    void allowsThreeWeekendsWhenTheMonthHasFive() {
        assertThat(WeekendCap.forWeekendsInMonth(5)).isEqualTo(3);
    }

    @Test
    void allowsTwoWeekendsWhenTheMonthHasFour() {
        assertThat(WeekendCap.forWeekendsInMonth(4)).isEqualTo(2);
    }

    @Test
    void allowsTwoWeekendsForAnyOtherCount() {
        assertThat(WeekendCap.forWeekendsInMonth(0)).isEqualTo(2);
        assertThat(WeekendCap.forWeekendsInMonth(3)).isEqualTo(2);
        assertThat(WeekendCap.forWeekendsInMonth(6)).isEqualTo(2);
    }

    @Test
    void octoberTwentyTwentySixCountsFiveWeekendsIncludingSaturdayThirtyOneSoTheCapIsThree() {
        assertThat(DriverHoursRules.monthWeekends(2026, 10))
                .containsExactly(
                        java.time.LocalDate.of(2026, 10, 3),
                        java.time.LocalDate.of(2026, 10, 10),
                        java.time.LocalDate.of(2026, 10, 17),
                        java.time.LocalDate.of(2026, 10, 24),
                        java.time.LocalDate.of(2026, 10, 31));
        assertThat(WeekendCap.forWeekendsInMonth(DriverHoursRules.monthWeekends(2026, 10).size())).isEqualTo(3);
    }

    @Test
    void aMonthWithFourSaturdaysAndNoWeekendSpillingFromThePreviousMonthHasACapOfTwo() {
        assertThat(DriverHoursRules.monthWeekends(2027, 2)).hasSize(4);
        assertThat(WeekendCap.forWeekendsInMonth(DriverHoursRules.monthWeekends(2027, 2).size())).isEqualTo(2);
    }
}
