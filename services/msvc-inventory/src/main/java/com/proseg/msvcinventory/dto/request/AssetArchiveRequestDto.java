package com.proseg.msvcinventory.dto.request;

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
public class AssetArchiveRequestDto {

    @NotNull(message = "El id del activo es obligatorio")
    private UUID assetId;

    @NotBlank(message = "El nombre del archivo es obligatorio")
    @Size(max = 500, message = "El nombre del archivo no puede superar los 500 caracteres")
    private String objectName;

    @Size(max = 255, message = "El título no puede superar los 255 caracteres")
    private String caption;
}
