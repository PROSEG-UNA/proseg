package com.sssi.msvcinventory.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingCreationDto {

    private String key;

    private String label;

    private String message;
}
