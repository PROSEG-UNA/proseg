package com.proseg.msvc_transport.dto.cleaning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class CleaningDraftCreateRequestDto {

    @NotBlank(message = "El nombre del archivo es obligatorio")
    @Size(max = 255, message = "El nombre del archivo no puede superar los 255 caracteres")
    private String fileName;

    @NotBlank(message = "El formato del archivo es obligatorio")
    @Size(max = 10, message = "El formato del archivo no puede superar los 10 caracteres")
    private String fileType;

    private Integer totalRowsRead;

    private Integer blocksDetected;

    private List<CleaningDraftRowRequestDto> rows;
}
