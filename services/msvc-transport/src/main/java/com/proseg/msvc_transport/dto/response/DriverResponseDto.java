package com.proseg.msvc_transport.dto.response;

import com.proseg.msvc_transport.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverResponseDto {
    private UUID id;
    private String name;
    private String status;
    private String restrictions;
    private List<VehicleType> allowedVehicleTypes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
