package com.proseg.msvc_forms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeReportDetailRow {

    @JsonProperty("no")
    @NotNull(message = "El número de fila es obligatorio")
    private Integer number;

    @JsonProperty("nombre")
    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @JsonProperty("cedula")
    @NotBlank(message = "La cédula es obligatoria")
    private String cedula;

    @JsonProperty("fecha")
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate date;

    @JsonProperty("horaEntrada")
    @NotNull(message = "La hora de entrada es obligatoria")
    private LocalTime entryTime;

    @JsonProperty("horaSalida")
    @NotNull(message = "La hora de salida es obligatoria")
    private LocalTime exitTime;

    @JsonProperty("totalHoras")
    @Min(value = 0, message = "El total de horas debe ser mayor o igual a 0")
    private Double totalHours;
}
