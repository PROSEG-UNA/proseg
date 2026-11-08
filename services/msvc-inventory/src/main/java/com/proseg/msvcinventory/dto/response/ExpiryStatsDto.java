package com.proseg.msvcinventory.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpiryStatsDto {

    private long expired;
    private long expiringSoon;
    private long valid;
    private long noDate;
}
