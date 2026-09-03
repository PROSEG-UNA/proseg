package com.proseg.msvc_maintenance.publisher;

import com.proseg.common.kafka.UserAssignedDomainEvent;
import com.proseg.common.kafka.events.CompanyUsersAssignedEvent;
import com.proseg.common.kafka.topics.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAssignedKafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserAssigned(UserAssignedDomainEvent event) {
        try {
            kafkaTemplate.send(
                    KafkaTopics.USER_COMPANY_ASSIGNED_TOPIC,
                    CompanyUsersAssignedEvent.builder()
                            .companyId(event.companyId())
                            .keycloakUserIds(event.keycloakUserIds())
                            .timestamp(System.currentTimeMillis())
                            .build()
            );
        } catch (Exception ex) {
            log.warn("No se pudo publicar evento Kafka post-commit: {}", ex.getMessage());
        }
    }
}