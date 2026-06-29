package com.sssi.msvc_document_processor.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryDto {
    private int totalRows;

    private int created;

    private boolean cancelled;

    private List<RowIssueDto> errors;
}
