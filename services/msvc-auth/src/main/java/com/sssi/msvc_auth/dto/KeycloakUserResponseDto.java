package com.sssi.msvc_auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KeycloakUserResponseDto {

    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String profileImageObjectName;
    private String profileImageUrl;
    private String status;
    private List<String> permissions;
}