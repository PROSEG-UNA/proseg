package com.sssi.msvc_transport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentGenerateResponseDto {
    private int totalToursProcessed;
    private int assignedCount;
    private int contractedCount;
    private int rejectedCount;
}
