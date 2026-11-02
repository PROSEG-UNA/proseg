package com.proseg.msvc_transport.dto.tourprocessing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class TourProcessingDraftRowDayResponseDto {
    private UUID id;
    private LocalDate date;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double ordinaryHours;
    private Double surplusHours;
    private Double overtimeHours;
    private boolean manuallyEdited;
}
