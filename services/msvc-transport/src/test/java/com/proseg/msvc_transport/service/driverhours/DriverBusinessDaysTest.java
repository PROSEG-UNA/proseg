package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DriverBusinessDaysTest {

    private final UUID ana = UUID.randomUUID();
    private final UUID beto = UUID.randomUUID();

    @Test
    void usesTheMonthDefaultForDriversWithoutACustomValue() {
        DriverBusinessDays businessDays = new DriverBusinessDays(28, Map.of(ana, 21));

        assertThat(businessDays.businessDaysOf(beto)).isEqualTo(28);
        assertThat(businessDays.isCustomized(beto)).isFalse();
        assertThat(businessDays.surplusCapOf(beto)).isEqualTo(37.5);
    }

    @Test
    void usesTheCustomValueAndTheCurrentFormulaForCustomizedDrivers() {
        DriverBusinessDays businessDays = new DriverBusinessDays(28, Map.of(ana, 21));

        assertThat(businessDays.businessDaysOf(ana)).isEqualTo(21);
        assertThat(businessDays.isCustomized(ana)).isTrue();
        assertThat(businessDays.surplusCapOf(ana)).isEqualTo(DriverHoursRules.surplusCap(21)).isEqualTo(28.0);
        assertThat(businessDays.surplusCapOf(ana)).isLessThan(businessDays.surplusCapOf(beto));
    }

    @Test
    void zeroBusinessDaysGiveAZeroMax() {
        DriverBusinessDays businessDays = new DriverBusinessDays(28, Map.of(ana, 0));

        assertThat(businessDays.surplusCapOf(ana)).isZero();
        assertThat(businessDays.surplusTargetOf(ana)).isZero();
    }

    @Test
    void targetIsNinetyFivePercentOfTheIndividualMax() {
        DriverBusinessDays businessDays = new DriverBusinessDays(28, Map.of(ana, 14));

        assertThat(businessDays.surplusTargetOf(ana)).isCloseTo(18.05, within(1e-9));
        assertThat(businessDays.surplusTargetOf(beto)).isCloseTo(35.625, within(1e-9));
        assertThat(businessDays.defaultSurplusCap()).isEqualTo(37.5);
    }

    @Test
    void uniformGivesEveryDriverTheSameValue() {
        DriverBusinessDays businessDays = DriverBusinessDays.uniform(30);

        assertThat(businessDays.businessDaysOf(ana)).isEqualTo(30);
        assertThat(businessDays.surplusCapOf(beto)).isEqualTo(40.0);
        assertThat(businessDays.isCustomized(ana)).isFalse();
    }

    @Test
    void handlesDriversWithoutIdAndCopiesTheMapItReceives() {
        Map<UUID, Integer> source = new HashMap<>();
        source.put(ana, 21);
        DriverBusinessDays businessDays = new DriverBusinessDays(28, source);
        source.put(beto, 3);

        assertThat(businessDays.businessDaysOf(beto)).isEqualTo(28);
        assertThat(businessDays.businessDaysOf((UUID) null)).isEqualTo(28);
        assertThat(businessDays.businessDaysOf((Driver) null)).isEqualTo(28);
        assertThat(new DriverBusinessDays(28, null).businessDaysOf(ana)).isEqualTo(28);
    }
}
