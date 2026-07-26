package com.sssi.msvc_auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileImageRequestDto {

    @NotBlank(message = "El nombre del objeto es obligatorio")
    @Size(max = 500, message = "El nombre del objeto no puede superar los 500 caracteres")
    private String objectName;
}
