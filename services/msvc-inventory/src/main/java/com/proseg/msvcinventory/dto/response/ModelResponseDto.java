package com.proseg.msvcinventory.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelResponseDto {

    private UUID id;
    private String name;
    private BrandResponseDto brand;
    private TypeResponseDto type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
