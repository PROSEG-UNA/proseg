package com.proseg.msvc_maintenance.dto.request;

import com.proseg.common.utils.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecordRequestDto {

    @NotNull(message = "El activo es obligatorio")
    private UUID assetId;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 600, message = "La descripción no puede superar los 600 caracteres")
    @Pattern(
            regexp = ValidationUtils.SAFE_TEXT_REGEX,
            message = "La descripción contiene caracteres inválidos"
    )
    private String description;
}
