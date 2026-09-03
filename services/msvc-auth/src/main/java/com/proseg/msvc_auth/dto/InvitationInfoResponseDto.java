package com.proseg.msvc_auth.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class InvitationInfoResponseDto {

    private String firstName;
    private String lastName;
    private String email;
    private Instant expiresAt;
}