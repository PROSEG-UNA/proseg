package com.proseg.msvc_transport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleOpenDraftTourResponseDto {
    private UUID draftId;
    private Integer year;
    private Integer month;
    private String fileName;
    private String number;
    private LocalDate departureDate;
}
