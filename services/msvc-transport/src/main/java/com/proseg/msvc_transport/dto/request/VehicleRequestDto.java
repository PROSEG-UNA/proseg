package com.proseg.msvc_transport.dto.request;

import com.proseg.msvc_transport.entity.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequestDto {

    @NotBlank
    @Size(max = 20)
    private String plate;

    @Min(1)
    private Integer capacity;

    private VehicleType type;

    @Builder.Default
    private boolean available = true;
}
