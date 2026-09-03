package com.proseg.msvc_transport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentIntegrityCleanupResponseDto {
    private int clearedDriverReferences;
    private int clearedVehicleReferences;
    private int softDeletedMissingTourAssignments;
}

