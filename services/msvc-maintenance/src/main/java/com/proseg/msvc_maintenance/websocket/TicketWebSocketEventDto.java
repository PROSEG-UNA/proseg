package com.proseg.msvc_maintenance.websocket;

import com.proseg.msvc_maintenance.entity.enums.TicketPriority;
import com.proseg.msvc_maintenance.entity.enums.TicketStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketWebSocketEventDto {

    private String type;
    private UUID ticketId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private TicketStatus status;
    private TicketPriority priority;
}
