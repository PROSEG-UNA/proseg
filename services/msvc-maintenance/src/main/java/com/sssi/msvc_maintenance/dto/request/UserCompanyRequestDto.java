package com.sssi.msvc_maintenance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCompanyRequestDto {

    @NotBlank(message = "El id del usuario de Keycloak es obligatorio")
    private String keycloakUserId;

    @NotNull(message = "La compañía es obligatoria")
    private UUID companyId;
}