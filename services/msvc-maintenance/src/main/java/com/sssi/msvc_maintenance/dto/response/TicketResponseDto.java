package com.sssi.msvc_maintenance.dto.response;

import com.sssi.msvc_maintenance.entity.enums.TicketPriority;
import com.sssi.msvc_maintenance.entity.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponseDto {
    private UUID id;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private String createdBy;
    private String assignedRole;
    private UUID siteId;
    private String siteName;
    private UUID buildingId;
    private String buildingName;
    private UUID floorId;
    private String floorName;
    private UUID locationId;
    private String locationDescription;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<TicketAssetResponseDto> assets;
    private List<TicketPhotoResponseDto> photos;
    private List<TicketCommentResponseDto> comments;
}