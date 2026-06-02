package com.sssi.msvc_maintenance.dto.request;

import jakarta.validation.constraints.NotBlank;
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
    private String assignedRole;
}
