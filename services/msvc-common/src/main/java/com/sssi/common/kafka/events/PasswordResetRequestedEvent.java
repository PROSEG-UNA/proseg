package com.sssi.common.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestedEvent {
    private String keycloakUserId;
    private String email;
    private String firstName;
    private String resetToken;
    private long timestamp;
}