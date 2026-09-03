package com.proseg.msvc_maintenance.dto.request;

import com.proseg.common.utils.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class CompanyUserRequestDto {

    @NotBlank(message = "El id del usuario de Keycloak es obligatorio")
    @Pattern(
            regexp = ValidationUtils.KEYCLOAK_ID_REGEX,
            message = "El id del usuario contiene caracteres inválidos"
    )
    private String keycloakUserId;
}

