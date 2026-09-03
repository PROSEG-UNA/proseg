package com.proseg.msvc_maintenance.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryBuildingEmailResponseDto {

    private UUID id;
    private String email;
}
