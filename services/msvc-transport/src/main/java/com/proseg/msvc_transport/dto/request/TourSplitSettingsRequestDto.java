package com.proseg.msvc_transport.dto.request;

import com.proseg.msvc_transport.dto.tourprocessing.TourSplitMode;
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
public class TourSplitSettingsRequestDto {

    @NotNull
    private TourSplitMode defaultMode;
}
