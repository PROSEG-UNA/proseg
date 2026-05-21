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
public class CompanyRequestDto {

    @NotBlank(message = "El nombre de la compañía es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    @Pattern(
            regexp = ValidationUtils.SAFE_TEXT_REGEX,
            message = "El nombre contiene caracteres inválidos"
    )
    private String name;

    @NotBlank(message = "La cédula jurídica es obligatoria")
    @Size(max = 50, message = "La cédula jurídica no puede superar los 50 caracteres")
    @Pattern(
            regexp = ValidationUtils.LEGAL_ID_REGEX,
            message = "La cédula jurídica contiene caracteres inválidos"
    )
    private String legalId;

    @Email(message = "El correo electrónico no es válido")
    @Size(max = 150, message = "El correo electrónico no puede superar los 150 caracteres")
    private String contactEmail;

    @Size(max = 50, message = "El teléfono no puede superar los 50 caracteres")
    @Pattern(
            regexp = ValidationUtils.PHONE_REGEX,
            message = "El teléfono contiene caracteres inválidos"
    )
    private String contactPhone;

    @Size(max = 1000, message = "La dirección no puede superar los 1000 caracteres")
    @Pattern(
            regexp = ValidationUtils.SAFE_TEXT_REGEX,
            message = "La dirección contiene caracteres inválidos"
    )
    private String address;
}