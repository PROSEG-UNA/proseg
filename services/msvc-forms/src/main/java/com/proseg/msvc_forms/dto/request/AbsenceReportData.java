package com.proseg.msvc_forms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenceReportData {

    @JsonProperty("fecha")
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate date;

    @JsonProperty("hora")
    @NotNull(message = "La hora es obligatoria")
    private LocalTime time;

    @JsonProperty("guarda")
    @NotBlank(message = "El guarda es obligatorio")
    private String guard;

    @JsonProperty("turno")
    @NotBlank(message = "El turno es obligatorio")
    private String shift;

    @JsonProperty("horaTurno")
    @NotBlank(message = "La hora del turno es obligatoria")
    private String shiftTime;

    @JsonProperty("puestoTrabajo")
    @NotBlank(message = "El puesto de trabajo es obligatorio")
    private String workPosition;

    @JsonProperty("motivoAusencia")
    @NotBlank(message = "El motivo de ausencia es obligatorio")
    private String absenceReason;

    @JsonProperty("supervisor")
    @NotBlank(message = "El supervisor es obligatorio")
    private String supervisor;
}
