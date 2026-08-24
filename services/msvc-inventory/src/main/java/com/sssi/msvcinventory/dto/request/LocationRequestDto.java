package com.sssi.msvcinventory.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRequestDto {

    @NotNull(message = "El campus es obligatorio")
    private UUID campusId;

    @NotNull(message = "El edificio es obligatorio")
    private UUID buildingId;

    @NotNull(message = "El número de piso es obligatorio")
    @Min(value = 0, message = "El número de piso debe ser 0 o mayor")
    private Integer floorNumber;

    @NotBlank(message = "El detalle de ubicación es obligatorio")
    @Size(min = 1, max = 255, message = "El detalle de ubicación debe tener entre 2 y 255 caracteres")
    private String description;
}
