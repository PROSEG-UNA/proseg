package com.sssi.msvc_transport.dto.cleaning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningExecutionDetailResponseDto {
    private UUID id;
    private Integer originalRowNumber;
    private String recordType;
    private String actionPerformed;
    private String processingResult;
    private String rejectionReason;
    private String observations;
    private String validationErrors;
    @Builder.Default
    private Map<String, String> originalData = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, String> normalizedData = new LinkedHashMap<>();
}
