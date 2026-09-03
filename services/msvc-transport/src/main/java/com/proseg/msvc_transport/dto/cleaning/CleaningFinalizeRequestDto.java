package com.proseg.msvc_transport.dto.cleaning;

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
public class CleaningFinalizeRequestDto {
    @NotNull
    private List<CleaningRowDto> rows;
    @Builder.Default
    private List<Integer> selectedForDeletion = List.of();
    @Builder.Default
    private List<Integer> manualRemovals = List.of();
    @Builder.Default
    private List<Integer> suggestedRemovals = List.of();
    @Builder.Default
    private boolean keepDuplicates = false;
}
