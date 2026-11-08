package com.proseg.msvc_transport.entity;

import java.time.LocalTime;

public enum UlloaShift {
    SHORT_MORNING(LocalTime.of(6, 30), LocalTime.of(12, 0)),
    MORNING(LocalTime.of(6, 30), LocalTime.of(17, 30)),
    AFTERNOON(LocalTime.of(12, 0), LocalTime.of(22, 0));

    private final LocalTime departureTime;
    private final LocalTime returnTime;

    UlloaShift(LocalTime departureTime, LocalTime returnTime) {
        this.departureTime = departureTime;
        this.returnTime = returnTime;
    }

    public LocalTime departureTime() {
        return departureTime;
    }

    public LocalTime returnTime() {
        return returnTime;
    }

    public boolean matches(LocalTime departure, LocalTime arrival) {
        return departureTime.equals(departure) && returnTime.equals(arrival);
    }
}
