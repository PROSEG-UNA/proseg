package com.proseg.msvc_transport.dto.tourprocessing;

import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourProcessingDraftResponseDto {
    private UUID id;
    private String fileName;
    private String fileType;
    private TourProcessingDraftStatus status;
    private String importedBy;
    private LocalDateTime importedAt;
    private int totalRows;
    private int duplicateRows;
    private int conflictingRows;
    private int oversizedRows;
    private int blocksDetected;
    private Integer year;
    private Integer month;
    private Integer businessDays;
    private LocalDate minDepartureDate;
    private LocalDate maxReturnDate;
    private LocalDateTime registeredAt;
}
