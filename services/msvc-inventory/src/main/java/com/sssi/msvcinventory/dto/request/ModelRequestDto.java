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
public class AssetModelRequestDto {

    @NotBlank(message = "El nombre del modelo es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @NotNull(message = "La marca es obligatoria")
    private UUID brandId;

    @NotNull(message = "El tipo de activo es obligatorio")
    private UUID assetTypeId;
}
