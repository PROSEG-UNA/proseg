package com.proseg.msvc_transport.dto.cleaning;

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
public class CleaningDriverAutoAssignResponseDto {
    private int clearedTours;
    private int assignedTours;
    private int unassignedTours;
}
