package com.proseg.msvc_transport.dto.cleaning;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningDraftRowRequestDto {

    @NotNull(message = "La fila no indica su número en el archivo")
    private Integer rowNumber;

    @Size(max = 200, message = "El chofer no puede superar los 200 caracteres")
    private String driver;

    @NotBlank(message = "El campo obligatorio 'Numero' está vacío")
    @Size(max = 50, message = "El número no puede superar los 50 caracteres")
    private String number;

    @Size(max = 100, message = "El vehículo no puede superar los 100 caracteres")
    private String vehicle;

    @NotNull(message = "El campo obligatorio 'Pasajeros' está vacío")
    @Min(value = 1, message = "Pasajeros debe ser mayor o igual a 1")
    private Integer passengers;

    @NotBlank(message = "El campo obligatorio 'Unidad Ejecutora' está vacío")
    @Size(max = 255, message = "La unidad ejecutora no puede superar los 255 caracteres")
    private String executingUnit;

    @NotBlank(message = "El campo obligatorio 'Responsable' está vacío")
    @Size(max = 255, message = "El responsable no puede superar los 255 caracteres")
    private String responsible;

    @NotBlank(message = "El campo obligatorio 'Destinos' está vacío")
    @Size(max = 255, message = "El destino no puede superar los 255 caracteres")
    private String destination;

    @NotNull(message = "El campo obligatorio 'Duración' está vacío")
    @Min(value = 1, message = "Duración debe ser mayor o igual a 1")
    private Integer durationDays;

    @NotNull(message = "El campo obligatorio 'Prioridad' está vacío")
    @Min(value = 1, message = "Prioridad debe ser mayor o igual a 1")
    private Integer priority;

    @Size(max = 50, message = "La modalidad no puede superar los 50 caracteres")
    private String modality;

    @NotNull(message = "El campo obligatorio 'Hora Salida' está vacío")
    private LocalTime departureTime;

    @NotNull(message = "El campo obligatorio 'Hora Regreso' está vacío")
    private LocalTime returnTime;

    @NotNull(message = "El campo obligatorio 'Fecha Salida' está vacío")
    private LocalDate departureDate;

    @NotNull(message = "El campo obligatorio 'Fecha Regreso' está vacío")
    private LocalDate returnDate;

    private String observations;
}
