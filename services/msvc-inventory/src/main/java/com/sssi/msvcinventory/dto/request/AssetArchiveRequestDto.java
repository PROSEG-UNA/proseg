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
public class AssetArchiveRequestDto {

    @NotNull(message = "El id del activo es obligatorio")
    private UUID assetId;

    @NotBlank(message = "El objectName del archivo es obligatorio")
    @Size(max = 500, message = "El objectName no puede superar los 500 caracteres")
    private String objectName;

    @Size(max = 255, message = "El caption no puede superar los 255 caracteres")
    private String caption;
}
