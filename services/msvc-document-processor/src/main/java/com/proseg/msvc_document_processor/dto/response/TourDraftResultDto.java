package com.proseg.msvc_document_processor.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourDraftResultDto {
    private UUID draftId;

    private int received;

    private int duplicateRows;

    private int conflictingRows;

    private List<RowIssueDto> errors;
}
