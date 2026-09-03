package com.proseg.msvcinventory.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetSchemaDto {

    private List<AssetColumnSchemaDto> columns;
}
