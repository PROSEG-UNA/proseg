package com.proseg.common.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInvitedEvent {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String invitationToken;
    private long timestamp;
}