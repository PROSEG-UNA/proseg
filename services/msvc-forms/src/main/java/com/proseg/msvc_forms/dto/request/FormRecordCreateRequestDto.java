package com.proseg.msvc_forms.dto.request;

import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormRecordCreateRequestDto {

    @NotNull(message = "El tipo de formulario es obligatorio")
    private UUID formTypeId;

    @NotNull(message = "Los datos del formulario son obligatorios")
    private JsonNode data;
}
