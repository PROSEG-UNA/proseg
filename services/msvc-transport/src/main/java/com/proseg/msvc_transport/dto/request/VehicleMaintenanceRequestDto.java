package com.proseg.msvc_transport.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleMaintenanceRequestDto {

    @NotNull
    private UUID vehicleId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 100)
    private String type;

    private LocalDate scheduledDate;

    private BigDecimal cost;

    @Builder.Default
    private String status = "PLANNED";

    private String notes;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
