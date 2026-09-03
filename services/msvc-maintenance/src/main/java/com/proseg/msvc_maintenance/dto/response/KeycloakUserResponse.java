package com.proseg.msvc_maintenance.dto.response;

import java.util.List;

public record KeycloakUserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String status,
        List<String> permissions
) {
}