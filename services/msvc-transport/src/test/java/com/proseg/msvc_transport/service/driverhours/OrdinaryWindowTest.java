package com.proseg.msvc_transport.service.driverhours;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driver;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driverWithSchedule;
import static org.assertj.core.api.Assertions.assertThat;

class OrdinaryWindowTest {

    @Test
    void onlyTheStandardAndTheAfternoonSchedulesAreAllowed() {
        assertThat(OrdinaryWindow.allowed(LocalTime.of(7, 30), LocalTime.of(16, 30))).contains(OrdinaryWindow.STANDARD);
        assertThat(OrdinaryWindow.allowed(LocalTime.of(12, 0), LocalTime.of(21, 0))).contains(OrdinaryWindow.AFTERNOON);
        assertThat(OrdinaryWindow.allowed(LocalTime.of(11, 0), LocalTime.of(20, 0))).isEmpty();
        assertThat(OrdinaryWindow.allowed(LocalTime.of(12, 0), LocalTime.of(20, 0))).isEmpty();
        assertThat(OrdinaryWindow.allowed(LocalTime.of(7, 30), LocalTime.of(21, 0))).isEmpty();
    }

    @Test
    void bothAllowedSchedulesLastNineHours() {
        assertThat(OrdinaryWindow.STANDARD.hours()).isEqualTo(9.0);
        assertThat(OrdinaryWindow.AFTERNOON.hours()).isEqualTo(9.0);
    }

    @Test
    void aDriverWithoutScheduleUsesTheStandardWindowAndOthersUseTheirOwn() {
        assertThat(OrdinaryWindow.of(driver("Ana"))).isEqualTo(OrdinaryWindow.STANDARD);
        assertThat(OrdinaryWindow.of(driverWithSchedule("Ricardo", "12:00", "21:00"))).isEqualTo(OrdinaryWindow.AFTERNOON);
    }

    @Test
    void describesTheAllowedSchedulesForErrorMessages() {
        assertThat(OrdinaryWindow.allowedDescription()).isEqualTo("07:30 a 16:30 o 12:00 a 21:00");
    }
}
