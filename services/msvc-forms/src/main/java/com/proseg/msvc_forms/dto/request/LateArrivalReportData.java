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
public class LateArrivalReportData {

    @JsonProperty("fecha")
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate date;

    @JsonProperty("oficialSeguridad")
    @NotBlank(message = "El oficial de seguridad es obligatorio")
    private String securityOfficer;

    @JsonProperty("operadorAcceso")
    @NotBlank(message = "El operador de acceso vehicular es obligatorio")
    private String vehicleAccessOperator;

    @JsonProperty("horaLlegada")
    @NotNull(message = "La hora de llegada tardía es obligatoria")
    private LocalTime arrivalTime;

    @JsonProperty("puesto")
    @NotBlank(message = "El puesto es obligatorio")
    private String position;

    @JsonProperty("motivo")
    @NotBlank(message = "El motivo de la llegada tardía es obligatorio")
    private String reason;

    @JsonProperty("supervisor")
    @NotBlank(message = "El supervisor/a de turno es obligatorio")
    private String supervisor;
}
