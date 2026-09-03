package com.proseg.msvcinventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampusRequestDto {

    @NotBlank(message = "El nombre del campus es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;
}
