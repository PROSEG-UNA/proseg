package com.sssi.msvc_maintenance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceAssetOptionDto {

    private UUID id;
    private String kind;
    private String status;
    private String assetNumber;
    private String serialNumber;
    private String type;
    private String brand;
    private String modelName;
    private String campusName;
    private String buildingName;
    private String floorName;
    private String locationName;
    private String executingUnit;
    private String responsibleEmployee;
    private String responsibleEmployeeId;
    private LocalDate acquisitionDate;
    private LocalDate warrantyEndDate;
    private LocalDate firmwareSupportEndDate;
    private LocalDate decommissionDate;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
