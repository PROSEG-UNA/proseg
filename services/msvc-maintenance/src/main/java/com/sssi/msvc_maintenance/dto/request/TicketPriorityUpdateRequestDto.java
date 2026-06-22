package com.sssi.msvc_maintenance.dto.request;

import com.sssi.msvc_maintenance.entity.enums.TicketPriority;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketPriorityUpdateRequestDto {
    @NotNull
    private TicketPriority priority;
}
