package com.proseg.msvcinventory.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDashboardResponseDto {

    private LocalDate referenceDate;
    private int days;
    private long totalAssets;
    private long activeAssets;
    private long decommissionedAssets;
    private ExpiryStatsDto warranty;
    private ExpiryStatsDto firmware;
    private List<TypeCountDto> byType;
}
