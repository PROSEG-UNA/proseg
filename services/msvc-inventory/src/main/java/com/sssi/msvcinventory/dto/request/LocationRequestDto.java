package com.sssi.msvcinventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRequestDto {

    @NotBlank(message = "El nombre de la ubicación es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @NotNull(message = "El sitio es obligatorio")
    private UUID siteId;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String description;
}
