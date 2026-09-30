package com.proseg.msvc_transport.dto.cleaning;

import lombok.AllArgsConstructor;
import lombok.Builder;
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
public class CleaningDraftRowResponseDto {
    private UUID id;
    private int rowNumber;
    private String driver;
    private String number;
    private String vehicle;
    private Integer passengers;
    private String executingUnit;
    private String responsible;
    private String destination;
    private Integer durationDays;
    private Integer priority;
    private String modality;
    private LocalTime departureTime;
    private LocalTime returnTime;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private String observations;
    private int duplicateGroupSize;
    private int duplicateRank;
    private boolean conflicting;
}
