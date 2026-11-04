package com.proseg.msvc_transport.dto.tourprocessing;

import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class TourProcessingDraftRowResponseDto {
    private UUID id;
    private int rowNumber;
    private String driver;
    private String number;
    private String vehicle;
    private ResourceType resourceType;
    private UUID borrowedVehicleId;
    private String borrowedVehiclePlate;
    private boolean pendingContracting;
    private TourApprovalStatus approvalStatus;
    private Integer passengers;
    private List<TourProcessingDraftAssignmentDto> assignments;
    private String executingUnit;
    private String responsible;
    private String destination;
    private Integer durationDays;
    private Integer priority;
    private String modality;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalTime departureTime;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalTime returnTime;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalDate departureDate;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalDate returnDate;
    private String observations;
    private int duplicateGroupSize;
    private int duplicateRank;
    private boolean conflicting;
    private List<TourProcessingDraftRowDayResponseDto> days;
}
