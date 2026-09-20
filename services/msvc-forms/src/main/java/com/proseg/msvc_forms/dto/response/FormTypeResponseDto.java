package com.proseg.msvc_forms.dto.response;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormTypeResponseDto {

    private UUID id;

    private String code;

    private String name;

    private String description;

    private boolean active;
}
