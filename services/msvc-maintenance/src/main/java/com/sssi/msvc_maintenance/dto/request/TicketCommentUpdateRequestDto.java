package com.sssi.msvc_maintenance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCommentUpdateRequestDto {

    @NotBlank(message = "El comentario es requerido")
    @Size(max = 3000, message = "El comentario no puede exceder 3000 caracteres")
    private String content;
}
