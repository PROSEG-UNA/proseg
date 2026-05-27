package com.sssi.msvcinventory.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FloorResponseDto {

    private UUID id;
    private String name;
    private BuildingResponseDto building;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
