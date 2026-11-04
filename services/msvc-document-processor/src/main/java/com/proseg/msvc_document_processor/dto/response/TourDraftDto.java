package com.proseg.msvc_document_processor.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourDraftDto {
    private UUID id;

    private String fileName;

    private Integer year;

    private Integer month;
}
