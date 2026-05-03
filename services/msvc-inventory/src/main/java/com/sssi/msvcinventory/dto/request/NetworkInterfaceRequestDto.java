package com.sssi.msvcinventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkInterfaceRequestDto {

    @NotNull(message = "El id del activo es obligatorio")
    private UUID assetId;

    @NotBlank(message = "La dirección IP es obligatoria")
    @Pattern(
            regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$",
            message = "La dirección IP no tiene un formato válido"
    )
    private String ipAddress;

    @NotBlank(message = "La dirección MAC es obligatoria")
    @Size(min = 12, max = 17, message = "La dirección MAC debe tener entre 12 y 17 caracteres")
    private String macAddress;
}