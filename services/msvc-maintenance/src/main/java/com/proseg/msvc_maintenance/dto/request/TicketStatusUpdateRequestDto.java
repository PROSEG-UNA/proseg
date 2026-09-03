package com.proseg.msvc_maintenance.dto.request;

import com.proseg.msvc_maintenance.entity.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketStatusUpdateRequestDto {

    @NotNull(message = "El estado es requerido")
    private TicketStatus status;
}
