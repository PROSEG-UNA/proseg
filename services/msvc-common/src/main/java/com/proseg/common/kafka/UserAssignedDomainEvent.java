package com.proseg.common.kafka;

import java.util.List;
import java.util.UUID;

public record UserAssignedDomainEvent(UUID companyId, List<String> keycloakUserIds) {}