package com.proseg.msvc_transport.dto.tourprocessing;

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
public class TourProcessingObservationReviewedRequestDto {

    @NotNull(message = "Indica si la observación queda revisada")
    private Boolean reviewed;
}
