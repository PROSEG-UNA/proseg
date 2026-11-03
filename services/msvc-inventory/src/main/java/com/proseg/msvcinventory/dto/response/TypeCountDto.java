package com.proseg.msvcinventory.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeCountDto {

    private String name;
    private long total;
}
