package com.sssi.msvcinventory.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkInterfaceResponseDto {

    private UUID id;
    private String ipAddress;
    private String macAddress;
    private UUID assetId;
}