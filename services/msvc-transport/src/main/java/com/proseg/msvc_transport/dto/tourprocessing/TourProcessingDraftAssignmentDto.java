package com.proseg.msvc_transport.dto.tourprocessing;

import com.proseg.msvc_transport.entity.ResourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class TourProcessingDraftAssignmentDto {
    private UUID rowId;
    private UUID driverId;
    private UUID vehicleId;
    @Size(max = 200, message = "El chofer no puede superar los 200 caracteres")
    private String driver;
    @Size(max = 100, message = "El vehículo no puede superar los 100 caracteres")
    private String vehicle;
    private ResourceType resourceType;
    private UUID borrowedVehicleId;
    private String borrowedVehiclePlate;
    @NotNull(message = "Indica los pasajeros de cada vehículo")
    @Min(value = 1, message = "Cada vehículo debe tener al menos un pasajero")
    private Integer passengers;
    private List<TourProcessingDraftRowDayResponseDto> days;
}
