package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;

public record OrdinaryWindow(double start, double end) {

    public static final OrdinaryWindow STANDARD = new OrdinaryWindow(DriverHoursRules.ORDINARY_START, DriverHoursRules.ORDINARY_END);

    public static OrdinaryWindow of(Driver driver) {
        if (driver == null || driver.getOrdinaryStart() == null || driver.getOrdinaryEnd() == null) {
            return STANDARD;
        }
        return new OrdinaryWindow(DriverHoursRules.hourOf(driver.getOrdinaryStart()), DriverHoursRules.hourOf(driver.getOrdinaryEnd()));
    }

    public double hours() {
        return end - start;
    }
}
