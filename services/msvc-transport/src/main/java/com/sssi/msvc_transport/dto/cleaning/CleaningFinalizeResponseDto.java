package com.sssi.msvc_transport.dto.cleaning;

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
public class CleaningFinalizeResponseDto {
    private List<CleaningRowDto> cleanedRows;
    private int originalRows;
    private int removedRows;
    private int remainingRows;
}
