package com.proseg.msvc_transport.dto.tourprocessing;

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
public class TourProcessingRegisterResponseDto {
    private UUID tourProcessingExecutionId;
    private int requestedRows;
    private int importedRows;
    private int replacedRows;
    private int createdDrivers;
    private int updatedDrivers;
    private int createdVehicles;
    private int updatedVehicles;
}
