package com.sssi.msvc_document_processor.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RowIssueDto {
    private Integer row;

    private String reason;
}
