package com.proseg.msvc_maintenance.dto.response;

import com.proseg.msvc_maintenance.entity.enums.TicketHistoryChangeType;
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
public class TicketHistoryChangeResponseDto {
    private UUID id;
    private TicketHistoryChangeType type;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String authorId;
    private String authorName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}