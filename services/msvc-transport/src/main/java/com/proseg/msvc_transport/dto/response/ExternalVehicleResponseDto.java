package com.proseg.msvc_transport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalVehicleResponseDto {
    private UUID id;
    private String provider;
    private String plate;
    private Integer capacity;
    private boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
