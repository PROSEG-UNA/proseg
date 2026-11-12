package com.proseg.msvc_transport.service.driverhours;

public final class WeekendCap {

    private static final int FIVE_WEEKENDS = 5;
    private static final int CAP_WITH_FIVE_WEEKENDS = 3;
    private static final int CAP_OTHERWISE = 2;

    private WeekendCap() {
    }

    public static int forWeekendsInMonth(int weekendsInMonth) {
        return weekendsInMonth == FIVE_WEEKENDS ? CAP_WITH_FIVE_WEEKENDS : CAP_OTHERWISE;
    }
}
