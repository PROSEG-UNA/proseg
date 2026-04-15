    package com.sssi.msvc_auth.dto;

    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.Size;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class LoginRequestDto {

        @NotBlank(message = "El usuario o correo no puede estar vacío")
        @Size(min = 3, max = 100, message = "Debe tener entre 3 y 100 caracteres")
        private String identifier;

        @NotBlank(message = "La contraseña no puede estar vacía")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        private String password;
    }
