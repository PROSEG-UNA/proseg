package com.proseg.msvc_maintenance.dto.request;

import com.proseg.common.utils.ValidationUtils;
import com.proseg.msvc_maintenance.entity.enums.TicketPriority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCreateRequestDto {

    @NotBlank(message = "El título es requerido")
    @Size(max = 120, message = "El título no puede superar los 120 caracteres")
    @Pattern(
        regexp = ValidationUtils.SAFE_TEXT_REGEX,
        message = "El título contiene caracteres inválidos"
    )
    private String title;

    @NotBlank(message = "La descripción es requerida")
    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    @Pattern(
        regexp = ValidationUtils.SAFE_TEXT_REGEX,
        message = "La descripción contiene caracteres inválidos"
    )
    private String description;

    private TicketPriority priority;

    private UUID siteId;

    private UUID buildingId;

    private UUID floorId;
    private UUID locationId;
    private List<UUID> assetIds;
    private List<UUID> removedPhotoIds;

    @AssertTrue(message = "La sede es requerida cuando el ticket no tiene activos asociados")
    public boolean isSiteProvidedWhenThereAreNoAssets() {
        return siteId != null || (assetIds != null && !assetIds.isEmpty());
    }
}