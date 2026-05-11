package com.sssi.msvc_auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequestDto {
    @NotBlank(message = "El nombre del rol no puede estar vacío")
    @Size(min = 3, max = 24, message = "El nombre del rol debe tener entre 3 y 24 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9 _-]+$", message = "El nombre del rol solo puede contener letras, números, espacios, guiones y guiones bajos")
    private String roleName;

            @Size(max = 150, message = "La descripción debe tener máximo 150 caracteres")
        @Pattern(
            regexp = "^[a-zA-Z0-9\\s\\-_.,;:'()\"áéíóúñÁÉÍÓÚÑ]*$",
            message = "La descripción solo puede contener letras, números, espacios y puntuación básica"
        )
    private String description;

    @NotEmpty(message = "Debe seleccionar al menos un privilegio")
    private List<String> privileges;
}