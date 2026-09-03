package com.proseg.msvc_document_processor.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportPreviewDto {

    private int received;

    private List<RowIssueDto> errors;

    private List<PendingCreationDto> pendingCreations;
}
