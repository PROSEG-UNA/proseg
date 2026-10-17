package com.proseg.msvc_transport.dto.request;

import com.proseg.msvc_transport.entity.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private String restrictions;

    @NotNull
    @Builder.Default
    private Set<VehicleType> allowedVehicleTypes = EnumSet.allOf(VehicleType.class);
}
