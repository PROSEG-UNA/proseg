package com.sssi.msvc_transport.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentRequestDto {

    private UUID driverId;
    private UUID vehicleId;

    @NotNull
    private UUID tourId;

    @Builder.Default
    private String status = "PENDING";

    private String notes;

    @Builder.Default
    private boolean isContracted = false;

    private String rejectionReason;
}
