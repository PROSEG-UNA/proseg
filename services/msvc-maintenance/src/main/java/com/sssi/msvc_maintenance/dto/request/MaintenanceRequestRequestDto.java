package com.sssi.msvc_maintenance.dto.request;

import com.sssi.msvc_maintenance.entity.enums.MaintenancePriority;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRequestRequestDto {

    @NotNull(message = "La compañía es obligatoria")
    private UUID companyId;

    @NotNull(message = "El activo es obligatorio")
    private UUID assetId;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String title;

    @Size(max = 5000, message = "La descripción no puede superar los 5000 caracteres")
    private String description;

    @NotNull(message = "El estado es obligatorio")
    private MaintenanceStatus status;

    @NotNull(message = "La prioridad es obligatoria")
    private MaintenancePriority priority;

    private LocalDate scheduledDate;

    @Size(max = 5000, message = "Las observaciones no pueden superar los 5000 caracteres")
    private String observations;
}