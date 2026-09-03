package com.proseg.msvcinventory.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetColumnSchemaDto {

    private String attribute;

    private List<String> names;

    private String type;
}
