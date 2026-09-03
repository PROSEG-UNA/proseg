package com.proseg.msvc_maintenance.publisher;

import com.proseg.common.kafka.events.TicketNotificationEvent;
import com.proseg.common.kafka.topics.KafkaTopics;
import com.proseg.msvc_maintenance.event.TicketNotificationDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketNotificationKafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketChanged(TicketNotificationDomainEvent event) {
        try {
            kafkaTemplate.send(
                    KafkaTopics.TICKET_NOTIFICATION_TOPIC,
                    TicketNotificationEvent.builder()
                            .ticketId(event.ticketId())
                            .actionType(event.actionType())
                            .actionLabel(event.actionLabel())
                            .actorId(event.actorId())
                            .actorName(event.actorName())
                            .ticketTitle(event.ticketTitle())
                            .ticketDescription(event.ticketDescription())
                            .status(event.status())
                            .priority(event.priority())
                            .campusName(event.campusName())
                            .buildingName(event.buildingName())
                            .detail(event.detail())
                            .changedFields(event.changedFields())
                            .recipientEmails(event.recipientEmails())
                            .extraEmails(event.extraEmails())
                            .timestamp(event.timestamp())
                            .build()
            );
        } catch (Exception ex) {
            log.warn("No se pudo publicar evento Kafka de ticket: {}", ex.getMessage());
        }
    }
}
