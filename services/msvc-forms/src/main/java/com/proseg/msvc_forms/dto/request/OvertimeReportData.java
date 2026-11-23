package com.proseg.msvc_forms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeReportData {

    @JsonProperty("fecha")
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate date;

    @JsonProperty("grupo")
    @NotBlank(message = "El grupo es obligatorio")
    private String group;

    @JsonProperty("supervisor")
    @NotBlank(message = "El supervisor es obligatorio")
    private String supervisor;

    @JsonProperty("detalle")
    @Valid
    @NotNull(message = "El detalle de funcionarios es obligatorio")
    private List<OvertimeReportDetailRow> details;

    @JsonProperty("observacion")
    private String observation;
}
