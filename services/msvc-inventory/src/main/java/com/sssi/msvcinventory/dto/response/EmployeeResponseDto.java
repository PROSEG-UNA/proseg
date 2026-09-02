package com.sssi.msvcinventory.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponseDto {

    private UUID id;
    private String name;
    private String identification;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
