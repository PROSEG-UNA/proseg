package com.proseg.msvc_document_processor.dto.request;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourDraftRequestDto {
    private String fileName;

    private String fileType;

    private int totalRowsRead;

    private int blocksDetected;

    private List<Map<String, Object>> rows;
}
