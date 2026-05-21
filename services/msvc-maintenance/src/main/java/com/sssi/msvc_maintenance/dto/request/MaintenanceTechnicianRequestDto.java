package com.sssi.msvc_maintenance.dto.request;

import com.sssi.common.utils.ValidationUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceTechnicianRequestDto {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres")
    @Pattern(
            regexp = ValidationUtils.SAFE_TEXT_REGEX,
            message = "El nombre contiene caracteres inválidos"
    )
    private String fullName;

    @Size(max = 100, message = "El puesto no puede superar los 100 caracteres")
    @Pattern(
            regexp = ValidationUtils.SAFE_TEXT_REGEX,
            message = "El puesto contiene caracteres inválidos"
    )
    private String position;

    @Email(message = "El correo electrónico no es válido")
    @Size(max = 150, message = "El correo electrónico no puede superar los 150 caracteres")
    private String email;

    @Size(max = 50, message = "El teléfono no puede superar los 50 caracteres")
    @Pattern(
            regexp = ValidationUtils.PHONE_REGEX,
            message = "El teléfono contiene caracteres inválidos"
    )
    private String phone;

    private boolean leader;
}