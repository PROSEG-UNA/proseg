package com.proseg.msvc_transport.dto.tourprocessing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourProcessingDriverAutoAssignResponseDto {
    private int clearedTours;
    private int assignedTours;
    private int unassignedTours;
    private List<TourIssue> unassignedDetails;
    private List<TourIssue> preservedAssignmentWarnings;
    private Map<String, Double> surplusDeficits;
    private double surplusTargetHours;
    private boolean optimizationLimitReached;

    public record TourIssue(UUID rowId, String number, List<String> reasons) {}
}
