package com.sssi.common.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordExpiringSoonEvent {

    private String keycloakUserId;
    private String email;
    private String firstName;
    private long daysRemaining;
    private Instant expiresAt;
    private long timestamp;
}