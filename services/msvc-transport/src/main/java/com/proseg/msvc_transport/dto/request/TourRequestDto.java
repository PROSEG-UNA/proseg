package com.proseg.msvc_transport.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourRequestDto {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 50)
    private String externalNumber;

    @NotBlank
    @Size(max = 200)
    private String origin;

    @NotBlank
    @Size(max = 200)
    private String destination;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    @Builder.Default
    private String status = "PLANNED";

    @NotNull
    @Min(1)
    private Integer priority;

    @NotNull
    @Min(1)
    private Integer passengers;

    @Size(max = 100)
    private String requestedVehicle;

    @Size(max = 100)
    private String requestedVehicleType;

    @Size(max = 200)
    private String requestedDriver;

    @Size(max = 200)
    private String responsible;

    @Size(max = 200)
    private String executingUnit;

    @Size(max = 50)
    private String modality;

    @Min(1)
    private Integer durationDays;

    @Size(max = 20)
    private String departureTime;

    @Size(max = 20)
    private String returnTime;

    private String observations;
}
