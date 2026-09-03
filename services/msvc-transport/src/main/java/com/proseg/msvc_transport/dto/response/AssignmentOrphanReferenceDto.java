package com.proseg.msvc_transport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentOrphanReferenceDto {
    private UUID assignmentId;
    private UUID driverId;
    private UUID vehicleId;
    private UUID tourId;
    private boolean missingDriver;
    private boolean missingVehicle;
    private boolean missingTour;
}

