package com.sssi.msvcinventory.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetModelResponseDto {

    private UUID id;
    private String name;
    private BrandResponseDto brand;
    private AssetTypeResponseDto assetType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
