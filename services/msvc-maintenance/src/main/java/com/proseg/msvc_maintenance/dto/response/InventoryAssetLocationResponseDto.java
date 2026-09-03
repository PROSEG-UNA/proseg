package com.proseg.msvc_maintenance.dto.response;

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
public class InventoryAssetLocationResponseDto {
    private UUID id;
    private String description;
    private InventoryAssetFloorResponseDto floor;
}
