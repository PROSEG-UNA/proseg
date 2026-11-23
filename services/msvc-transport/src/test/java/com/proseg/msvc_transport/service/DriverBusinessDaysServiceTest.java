package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.service.driverhours.DriverBusinessDays;
import com.proseg.msvc_transport.service.driverhours.DriverSchedule;
import com.proseg.msvc_transport.service.driverhours.OrdinaryWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriverBusinessDaysServiceTest {

    private final UUID ana = UUID.randomUUID();
    private final UUID beto = UUID.randomUUID();

    private InMemoryBusinessDays store;
    private DriverBusinessDaysService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryBusinessDays();
        service = store.service();
    }

    @Test
    void theMaxOfADriverComesFromTheDraftValueUntilTheyAreEdited() {
        TourProcessingDraft draft = TourProcessingDraft.builder().year(2026).month(5).businessDays(28).build();

        assertThat(service.surplusCap(draft, ana)).isEqualTo(37.5);

        service.set(ana, 2026, 5, 21);

        assertThat(service.surplusCap(draft, ana)).isEqualTo(28.0);
        assertThat(service.surplusCap(draft, beto)).isEqualTo(37.5);
    }

    @Test
    void valuesAreKeptPerMonth() {
        service.set(ana, 2026, 5, 21);

        DriverBusinessDays june = service.forMonth(2026, 6, 30);

        assertThat(june.businessDaysOf(ana)).isEqualTo(30);
        assertThat(service.forMonth(2026, 5, 28).businessDaysOf(ana)).isEqualTo(21);
    }

    @Test
    void settingTheSameDriverAndMonthAgainReplacesTheValue() {
        service.set(ana, 2026, 5, 21);
        service.set(ana, 2026, 5, 18);

        assertThat(store.storedRecords()).isEqualTo(1);
        assertThat(store.storedValue(ana, 2026, 5)).isEqualTo(18);
    }

    @Test
    void nullRemovesTheCustomValueAndIgnoresDriversWithoutOne() {
        service.set(ana, 2026, 5, 21);

        service.set(ana, 2026, 5, null);
        service.set(beto, 2026, 5, null);

        assertThat(store.storedRecords()).isZero();
    }

    @Test
    void acceptsFromZeroUpToTheDaysOfTheMonth() {
        service.set(ana, 2026, 2, 0);
        service.set(beto, 2026, 2, 28);
        service.set(ana, 2028, 2, 29);

        assertThat(store.storedValue(ana, 2026, 2)).isZero();
        assertThat(store.storedValue(beto, 2026, 2)).isEqualTo(28);
        assertThat(store.storedValue(ana, 2028, 2)).isEqualTo(29);
    }

    @Test
    void rejectsValuesBelowZeroOrAboveTheDaysOfTheMonth() {
        assertThatThrownBy(() -> service.set(ana, 2026, 2, 29))
                .isInstanceOf(TransportException.class).hasMessage("Los días hábiles deben estar entre 0 y 28");
        assertThatThrownBy(() -> service.set(ana, 2026, 2, -1))
                .isInstanceOf(TransportException.class).hasMessage("Los días hábiles deben estar entre 0 y 28");
        assertThat(store.storedRecords()).isZero();
    }

    @Test
    void schedulesAreKeptPerDriverAndDay() {
        Driver driver = Driver.builder().id(ana).name("Ana").ordinaryStart(LocalTime.of(7, 30)).ordinaryEnd(LocalTime.of(16, 30)).build();
        service.setSchedule(ana, 2026, 5, Map.of(LocalDate.of(2026, 5, 18), OrdinaryWindow.AFTERNOON));

        DriverSchedule may = service.forMonth(2026, 5, 28).scheduleOf(driver);

        assertThat(may.windowOn(LocalDate.of(2026, 5, 18))).isEqualTo(OrdinaryWindow.AFTERNOON);
        assertThat(may.windowOn(LocalDate.of(2026, 5, 19))).isEqualTo(OrdinaryWindow.STANDARD);
        assertThat(service.forMonth(2026, 6, 30).scheduleOf(driver).windowOn(LocalDate.of(2026, 6, 18))).isEqualTo(OrdinaryWindow.STANDARD);
        assertThat(service.forMonth(2026, 5, 28).scheduleOverridesOf(beto)).isEmpty();
        assertThat(service.forMonth(2026, 5, 28).scheduleOverridesOf(ana)).containsOnlyKeys(LocalDate.of(2026, 5, 18));
    }

    @Test
    void settingTheScheduleAgainReplacesTheDaysOfThatMonthOnly() {
        LocalDate first = LocalDate.of(2026, 5, 18);
        LocalDate second = LocalDate.of(2026, 5, 19);
        LocalDate june = LocalDate.of(2026, 6, 1);
        service.setSchedule(ana, 2026, 6, Map.of(june, OrdinaryWindow.AFTERNOON));
        service.setSchedule(ana, 2026, 5, Map.of(first, OrdinaryWindow.AFTERNOON, second, OrdinaryWindow.AFTERNOON));

        service.setSchedule(ana, 2026, 5, Map.of(second, OrdinaryWindow.STANDARD));

        assertThat(store.storedSchedule(ana, first)).isNull();
        assertThat(store.storedSchedule(ana, second).getOrdinaryStart()).isEqualTo(LocalTime.of(7, 30));
        assertThat(store.storedSchedule(ana, june).getOrdinaryStart()).isEqualTo(LocalTime.of(12, 0));
        assertThat(store.storedScheduleDays()).isEqualTo(2);
    }

    @Test
    void anEmptyScheduleRemovesEveryDayOfTheMonthAndIgnoresDriversWithoutOnes() {
        service.setSchedule(ana, 2026, 5, Map.of(LocalDate.of(2026, 5, 18), OrdinaryWindow.AFTERNOON));

        service.setSchedule(ana, 2026, 5, Map.of());
        service.setSchedule(beto, 2026, 5, Map.of());

        assertThat(store.storedScheduleDays()).isZero();
    }

    @Test
    void draftsWithoutBusinessDaysHaveNoMax() {
        TourProcessingDraft draft = TourProcessingDraft.builder().year(2026).month(5).build();

        assertThatThrownBy(() -> service.forDraft(draft)).isInstanceOf(TransportException.class);
    }

    @Test
    void draftsWithoutPeriodUseTheDraftValueForEveryDriver() {
        TourProcessingDraft draft = TourProcessingDraft.builder().businessDays(21).build();

        assertThat(service.forDraft(draft).businessDaysOf(ana)).isEqualTo(21);
        assertThat(service.surplusCap(draft, beto)).isEqualTo(28.0);
    }
}
