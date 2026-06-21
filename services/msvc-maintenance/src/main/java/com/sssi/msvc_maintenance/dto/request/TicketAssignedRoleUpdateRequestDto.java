package com.sssi.msvc_maintenance.dto.request;

import com.sssi.common.utils.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAssignedRoleUpdateRequestDto {
    @NotBlank
    @Pattern(
            regexp = ValidationUtils.SAFE_TEXT_REGEX,
            message = "El rol asignado contiene caracteres inválidos"
    )
    private String assignedRole;
}
