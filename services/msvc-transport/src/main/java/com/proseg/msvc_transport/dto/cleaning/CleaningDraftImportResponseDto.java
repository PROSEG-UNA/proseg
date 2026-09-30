package com.proseg.msvc_transport.dto.cleaning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningDraftImportResponseDto {
    private UUID draftId;
    private int received;
    private int duplicateRows;
    private int conflictingRows;
    private List<CleaningDraftRowIssueDto> errors;
}
