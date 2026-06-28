package com.sssi.msvc_document_processor.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnSchemaDto {
    private String attribute;

    private List<String> names;

    private String type;
}
