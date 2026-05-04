package com.sssi.msvcinventory.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetImageResponseDto {

    private UUID id;
    private UUID assetId;
    private String url;
    private String caption;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
