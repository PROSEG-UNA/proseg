package com.sssi.msvc_auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequestDto {
    @NotBlank(message = "El nombre del rol no puede estar vacío")
    private String roleName;

    @NotEmpty(message = "Debe seleccionar al menos un privilegio")
    private List<String> privileges;
}