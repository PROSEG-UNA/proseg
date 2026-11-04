package com.proseg.msvc_transport.dto.tourprocessing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourProcessingVehicleAutoAssignResponseDto {
    private int clearedTours;
    private int assignedTours;
    private int unassignedTours;
    private List<TourIssue> unassignedDetails;
    private List<TourIssue> preservedAssignmentWarnings;

    public record TourIssue(UUID rowId, String number, List<String> reasons) {}
}
