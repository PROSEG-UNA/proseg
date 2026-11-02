package com.proseg.msvc_transport.dto.tourprocessing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalCommissionCheckDto {
    private List<Issue> blockers;
    private List<Issue> errors;
    private List<Issue> warnings;
    private Issue rejections;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Issue {
        private String code;
        private String title;
        private List<String> details;
    }
}
