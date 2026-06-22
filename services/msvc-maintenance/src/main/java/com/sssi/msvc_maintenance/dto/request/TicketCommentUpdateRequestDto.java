package com.sssi.msvc_maintenance.dto.request;

import com.sssi.common.utils.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Size(max = 1000, message = "El comentario no puede exceder 1000 caracteres")
    @Pattern(
            regexp = ValidationUtils.COMMENT_TEXT_REGEX,
            message = "El comentario contiene caracteres inválidos"
    )
    private String content;
}
