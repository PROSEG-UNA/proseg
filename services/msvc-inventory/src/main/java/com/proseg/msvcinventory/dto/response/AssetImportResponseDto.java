package com.proseg.msvcinventory.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetImportResponseDto {

    private int received;

    private int created;

    private boolean cancelled;

    private List<AssetImportRowIssueDto> errors;
}
