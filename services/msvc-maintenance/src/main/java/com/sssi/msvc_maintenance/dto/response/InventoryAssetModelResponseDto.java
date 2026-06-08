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
public class InventoryAssetModelResponseDto {
    private UUID id;
    private String name;
    private InventoryNamedRefDto brand;
    private InventoryNamedRefDto type;
}
