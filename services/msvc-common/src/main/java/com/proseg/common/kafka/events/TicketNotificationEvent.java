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
public class TicketNotificationEvent {

    private UUID ticketId;
    private String actionType;
    private String actionLabel;
    private String actorId;
    private String actorName;
    private String ticketTitle;
    private String ticketDescription;
    private String status;
    private String priority;
    private String campusName;
    private String buildingName;
    private String detail;
    private List<String> changedFields;
    private List<String> recipientEmails;
    private List<String> extraEmails;
    private Long timestamp;
}
