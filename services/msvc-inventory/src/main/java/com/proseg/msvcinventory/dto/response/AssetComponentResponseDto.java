package com.proseg.msvcinventory.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetComponentResponseDto {

    private UUID id;
    private UUID assetId;
    private String name;
    private Integer quantity;
    private String location;
    private String observations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

