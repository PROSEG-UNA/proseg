package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.UlloaShift;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class UlloaTours {

    private static final String DESTINATION_KEYWORD = "ulloa";

    private UlloaTours() {
    }

    public static Optional<UlloaShift> shiftOf(TourProcessingDraftAssignment tour) {
        if (!DriverHoursRules.isGeneralServices(tour.getExecutingUnit()) || !mentionsUlloa(tour.getDestination())) {
            return Optional.empty();
        }
        if (tour.getDepartureDate() == null || !tour.getDepartureDate().equals(tour.getReturnDate())) {
            return Optional.empty();
        }
        return Arrays.stream(UlloaShift.values())
                .filter(shift -> shift.matches(tour.getDepartureTime(), tour.getReturnTime()))
                .findFirst();
    }

    public static boolean isUlloaTour(TourProcessingDraftAssignment tour) {
        return shiftOf(tour).isPresent();
    }

    public static boolean isPeripheralDriver(Driver driver) {
        return !driver.getUlloaShifts().isEmpty();
    }

    public static boolean isCoveredBy(Driver driver, UlloaShift shift) {
        return isPeripheralDriver(driver) && windowOf(shift).equals(OrdinaryWindow.of(driver));
    }

    public static Set<UlloaShift> shiftsCoveredBy(OrdinaryWindow window) {
        return Arrays.stream(UlloaShift.values())
                .filter(shift -> windowOf(shift).equals(window))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(UlloaShift.class)));
    }

    private static OrdinaryWindow windowOf(UlloaShift shift) {
        return shift == UlloaShift.AFTERNOON ? OrdinaryWindow.AFTERNOON : OrdinaryWindow.STANDARD;
    }

    private static boolean mentionsUlloa(String destination) {
        if (destination == null) {
            return false;
        }
        String normalized = Normalizer.normalize(destination, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.contains(DESTINATION_KEYWORD);
    }
}
