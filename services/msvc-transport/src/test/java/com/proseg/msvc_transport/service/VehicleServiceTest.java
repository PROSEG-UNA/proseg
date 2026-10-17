package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.VehicleRequestDto;
import com.proseg.msvc_transport.dto.response.VehicleResponseDto;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.entity.VehicleType;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VehicleServiceTest {

    private VehicleRepository vehicleRepository;
    private VehicleService service;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new VehicleService(vehicleRepository);
    }

    @Test
    void createsVehicleWithOnlyThePlate() {
        VehicleResponseDto response = service.create(VehicleRequestDto.builder().plate(" 301-295 ").build());

        assertThat(response.getPlate()).isEqualTo("301-295");
        assertThat(response.getCapacity()).isNull();
        assertThat(response.getType()).isNull();
        assertThat(response.isAvailable()).isTrue();
    }

    @Test
    void storesTheVehicleType() {
        VehicleResponseDto response = service.create(VehicleRequestDto.builder().plate("301-295").type(VehicleType.PICK_UP).build());

        assertThat(response.getType()).isEqualTo(VehicleType.PICK_UP);
    }

    @Test
    void rejectsCreateWhenPlateAlreadyExists() {
        when(vehicleRepository.existsByPlateIgnoreCase("301-295")).thenReturn(true);

        assertThatThrownBy(() -> service.create(VehicleRequestDto.builder().plate("301-295").build()))
                .isInstanceOf(TransportException.class)
                .hasMessage("Ya existe un vehículo con esa placa");
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }
}
