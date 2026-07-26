package com.sssi.msvc_transport.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverRequestDto {

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Size(max = 50)
    private String documentId;

    @NotBlank
    @Size(max = 50)
    private String licenseNumber;

    @Size(max = 50)
    private String phone;

    @Email
    @Size(max = 150)
    private String email;

    @Builder.Default
    private String status = "ACTIVE";

    private String restrictions;

    @Builder.Default
    private boolean availability = true;

    @PositiveOrZero
    @Builder.Default
    private double accumulatedHours = 0D;

    @PositiveOrZero
    @Builder.Default
    private double freeWeekendsCount = 1D;

    @PositiveOrZero
    @Builder.Default
    private double overtimeHours = 0D;

    @PositiveOrZero
    @Builder.Default
    private double surplusHours = 0D;

    @PositiveOrZero
    @Builder.Default
    private double jornadaHoursPerDay = 8D;
}
