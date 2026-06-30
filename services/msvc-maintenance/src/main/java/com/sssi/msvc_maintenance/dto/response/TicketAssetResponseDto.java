package com.sssi.msvc_maintenance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAssetResponseDto {
    private UUID id;
    private UUID assetId;
    private String assetNumber;
    private String serialNumber;
    private String assetName;
    private String modelName;
    private String type;
    private String brand;
    private String locationDescription;
}
