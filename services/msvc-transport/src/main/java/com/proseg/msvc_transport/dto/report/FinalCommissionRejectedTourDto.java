package com.proseg.msvc_transport.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalCommissionRejectedTourDto {
    private UUID tourId;
    private String tourName;
    private String reason;
}
