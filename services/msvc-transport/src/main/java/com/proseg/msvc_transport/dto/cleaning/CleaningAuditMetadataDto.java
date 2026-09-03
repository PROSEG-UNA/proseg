package com.proseg.msvc_transport.dto.cleaning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningAuditMetadataDto {
    private String fileName;
    private String fileType;
    private LocalDateTime processStartedAt;
    private Integer totalRowsRead;
    private Integer validRows;
    private Integer invalidRows;
    private Integer duplicateRowsDetected;
    @Builder.Default
    private List<Integer> selectedForDeletion = List.of();
    @Builder.Default
    private List<Integer> suggestedRemovals = List.of();
    private Integer removedRows;
    private Integer remainingRows;
}
