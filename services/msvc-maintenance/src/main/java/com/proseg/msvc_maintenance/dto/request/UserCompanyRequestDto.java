package com.proseg.msvc_maintenance.dto.request;

import com.proseg.common.utils.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCompanyRequestDto {

    @NotBlank(message = "El id del usuario de Keycloak es obligatorio")
    @Pattern(
            regexp = ValidationUtils.KEYCLOAK_ID_REGEX,
            message = "El id del usuario contiene caracteres inválidos"
    )
    private String keycloakUserId;

    @NotBlank(message = "La empresa es obligatoria")
    @Pattern(
            regexp = ValidationUtils.UUID_REGEX,
            message = "El id de la empresa debe tener un formato UUID válido"
    )
    private String companyId;

    @Pattern(
            regexp = ValidationUtils.EMAIL_REGEX,
            message = "El correo electrónico tiene un formato inválido"
    )
    private String userEmail;
}