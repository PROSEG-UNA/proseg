package com.proseg.msvc_transport.dto.cleaning;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningDraftRowDayRequestDto {

    @NotNull(message = "El campo obligatorio 'Hora Inicio' está vacío")
    private LocalTime startTime;

    @NotNull(message = "El campo obligatorio 'Hora Fin' está vacío")
    private LocalTime endTime;

    @NotNull(message = "El campo obligatorio 'Horas Ordinarias' está vacío")
    @PositiveOrZero(message = "Horas ordinarias no puede ser negativo")
    private Double ordinaryHours;

    @NotNull(message = "El campo obligatorio 'Horas Sobresueldo' está vacío")
    @PositiveOrZero(message = "Horas sobresueldo no puede ser negativo")
    private Double surplusHours;

    @NotNull(message = "El campo obligatorio 'Horas Extra' está vacío")
    @PositiveOrZero(message = "Horas extra no puede ser negativo")
    private Double overtimeHours;

    private boolean confirmOvertimeBelowSurplusThreshold;
}
