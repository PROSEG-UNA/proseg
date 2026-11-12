package com.proseg.msvc_forms.dto.response;

public record KeycloakUserResponseDto(
        String id,
        String username,
        String email,
        String firstName,
        String lastName
) {
}
