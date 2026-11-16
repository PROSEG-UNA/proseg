package com.proseg.msvc_transport.dto.tourprocessing;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record TourProcessingAutoAssignWizardDto(
        boolean active,
        String currentStep,
        boolean dirty,
        List<Step> steps,
        JsonNode lastResult
) {

    public static TourProcessingAutoAssignWizardDto inactive() {
        return new TourProcessingAutoAssignWizardDto(false, null, false, List.of(), null);
    }

    public record Step(
            String code,
            String label,
            String filterRule,
            int tours,
            int complete,
            int withoutDriver,
            int withoutVehicle,
            List<DriverLoad> drivers
    ) {
    }

    public record DriverLoad(String driver, int tours, List<String> tourNumbers) {
    }

    public record ProgressRequest(String currentStep) {
    }
}
