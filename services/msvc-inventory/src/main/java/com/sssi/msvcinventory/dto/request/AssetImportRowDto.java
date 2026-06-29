package com.sssi.msvcinventory.dto.request;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetImportRowDto {

    private int rowNumber;

    private String assetNumber;

    private String executingUnit;

    private String typeName;

    private String brandName;

    private String modelName;

    private String serialNumber;

    private String campusName;

    private String buildingName;

    private String floorName;

    private String locationName;

    private String responsibleEmployeeId;

    private String responsibleEmployee;

    private String status;

    private LocalDate acquisitionDate;

    private LocalDate warrantyEndDate;

    private LocalDate firmwareSupportEndDate;

    private String ipAddress;

    private String macAddress;

    private BigDecimal latitude;

    private BigDecimal longitude;
}
