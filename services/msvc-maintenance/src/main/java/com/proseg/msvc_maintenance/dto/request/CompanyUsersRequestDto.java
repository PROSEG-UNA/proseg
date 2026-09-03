package com.proseg.msvc_maintenance.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyUsersRequestDto {

    @NotEmpty(message = "Se debe proveer al menos un id de usuario")
    private List<@Pattern(regexp = "[a-zA-Z0-9-_.]{1,255}", message = "Id de usuario inválido") String> keycloakUserIds;
}

