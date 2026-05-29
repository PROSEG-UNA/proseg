package com.sssi.msvc_maintenance.dto.request;

import com.sssi.common.utils.ValidationUtils;
import com.sssi.msvc_maintenance.entity.enums.MaintenancePriority;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRequestRequestDto {

    @NotBlank(message = "La empresa es obligatoria")
    @Pattern(
            regexp = ValidationUtils.UUID_REGEX,
            message = "El id de la empresa debe tener un formato UUID válido"
    )
    private String companyId;

    @NotBlank(message = "El activo es obligatorio")
    @Pattern(
            regexp = ValidationUtils.UUID_REGEX,
            message = "El id del activo debe tener un formato UUID válido"
    )
    private String assetId;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    @Pattern(
            regexp = ValidationUtils.SAFE_TEXT_REGEX,
            message = "El título contiene caracteres inválidos"
    )
    private String title;

    @Size(max = 600, message = "La descripción no puede superar los 600 caracteres")
    @Pattern(
            regexp = ValidationUtils.SAFE_TEXT_REGEX,
            message = "La descripción contiene caracteres inválidos"
    )
    private String description;

    @NotNull(message = "El estado es obligatorio")
    private MaintenanceStatus status;

    @NotNull(message = "La prioridad es obligatoria")
    private MaintenancePriority priority;

    private LocalDate scheduledDate;

    @Size(max = 600, message = "Las observaciones no pueden superar los 600 caracteres")
    @Pattern(
            regexp = ValidationUtils.SAFE_TEXT_REGEX,
            message = "Las observaciones contienen caracteres inválidos"
    )
    private String observations;
}