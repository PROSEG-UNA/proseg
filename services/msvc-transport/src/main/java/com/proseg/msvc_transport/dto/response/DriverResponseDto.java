package com.proseg.msvc_transport.dto.response;

import com.proseg.msvc_transport.entity.UlloaShift;
import com.proseg.msvc_transport.entity.DriverUnit;
import com.proseg.msvc_transport.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private DriverUnit organizationalUnit;
    private String restrictions;
    private List<VehicleType> allowedVehicleTypes;
    private LocalTime ordinaryStart;
    private LocalTime ordinaryEnd;
    private List<UlloaShift> ulloaShifts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
