package com.sssi.msvcinventory.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkInterfaceEmbeddedRequestDto {

    @Pattern(
            regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$",
            message = "La dirección IP no tiene un formato válido"
    )
    private String ipAddress;

    @Size(min = 12, max = 17, message = "La dirección MAC debe tener entre 12 y 17 caracteres")
    private String macAddress;
}