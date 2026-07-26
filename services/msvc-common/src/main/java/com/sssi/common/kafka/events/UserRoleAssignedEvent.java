package com.sssi.common.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleAssignedEvent {

    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String roleId;
    private String roleName;
    private String assignedByUserId;
    private String assignedByUsername;
    private String assignedByEmail;
    private String assignedByFirstName;
    private String assignedByLastName;
    private long timestamp;
}
