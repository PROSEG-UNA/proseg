package com.proseg.msvc_transport.dto.tourprocessing;

import com.proseg.msvc_transport.entity.TourApprovalStatus;
import jakarta.validation.constraints.NotNull;
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
public class TourProcessingDraftRowApprovalRequestDto {

    @NotNull(message = "El estado de la gira es obligatorio")
    private TourApprovalStatus approvalStatus;
}
