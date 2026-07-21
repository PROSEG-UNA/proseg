package com.sssi.msvc_transport.dto.cleaning;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningRegisterRequestDto {
    @NotNull
    private List<CleaningRowDto> rows;

    @Builder.Default
    private boolean replaceExistingInRange = false;

    private CleaningAuditMetadataDto audit;
}
