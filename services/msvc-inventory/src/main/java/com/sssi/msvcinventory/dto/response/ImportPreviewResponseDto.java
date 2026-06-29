package com.sssi.msvcinventory.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportPreviewResponseDto {

    private int received;

    private List<AssetImportRowIssueDto> errors;

    private List<PendingCreationDto> pendingCreations;
}
