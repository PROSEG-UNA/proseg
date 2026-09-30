package com.proseg.msvc_transport.dto.cleaning;

import com.proseg.msvc_transport.entity.CleaningDraftStatus;
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
public class CleaningDraftResponseDto {
    private UUID id;
    private String fileName;
    private String fileType;
    private CleaningDraftStatus status;
    private String importedBy;
    private LocalDateTime importedAt;
    private int totalRows;
    private int duplicateRows;
    private int conflictingRows;
    private int blocksDetected;
    private LocalDate minDepartureDate;
    private LocalDate maxReturnDate;
    private LocalDateTime registeredAt;
    private UUID cleaningExecutionId;
}
