package com.sssi.msvc_maintenance.websocket;

import com.sssi.msvc_maintenance.entity.enums.TicketPriority;
import com.sssi.msvc_maintenance.entity.enums.TicketStatus;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketWebSocketEventDto {

    private String type;
    private UUID ticketId;
    private String createdBy;
    private String assignedRole;
    private TicketStatus status;
    private TicketPriority priority;
}
