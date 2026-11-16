package com.proseg.msvc_transport.service.autoassign;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.UlloaTours;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AutoAssignStepCatalog {

    public static final String MANDATORY = "MANDATORY";
    public static final String PERIPHERAL = "PERIPHERAL";
    public static final String FILTER_PREFIX = "WIZARD_STEP:";

    private static final String PRIORITY_PREFIX = "PRIORITY_";
    private static final String EXTRA_SUFFIX = "_EXTRA";
    private static final int MINIMUM_PASSENGERS = 5;
    private static final double HOURS_EPSILON = 1e-9;

    public static final Comparator<String> STEP_ORDER = Comparator
            .comparingInt(AutoAssignStepCatalog::stage)
            .thenComparingInt(AutoAssignStepCatalog::priorityOf);

    private AutoAssignStepCatalog() {
    }

    public static String stepCodeOf(TourProcessingDraftTour tour, Map<UUID, List<TourProcessingDraftDay>> daysByAssignmentId) {
        int priority = tour.getPriority() == null ? 1 : tour.getPriority();
        TourProcessingDraftAssignment primary = tour.primaryAssignment();
        if (priority <= 1 && tour.getPassengers() != null && tour.getPassengers() < MINIMUM_PASSENGERS) {
            return MANDATORY;
        }
        if (primary != null && UlloaTours.isUlloaTour(primary)) {
            return PERIPHERAL;
        }
        return PRIORITY_PREFIX + priority + (needsExtra(tour, daysByAssignmentId) ? EXTRA_SUFFIX : "");
    }

    public static boolean needsExtra(TourProcessingDraftTour tour, Map<UUID, List<TourProcessingDraftDay>> daysByAssignmentId) {
        boolean extraHours = tour.getAssignments().stream()
                .map(TourProcessingDraftAssignment::getId)
                .map(daysByAssignmentId::get)
                .filter(days -> days != null)
                .flatMap(List::stream)
                .anyMatch(day -> day.getOvertimeHours() != null && day.getOvertimeHours() > HOURS_EPSILON);
        return extraHours || touchesWeekend(tour);
    }

    public static String labelOf(String code) {
        if (MANDATORY.equals(code)) {
            return "Prioridad 1 con menos de 5 pasajeros";
        }
        if (PERIPHERAL.equals(code)) {
            return "Giras a la periférica";
        }
        int priority = priorityOf(code);
        if (stage(code) == 3) {
            return "Prioridad " + priority + " con fin de semana u horas extra";
        }
        return priority == 1 ? "Prioridad 1 restantes, sin horas extra" : "Prioridad " + priority + ", sin horas extra";
    }

    public static boolean isStepCode(String code) {
        return code != null && (MANDATORY.equals(code) || PERIPHERAL.equals(code) || code.matches("PRIORITY_\\d+(_EXTRA)?"));
    }

    private static boolean touchesWeekend(TourProcessingDraftTour tour) {
        LocalDate from = tour.getDepartureDate();
        LocalDate to = tour.getReturnDate();
        if (from == null || to == null) {
            return false;
        }
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (DriverHoursRules.isWeekend(date)) {
                return true;
            }
        }
        return false;
    }

    private static int stage(String code) {
        if (MANDATORY.equals(code)) {
            return 0;
        }
        if (PERIPHERAL.equals(code)) {
            return 1;
        }
        return code.endsWith(EXTRA_SUFFIX) ? 3 : 2;
    }

    private static int priorityOf(String code) {
        if (!code.startsWith(PRIORITY_PREFIX)) {
            return 0;
        }
        String digits = code.substring(PRIORITY_PREFIX.length()).replace(EXTRA_SUFFIX, "");
        return Integer.parseInt(digits);
    }
}
