package com.sssi.msvc_maintenance.publisher;

import com.sssi.common.kafka.events.MaintenanceRequestNotificationEvent;
import com.sssi.common.kafka.topics.KafkaTopics;
import com.sssi.msvc_maintenance.config.MaintenanceNotificationProperties;
import com.sssi.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryCampusResponseDto;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import com.sssi.msvc_maintenance.event.MaintenanceRequestNotificationDomainEvent;
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
public class MaintenanceRequestNotificationKafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MaintenanceLocationService maintenanceLocationService;
    private final MaintenanceNotificationProperties notificationProperties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMaintenanceRequestChanged(MaintenanceRequestNotificationDomainEvent event) {
        try {
            kafkaTemplate.send(
                    KafkaTopics.MAINTENANCE_REQUEST_NOTIFICATION_TOPIC,
                    MaintenanceRequestNotificationEvent.builder()
                            .requestId(event.requestId())
                            .actionType(event.actionType())
                            .actionLabel(event.actionLabel())
                            .detail(event.detail())
                            .companyName(event.companyName())
                            .legalId(event.legalId())
                            .description(event.description())
                            .cancellationReason(event.cancellationReason())
                            .status(toStatusLabel(event.status()))
                            .startDate(event.startDate())
                            .endDate(event.endDate())
                            .startTime(event.startTime())
                            .endTime(event.endTime())
                            .campusName(resolveCampusName(event.campusId()))
                            .buildingName(resolveBuildingName(event.buildingId()))
                            .changedFields(event.changedFields())
                            .recipientEmails(cleanEmails(event.recipientEmails()))
                            .extraEmails(cleanEmails(notificationProperties.getExtraEmails()))
                            .timestamp(event.timestamp())
                            .build()
            );
        } catch (Exception ex) {
            log.warn("No se pudo publicar evento Kafka de solicitud modificada: {}", ex.getMessage());
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

    private String toStatusLabel(String statusValue) {
        if (statusValue == null || statusValue.isBlank()) {
            return null;
        }
        try {
            return toStatusLabel(MaintenanceStatus.valueOf(statusValue));
        } catch (IllegalArgumentException ex) {
            return statusValue;
        }
    }

    private String toStatusLabel(MaintenanceStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "Pendiente";
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
}
