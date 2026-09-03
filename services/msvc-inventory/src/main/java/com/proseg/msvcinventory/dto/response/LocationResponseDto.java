package com.proseg.msvcinventory.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResponseDto {

    private UUID id;
    private FloorResponseDto floor;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
