package com.proseg.msvc_transport.dto.response;

import com.proseg.msvc_transport.dto.tourprocessing.TourSplitMode;
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
public class TourSplitSettingsResponseDto {

    private TourSplitMode defaultMode;
}
