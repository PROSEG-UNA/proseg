package com.proseg.common.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusChangedEvent {

    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String oldStatus;
    private String newStatus;
    private String changedByUserId;
    private String changedByUsername;
    private String changedByEmail;
    private String changedByFirstName;
    private String changedByLastName;
    private String reason;
    private long timestamp;
}
