package com.proseg.msvc_document_processor.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourImportSummaryDto {
    private int totalRows;

    private int blocksDetected;

    private String sourceFormat;

    private UUID draftId;

    private int duplicateRows;

    private List<RowIssueDto> errors;
}
