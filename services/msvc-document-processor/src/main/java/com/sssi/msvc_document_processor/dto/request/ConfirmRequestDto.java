package com.sssi.msvc_document_processor.dto.request;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmRequestDto {

    private List<Map<String, Object>> rows;

    private List<String> approvedKeys;
}
