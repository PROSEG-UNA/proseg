package com.sssi.msvcinventory.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetImportRowIssueDto {

    private Integer row;

    private String reason;
}
