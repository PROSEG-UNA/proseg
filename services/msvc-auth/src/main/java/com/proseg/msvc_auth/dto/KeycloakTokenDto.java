package com.proseg.msvc_auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KeycloakTokenDto {
    private String accessToken;
    private String refreshToken;
}