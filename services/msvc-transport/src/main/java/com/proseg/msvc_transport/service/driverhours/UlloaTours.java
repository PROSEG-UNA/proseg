package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.UlloaShift;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

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
