package com.proseg.msvcinventory.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingEmailResponseDto {

    private UUID id;
    private String email;
    private BuildingResponseDto building;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}