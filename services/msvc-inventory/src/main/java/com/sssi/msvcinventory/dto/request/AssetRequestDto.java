package com.sssi.msvcinventory.dto.request;

import com.sssi.msvcinventory.entity.enums.AssetStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AssetRequestDto {

    @NotBlank(message = "El nombre del activo es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres")
    private String name;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String description;

    @Size(max = 100, message = "El subtipo no puede superar los 100 caracteres")
    private String subtype;

    @NotNull(message = "El modelo del activo es obligatorio")
    private UUID modelId;

    @NotNull(message = "La ubicación es obligatoria")
    private UUID locationId;

    @NotNull(message = "El estado es obligatorio")
    private AssetStatus status;

    @Size(max = 255, message = "La descripción del estado no puede superar los 255 caracteres")
    private String statusDescription;

    private LocalDate acquisitionDate;

    private LocalDate warrantyEndDate;

    private LocalDate firmwareSupportEndDate;

    @Valid
    private NetworkInterfaceEmbeddedRequestDto networkInterface;
}
