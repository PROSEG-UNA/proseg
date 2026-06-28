package com.sssi.msvc_document_processor.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviewSummaryDto {

    private int totalRows;

    private List<RowIssueDto> errors;

    private List<PendingCreationDto> pendingCreations;

    private List<Map<String, Object>> rows;
}
