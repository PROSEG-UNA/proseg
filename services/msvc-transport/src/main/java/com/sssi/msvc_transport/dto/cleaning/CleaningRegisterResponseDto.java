package com.sssi.msvc_transport.dto.cleaning;

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
public class CleaningRegisterResponseDto {
    private UUID cleaningExecutionId;
    private int requestedRows;
    private int importedRows;
    private int replacedRows;
    private int createdDrivers;
    private int updatedDrivers;
    private int createdVehicles;
    private int updatedVehicles;
}
