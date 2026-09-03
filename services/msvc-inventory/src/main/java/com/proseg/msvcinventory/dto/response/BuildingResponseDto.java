package com.proseg.msvcinventory.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingResponseDto {

    private UUID id;
    private String name;
    private CampusResponseDto campus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
