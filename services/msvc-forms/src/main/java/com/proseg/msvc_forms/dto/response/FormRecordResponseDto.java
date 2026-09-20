package com.proseg.msvc_forms.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormRecordResponseDto {

    private UUID id;

    private UUID formTypeId;

    private String formTypeName;

    private String formTypeCode;

    private JsonNode data;

    private String createdBy;

    private String createdByName;

    private OffsetDateTime createdAt;

    private String updatedBy;

    private OffsetDateTime updatedAt;
}
