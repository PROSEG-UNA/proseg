package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record DriverBusinessDays(
        int defaultBusinessDays,
        Map<UUID, Integer> customizedBusinessDays,
        Map<UUID, Map<LocalDate, OrdinaryWindow>> customizedSchedules
) {

    public DriverBusinessDays {
        customizedBusinessDays = customizedBusinessDays == null ? Map.of() : new HashMap<>(customizedBusinessDays);
        customizedSchedules = customizedSchedules == null ? Map.of() : new HashMap<>(customizedSchedules);
    }

    public DriverBusinessDays(int defaultBusinessDays, Map<UUID, Integer> customizedBusinessDays) {
        this(defaultBusinessDays, customizedBusinessDays, Map.of());
    }

    public static DriverBusinessDays uniform(int businessDays) {
        return new DriverBusinessDays(businessDays, Map.of());
    }

    public Map<LocalDate, OrdinaryWindow> scheduleOverridesOf(UUID driverId) {
        return driverId == null ? Map.of() : customizedSchedules.getOrDefault(driverId, Map.of());
    }

    public DriverSchedule scheduleOf(Driver driver) {
        return new DriverSchedule(OrdinaryWindow.of(driver), driver == null ? Map.of() : scheduleOverridesOf(driver.getId()));
    }

    public boolean isCustomized(UUID driverId) {
        return driverId != null && customizedBusinessDays.containsKey(driverId);
    }

    public int businessDaysOf(UUID driverId) {
        return isCustomized(driverId) ? customizedBusinessDays.get(driverId) : defaultBusinessDays;
    }

    public int businessDaysOf(Driver driver) {
        return businessDaysOf(driver == null ? null : driver.getId());
    }

    public double surplusCapOf(UUID driverId) {
        return DriverHoursRules.surplusCap(businessDaysOf(driverId));
    }

    public double surplusCapOf(Driver driver) {
        return surplusCapOf(driver == null ? null : driver.getId());
    }

    public double defaultSurplusCap() {
        return DriverHoursRules.surplusCap(defaultBusinessDays);
    }

    public double surplusTargetOf(UUID driverId) {
        return surplusCapOf(driverId) * DriverHoursRules.SURPLUS_CUTOFF_RATIO;
    }

    public double surplusTargetOf(Driver driver) {
        return surplusTargetOf(driver == null ? null : driver.getId());
    }
}
