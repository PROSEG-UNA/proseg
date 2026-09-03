package com.proseg.msvc_maintenance.dto.response;

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
public class InventoryAssetResponseDto {

    private UUID id;
    private String kind;
    private String status;
    private String assetNumber;
    private String serialNumber;
    private InventoryAssetExecutingUnitResponseDto executingUnit;
    private InventoryAssetEmployeeResponseDto employee;
    private LocalDate acquisitionDate;
    private LocalDate warrantyEndDate;
    private LocalDate firmwareSupportEndDate;
    private LocalDate decommissionDate;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private InventoryAssetModelResponseDto model;
    private InventoryAssetLocationResponseDto location;
}
