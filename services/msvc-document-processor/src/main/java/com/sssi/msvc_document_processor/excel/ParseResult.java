package com.sssi.msvc_document_processor.excel;

import com.sssi.msvc_document_processor.dto.response.RowIssueDto;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParseResult {

    private int totalRows;

    private List<Map<String, Object>> rows;

    private List<RowIssueDto> errors;
}
