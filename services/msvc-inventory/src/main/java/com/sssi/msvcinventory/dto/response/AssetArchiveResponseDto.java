package com.sssi.msvcinventory.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetArchiveResponseDto {

    private UUID id;
    private String caption;
    private String imageUrl;
}
