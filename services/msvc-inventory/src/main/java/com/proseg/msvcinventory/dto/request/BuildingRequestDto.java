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
public class BuildingRequestDto {

    @NotBlank(message = "El nombre del edificio es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @NotNull(message = "El campus es obligatorio")
    private UUID campusId;
}
