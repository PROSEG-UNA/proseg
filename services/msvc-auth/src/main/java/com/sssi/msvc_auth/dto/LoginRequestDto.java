    package com.sssi.msvc_auth.dto;

    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.Pattern;
    import jakarta.validation.constraints.Size;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class LoginRequestDto {

        @NotBlank(message = "El usuario o correo no puede estar vacío")
        @Size(min = 3, max = 64, message = "Debe tener entre 3 y 64 caracteres")
            @Pattern(
                regexp = "^(?:[a-zA-Z0-9_-]{3,64}|[^\\s@]+@[^\\s@]+\\.[^\\s@]+)$",
                message = "Debe ser un usuario de 3-64 caracteres o un email válido"
            )
        private String identifier;

        @NotBlank(message = "La contraseña no puede estar vacía")
            @Size(min = 8, max = 24, message = "La contraseña debe tener entre 8 y 24 caracteres")
            @Pattern(
                regexp = "^[a-zA-Z0-9!@#$%^&*()_+=;:'\",.<>/?|~`-]{8,24}$",
                message = "La contraseña solo puede usar caracteres permitidos"
            )
        private String password;
    }
