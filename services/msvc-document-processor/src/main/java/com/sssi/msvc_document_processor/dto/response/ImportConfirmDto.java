package com.sssi.msvc_document_processor.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportConfirmDto {
    private int received;

    private int created;

    private boolean cancelled;

    private List<RowIssueDto> errors;
}
