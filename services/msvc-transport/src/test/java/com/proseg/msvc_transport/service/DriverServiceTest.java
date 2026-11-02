package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.DriverRequestDto;
import com.proseg.msvc_transport.dto.response.DriverResponseDto;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.DriverUnit;
import com.proseg.msvc_transport.entity.UlloaShift;
import com.proseg.msvc_transport.entity.VehicleType;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DriverServiceTest {

    private DriverRepository driverRepository;
    private DriverService service;

    @BeforeEach
    void setUp() {
        driverRepository = mock(DriverRepository.class);
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new DriverService(driverRepository, mock(DraftCatalogService.class));
    }

    @Test
    void createsDriverWithTrimmedNameAndDefaultStatus() {
        DriverRequestDto request = DriverRequestDto.builder().name("  Daniel Zuniga  ").build();

        DriverResponseDto response = service.create(request);

        assertThat(response.getName()).isEqualTo("Daniel Zuniga");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getOrganizationalUnit()).isEqualTo(DriverUnit.GENERAL_SERVICES);
    }

    @Test
    void updatesDriverUnitAndPreservesItWhenAnOlderClientOmitsTheField() {
        UUID id = UUID.randomUUID();
        Driver driver = Driver.builder().id(id).name("Daniel Zuniga").build();
        when(driverRepository.findById(id)).thenReturn(java.util.Optional.of(driver));

        DriverResponseDto updated = service.update(id, DriverRequestDto.builder()
                .name("Daniel Zuniga")
                .organizationalUnit(DriverUnit.OTHER_UNIT)
                .build());
        DriverResponseDto preserved = service.update(id, DriverRequestDto.builder()
                .name("Daniel Zuniga")
                .build());

        assertThat(updated.getOrganizationalUnit()).isEqualTo(DriverUnit.OTHER_UNIT);
        assertThat(preserved.getOrganizationalUnit()).isEqualTo(DriverUnit.OTHER_UNIT);
    }

    @Test
    void createsDriverAbleToDriveEveryVehicleTypeByDefault() {
        DriverResponseDto response = service.create(DriverRequestDto.builder().name("Daniel Zuniga").build());

        assertThat(response.getAllowedVehicleTypes()).containsExactly(VehicleType.values());
    }

    @Test
    void createsDriverWithOnlyTheRequestedVehicleTypes() {
        DriverRequestDto request = DriverRequestDto.builder()
                .name("Daniel Zuniga")
                .allowedVehicleTypes(Set.of(VehicleType.PICK_UP, VehicleType.SEDAN))
                .build();

        DriverResponseDto response = service.create(request);

        assertThat(response.getAllowedVehicleTypes()).containsExactly(VehicleType.SEDAN, VehicleType.PICK_UP);
    }

    @Test
    void updatesAllowedVehicleTypesOfExistingDriver() {
        UUID id = UUID.randomUUID();
        Driver driver = Driver.builder().id(id).name("Daniel Zuniga").build();
        when(driverRepository.findById(id)).thenReturn(java.util.Optional.of(driver));
        DriverRequestDto request = DriverRequestDto.builder()
                .name("Daniel Zuniga")
                .allowedVehicleTypes(Set.of(VehicleType.MICROBUS))
                .build();

        DriverResponseDto response = service.update(id, request);

        assertThat(response.getAllowedVehicleTypes()).containsExactly(VehicleType.MICROBUS);
        assertThat(driver.getAllowedVehicleTypes()).containsExactly(VehicleType.MICROBUS);
    }

    @Test
    void rejectsCreateWhenNameAlreadyExists() {
        when(driverRepository.existsByNameIgnoreCase("Daniel Zuniga")).thenReturn(true);

        assertThatThrownBy(() -> service.create(DriverRequestDto.builder().name("Daniel Zuniga").build()))
                .isInstanceOf(TransportException.class)
                .hasMessage("Ya existe un chofer con ese nombre");
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void rejectsUpdateWhenAnotherDriverHasTheName() {
        UUID id = UUID.randomUUID();
        when(driverRepository.findById(id)).thenReturn(java.util.Optional.of(new Driver()));
        when(driverRepository.existsByNameIgnoreCaseAndIdNot("Daniel Zuniga", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, DriverRequestDto.builder().name("Daniel Zuniga").build()))
                .isInstanceOf(TransportException.class)
                .hasMessage("Ya existe un chofer con ese nombre");
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void createsDriverWithTheStandardOrdinaryScheduleAndNoUlloaShiftsByDefault() {
        DriverResponseDto response = service.create(DriverRequestDto.builder().name("Daniel Zuniga").build());

        assertThat(response.getOrdinaryStart()).isEqualTo(LocalTime.of(7, 30));
        assertThat(response.getOrdinaryEnd()).isEqualTo(LocalTime.of(16, 30));
        assertThat(response.getUlloaShifts()).isEmpty();
    }

    @Test
    void createsDriverWithItsOwnOrdinaryScheduleAndUlloaShifts() {
        DriverRequestDto request = DriverRequestDto.builder()
                .name("Ricardo Jovane")
                .ordinaryStart(LocalTime.of(11, 0))
                .ordinaryEnd(LocalTime.of(20, 0))
                .ulloaShifts(Set.of(UlloaShift.AFTERNOON, UlloaShift.SHORT_MORNING))
                .build();

        DriverResponseDto response = service.create(request);

        assertThat(response.getOrdinaryStart()).isEqualTo(LocalTime.of(11, 0));
        assertThat(response.getOrdinaryEnd()).isEqualTo(LocalTime.of(20, 0));
        assertThat(response.getUlloaShifts()).containsExactly(UlloaShift.SHORT_MORNING, UlloaShift.AFTERNOON);
    }

    @Test
    void rejectsAnOrdinaryScheduleThatEndsBeforeItStarts() {
        DriverRequestDto request = DriverRequestDto.builder()
                .name("Ricardo Jovane")
                .ordinaryStart(LocalTime.of(16, 0))
                .ordinaryEnd(LocalTime.of(8, 0))
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(TransportException.class)
                .hasMessage("El horario ordinario debe terminar después de la hora de inicio");
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void rejectsAnOrdinaryScheduleLongerThanNineHours() {
        DriverRequestDto request = DriverRequestDto.builder()
                .name("Ricardo Jovane")
                .ordinaryStart(LocalTime.of(7, 0))
                .ordinaryEnd(LocalTime.of(17, 0))
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(TransportException.class)
                .hasMessage("El horario ordinario no puede superar las 9 horas por día");
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void updatesOrdinaryScheduleAndUlloaShiftsOfExistingDriver() {
        UUID id = UUID.randomUUID();
        Driver driver = Driver.builder().id(id).name("Daniel Zuniga").build();
        when(driverRepository.findById(id)).thenReturn(java.util.Optional.of(driver));
        DriverRequestDto request = DriverRequestDto.builder()
                .name("Daniel Zuniga")
                .ordinaryStart(LocalTime.of(11, 0))
                .ordinaryEnd(LocalTime.of(20, 0))
                .ulloaShifts(Set.of(UlloaShift.AFTERNOON))
                .build();

        service.update(id, request);

        assertThat(driver.getOrdinaryStart()).isEqualTo(LocalTime.of(11, 0));
        assertThat(driver.getOrdinaryEnd()).isEqualTo(LocalTime.of(20, 0));
        assertThat(driver.getUlloaShifts()).containsExactly(UlloaShift.AFTERNOON);
    }
}
