package com.proseg.msvcinventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AlarmSensorRequestDto extends AssetRequestDto {

    @NotBlank(message = "El panel de alarma es obligatorio")
    @Size(max = 100, message = "El panel no puede superar los 100 caracteres")
    private String panel;

    @NotBlank(message = "El tipo de sensor es obligatorio")
    @Size(max = 100, message = "El tipo de sensor no puede superar los 100 caracteres")
    private String sensorType;

    @Size(max = 150, message = "La salida enlazada no puede superar los 150 caracteres")
    private String linkedOutput;
}
