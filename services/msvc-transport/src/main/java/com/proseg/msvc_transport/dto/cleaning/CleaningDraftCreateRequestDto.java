package com.proseg.msvc_transport.dto.cleaning;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "El año es obligatorio")
    @Min(value = 2000, message = "El año no es válido")
    @Max(value = 2100, message = "El año no es válido")
    private Integer year;

    @NotNull(message = "El mes es obligatorio")
    @Min(value = 1, message = "El mes debe estar entre 1 y 12")
    @Max(value = 12, message = "El mes debe estar entre 1 y 12")
    private Integer month;

    @NotNull(message = "Los días hábiles son obligatorios")
    @Min(value = 1, message = "Los días hábiles deben estar entre 1 y 31")
    @Max(value = 31, message = "Los días hábiles deben estar entre 1 y 31")
    private Integer businessDays;

    private List<CleaningDraftRowRequestDto> rows;
}
