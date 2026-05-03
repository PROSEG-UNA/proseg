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
public class AssetImageRequestDto {

    @NotNull(message = "El id del activo es obligatorio")
    private UUID assetId;

    @NotBlank(message = "La URL de la imagen es obligatoria")
    @Size(max = 500, message = "La URL no puede superar los 500 caracteres")
    private String url;

    @Size(max = 255, message = "El caption no puede superar los 255 caracteres")
    private String caption;
}
