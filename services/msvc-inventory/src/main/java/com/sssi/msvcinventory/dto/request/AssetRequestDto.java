package com.sssi.msvcinventory.dto.request;

import com.sssi.msvcinventory.entity.enums.AssetStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
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

    @NotBlank(message = "El número de activo es obligatorio")
    @Size(max = 100, message = "El número de activo no puede superar los 100 caracteres")
    private String assetNumber;

    @NotBlank(message = "El número de serie es obligatorio")
    @Size(max = 100, message = "El número de serie no puede superar los 100 caracteres")
    private String serialNumber;

    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    private BigDecimal longitude;
}
