package com.proseg.common.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagedUserCreatedEvent {

    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String createdByUserId;
    private String createdByUsername;
    private String createdByEmail;
    private String createdByFirstName;
    private String createdByLastName;
    private long timestamp;
}