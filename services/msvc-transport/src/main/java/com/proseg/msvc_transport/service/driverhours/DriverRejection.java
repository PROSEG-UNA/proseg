package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.Driver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record DriverRejection(Reason reason, String message) {

    public enum Reason {
        OVERLAP,
        DAY_HOURS_EXCEEDED,
        WEEKEND_WITHOUT_SURPLUS,
        OVERTIME_CAP_EXCEEDED
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static DriverRejection overlap(Driver driver, CleaningDraftRow other) {
        LocalDateTime start = LocalDateTime.of(other.getDepartureDate(), other.getDepartureTime());
        LocalDateTime end = LocalDateTime.of(other.getReturnDate(), other.getReturnTime());
        return new DriverRejection(Reason.OVERLAP, "El chofer " + driver.getName() + " ya está asignado a la gira "
                + other.getNumber() + " (del " + start.format(DATE_TIME) + " al " + end.format(DATE_TIME)
                + "), que se cruza con las fechas de esta gira.");
    }

    static DriverRejection dayHoursExceeded(Driver driver, LocalDate date, double hoursAlready, double tourHours) {
        return new DriverRejection(Reason.DAY_HOURS_EXCEEDED, "El chofer " + driver.getName() + " ya trabaja "
                + hours(hoursAlready) + " horas el " + date.format(DATE) + " y esta gira le suma " + hours(tourHours)
                + ", lo que supera el máximo de " + hours(DriverHoursRules.MAX_STACKED_DAY_HOURS)
                + " horas por día.");
    }

    static DriverRejection weekendWithoutSurplus(Driver driver, double surplusHours, double surplusCap) {
        return new DriverRejection(Reason.WEEKEND_WITHOUT_SURPLUS, "Esta gira abarca fin de semana y el chofer "
                + driver.getName() + " aún no alcanza el 95% de su sobresueldo del mes (lleva " + hours(surplusHours)
                + " de " + hours(surplusCap) + " horas).");
    }

    static DriverRejection overtimeCapExceeded(Driver driver, double overtimeHours) {
        return new DriverRejection(Reason.OVERTIME_CAP_EXCEEDED, "Con esta gira el chofer " + driver.getName()
                + " llegaría a " + hours(overtimeHours) + " horas extra en el mes, por encima del tope de "
                + hours(DriverHoursRules.MONTHLY_OVERTIME_CAP) + " horas.");
    }

    private static String hours(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
