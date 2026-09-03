package com.proseg.msvcinventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetComponentRequestDto {

    @NotBlank(message = "El nombre del componente es obligatorio")
    @Size(max = 255, message = "El nombre del componente no puede superar los 255 caracteres")
    private String name;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1")
    private Integer quantity;

    @Size(max = 255, message = "La ubicación no puede superar los 255 caracteres")
    private String location;

    @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres")
    private String observations;
}

