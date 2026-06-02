package com.sssi.msvc_maintenance.dto.response;

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
public class InventoryAssetResponseDto {

    private UUID id;
    private String kind;
    private String status;
    private String assetNumber;
    private String serialNumber;
    private InventoryAssetModelResponseDto model;
    private InventoryAssetLocationResponseDto location;
}

