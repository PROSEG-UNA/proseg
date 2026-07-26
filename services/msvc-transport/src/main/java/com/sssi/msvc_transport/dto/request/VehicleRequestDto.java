package com.sssi.msvc_transport.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequestDto {

    @NotBlank
    @Size(max = 20)
    private String plate;

    @NotBlank
    @Size(max = 100)
    private String brand;

    @NotBlank
    @Size(max = 100)
    private String model;

    @NotNull
    private Integer year;

    @NotNull
    @Min(1)
    private Integer capacity;

    @Builder.Default
    private String status = "ACTIVE";

    @Size(max = 100)
    private String type;

    @Builder.Default
    private boolean available = true;

    @Builder.Default
    private boolean underMaintenance = false;

    private LocalDate lastMaintenanceDate;
}
