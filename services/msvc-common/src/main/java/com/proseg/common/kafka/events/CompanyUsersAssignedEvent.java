package com.proseg.common.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyUsersAssignedEvent {

    private UUID companyId;
    private List<String> keycloakUserIds;
    private Long timestamp;
}