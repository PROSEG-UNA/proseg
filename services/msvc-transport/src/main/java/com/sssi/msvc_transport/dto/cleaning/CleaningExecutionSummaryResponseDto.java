package com.sssi.msvc_transport.dto.cleaning;

import com.sssi.msvc_transport.entity.CleaningExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningExecutionSummaryResponseDto {
    private UUID id;
    private LocalDateTime executedAt;
    private String executedBy;
    private String executedByDisplay;
    private String fileName;
    private String fileType;
    private CleaningExecutionStatus finalStatus;
    private Long durationMs;
    private Integer totalReadRecords;
    private Integer validRecords;
    private Integer invalidRecords;
    private Integer duplicatesDetected;
    private Integer createdDrivers;
    private Integer updatedDrivers;
    private Integer createdVehicles;
    private Integer updatedVehicles;
    private Integer createdTours;
    private Integer updatedTours;
}
