package com.proseg.msvc_transport.dto.request;

import com.proseg.msvc_transport.entity.UlloaShift;
import com.proseg.msvc_transport.entity.DriverUnit;
import com.proseg.msvc_transport.entity.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverRequestDto {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Builder.Default
    private String status = "ACTIVE";

    private DriverUnit organizationalUnit;

    private String restrictions;

    @NotNull
    @Builder.Default
    private Set<VehicleType> allowedVehicleTypes = EnumSet.allOf(VehicleType.class);

    private LocalTime ordinaryStart;

    private LocalTime ordinaryEnd;

    @NotNull
    @Builder.Default
    private Set<UlloaShift> ulloaShifts = EnumSet.noneOf(UlloaShift.class);
}
