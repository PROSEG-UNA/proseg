package com.sssi.msvc_maintenance.dto.request;

import com.sssi.common.utils.ValidationUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateManagedUserRequestDto {

    @NotBlank(message = "El username no puede estar vacío")
    @Size(min = 3, max = 64, message = "El username debe tener entre 3 y 64 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "El username solo puede contener letras, números, guiones y guiones bajos")
    private String username;

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El email debe ser válido")
    @Pattern(regexp = ValidationUtils.EMAIL_REGEX, message = "El email contiene caracteres inválidos")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    private String email;

    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(min = 2, max = 24, message = "El nombre debe tener entre 2 y 24 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido no puede estar vacío")
    @Size(min = 2, max = 24, message = "El apellido debe tener entre 2 y 24 caracteres")
    private String lastName;
}

