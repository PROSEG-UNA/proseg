package com.sssi.msvc_transport.dto.cleaning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningPreviewResponseDto {
    private List<String> headers;
    private List<CleaningRowDto> rows;
    private List<CleaningDuplicateGroupDto> duplicateGroups;
    private List<Integer> suggestedRemovals;
    private int totalRowsRead;
    private int validRows;
    private int invalidRows;
    private int duplicateRowsDetected;
}
