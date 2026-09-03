package com.proseg.msvc_maintenance.dto.response;

import com.proseg.msvc_maintenance.entity.enums.TicketPriority;
import com.proseg.msvc_maintenance.entity.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketListResponseDto {
    private UUID id;
    private String title;
    private String description;
    private TicketStatus status;
    private String createdBy;
    private String createdByName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
