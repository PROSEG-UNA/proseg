package com.sssi.msvcinventory.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetTypeResponseDto {

    private UUID id;
    private String name;
    private String description;
    private boolean requiresNetworkInterface;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
