package com.sssi.msvc_maintenance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketPhotoResponseDto {
    private UUID id;
    private String objectName;
    private String fileName;
    private String contentType;
    private String imageUrl;
}
