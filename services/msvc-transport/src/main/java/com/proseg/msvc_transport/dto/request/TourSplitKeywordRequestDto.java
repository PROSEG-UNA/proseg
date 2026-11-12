package com.proseg.msvc_transport.dto.request;

import com.proseg.msvc_transport.dto.tourprocessing.TourSplitMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class TourSplitKeywordRequestDto {

    @NotBlank
    @Size(max = 100)
    private String text;

    @NotNull
    private TourSplitMode mode;
}
