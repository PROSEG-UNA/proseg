package com.sssi.msvc_maintenance.dto.request;

import com.sssi.common.utils.ValidationUtils;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

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

    @Size(max = 600, message = "La descripción no puede superar los 600 caracteres")
    @Pattern(
            regexp = ValidationUtils.SAFE_TEXT_REGEX,
            message = "La descripción contiene caracteres inválidos"
    )
    private String description;

    private MaintenanceStatus status;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate endDate;

    @NotNull(message = "La hora de llegada es obligatoria")
    private LocalTime startTime;

    @NotNull(message = "La hora de salida es obligatoria")
    private LocalTime endTime;

    @NotBlank(message = "El campus es obligatorio")
    @Pattern(
            regexp = ValidationUtils.UUID_REGEX,
            message = "El id del campus debe tener un formato UUID válido"
    )
    private String campusId;

    private UUID buildingId;

    @NotEmpty(message = "Se requiere al menos un técnico asignado")
    private List<UUID> assignedTechnicianIds;

    private UUID leaderUserCompanyId;

    @NotEmpty(message = "Se requiere al menos un correo electrónico")
    private List<@Pattern(
            regexp = ValidationUtils.EMAIL_REGEX,
            message = "El correo electrónico tiene un formato inválido"
    ) String> emails;
}