package com.sssi.msvc_document_processor.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaDto {
    private List<ColumnSchemaDto> columns;
}
