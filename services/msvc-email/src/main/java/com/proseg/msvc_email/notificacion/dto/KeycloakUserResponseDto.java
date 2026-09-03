package com.proseg.msvc_email.notificacion.dto;

import lombok.Data;

@Data
public class KeycloakUserResponseDto {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
}
