package com.sssi.msvc_maintenance.publisher;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.kafka.events.MaintenanceRequestCreatedEvent;
import com.sssi.common.kafka.topics.KafkaTopics;
import com.sssi.msvc_maintenance.client.AuthClient;
import com.sssi.msvc_maintenance.config.MaintenanceNotificationProperties;
import com.sssi.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryCampusResponseDto;
import com.sssi.msvc_maintenance.dto.response.KeycloakUserDto;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import com.sssi.msvc_maintenance.event.MaintenanceRequestCreatedDomainEvent;
import com.sssi.msvc_maintenance.service.MaintenanceLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenanceRequestCreatedKafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MaintenanceLocationService maintenanceLocationService;
    private final AuthClient authClient;
    private final MaintenanceNotificationProperties notificationProperties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMaintenanceRequestCreated(MaintenanceRequestCreatedDomainEvent event) {
        try {
            kafkaTemplate.send(
                    KafkaTopics.MAINTENANCE_REQUEST_CREATED_TOPIC,
                    MaintenanceRequestCreatedEvent.builder()
                            .requestId(event.requestId())
                            .emails(cleanEmails(event.emails()))
                            .extraEmails(cleanEmails(notificationProperties.getExtraEmails()))
                            .companyName(event.companyName())
                            .legalId(event.legalId())
                            .description(event.description())
                            .status(toStatusLabel(event.status()))
                            .startDate(event.startDate())
                            .endDate(event.endDate())
                            .startTime(event.startTime())
                            .endTime(event.endTime())
                            .campusName(resolveCampusName(event.campusId()))
                            .buildingName(resolveBuildingName(event.buildingId()))
                            .technicianNames(resolveUserNames(event.technicianKeycloakIds()))
                            .responsibleName(resolveUserName(event.responsibleKeycloakId()))
                            .timestamp(System.currentTimeMillis())
                            .build()
            );
        } catch (Exception ex) {
            log.warn("No se pudo publicar evento Kafka de solicitud creada: {}", ex.getMessage());
        }
    }

    private List<String> cleanEmails(List<String> emails) {
        if (emails == null) {
            return List.of();
        }
        return emails.stream()
                .filter(email -> email != null && !email.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String toStatusLabel(MaintenanceStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "Pendiente";
            case ACCEPTED -> "Aceptada";
            case COMPLETED -> "Completado";
            case CANCELLED -> "Cancelado";
        };
    }

    private String resolveCampusName(UUID campusId) {
        if (campusId == null) {
            return null;
        }
        try {
            InventoryCampusResponseDto campus = maintenanceLocationService.findCampusById(campusId);
            return campus != null ? campus.getName() : null;
        } catch (Exception ex) {
            log.warn("No se pudo resolver el campus {}: {}", campusId, ex.getMessage());
            return null;
        }
    }

    private String resolveBuildingName(UUID buildingId) {
        if (buildingId == null) {
            return null;
        }
        try {
            InventoryBuildingResponseDto building = maintenanceLocationService.findBuildingById(buildingId);
            return building != null ? building.getName() : null;
        } catch (Exception ex) {
            log.warn("No se pudo resolver el edificio {}: {}", buildingId, ex.getMessage());
            return null;
        }
    }

    private List<String> resolveUserNames(List<String> keycloakUserIds) {
        if (keycloakUserIds == null || keycloakUserIds.isEmpty()) {
            return List.of();
        }
        return keycloakUserIds.stream()
                .map(this::resolveUserName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    private String resolveUserName(String keycloakUserId) {
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            return null;
        }
        try {
            ApiResponse<KeycloakUserDto> response = authClient.getUserById(keycloakUserId);
            KeycloakUserDto user = response != null ? response.getData() : null;
            if (user == null) {
                return null;
            }
            return ((nullToEmpty(user.getFirstName()) + " " + nullToEmpty(user.getLastName())).trim());
        } catch (Exception ex) {
            log.warn("No se pudo resolver el usuario {}: {}", keycloakUserId, ex.getMessage());
            return null;
        }
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
