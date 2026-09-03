package com.proseg.msvc_document_processor.dto.request;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RowsRequestDto {
    private List<Map<String, Object>> rows;
}
