package com.proseg.msvc_transport.dto.tourprocessing;

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
public class TourProcessingDraftRowIssueDto {
    private Integer row;
    private String reason;
}
