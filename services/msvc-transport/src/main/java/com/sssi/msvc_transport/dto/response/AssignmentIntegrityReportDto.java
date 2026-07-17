package com.sssi.msvc_transport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentIntegrityReportDto {
    private int totalOrphans;
    private int missingDriverCount;
    private int missingVehicleCount;
    private int missingTourCount;
    private List<AssignmentOrphanReferenceDto> rows;
}

