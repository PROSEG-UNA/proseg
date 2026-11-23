package com.proseg.msvc_transport.dto.request;

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
public class ExternalVehicleRequestDto {

    @NotBlank
    @Size(max = 200)
    private String provider;

    @NotBlank
    @Size(max = 20)
    private String plate;

    @Min(1)
    private Integer capacity;

    @Builder.Default
    private boolean available = true;
}
