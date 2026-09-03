package com.proseg.msvc_maintenance.dto.response;

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
public class TicketCommentResponseDto {
    private UUID id;
    private String authorId;
    private String authorName;
    private String content;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
