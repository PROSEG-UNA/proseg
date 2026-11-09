package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.VehicleRequestDto;
import com.proseg.msvc_transport.dto.response.VehicleOpenDraftTourResponseDto;
import com.proseg.msvc_transport.dto.response.VehicleResponseDto;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.entity.VehicleType;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.VehicleRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VehicleServiceTest {

    private VehicleRepository vehicleRepository;
    private DraftCatalogService draftCatalogService;
    private VehicleOpenDraftTourFinder openDraftTourFinder;
    private VehicleService service;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        draftCatalogService = mock(DraftCatalogService.class);
        openDraftTourFinder = mock(VehicleOpenDraftTourFinder.class);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new VehicleService(vehicleRepository, draftCatalogService, openDraftTourFinder);
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

    @Test
    void filtersByAvailableFlag() {
        @SuppressWarnings("unchecked")
        Root<Vehicle> root = mock(Root.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        @SuppressWarnings("unchecked")
        Path<Object> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get("available")).thenReturn(path);
        when(criteriaBuilder.equal(path, false)).thenReturn(predicate);

        assertThat(VehicleService.availableFilter("false").toPredicate(root, null, criteriaBuilder)).isSameAs(predicate);
        assertThat(VehicleService.availableFilter(" ").toPredicate(root, null, criteriaBuilder)).isNull();
    }

    @Test
    void findAllRemovesAvailableFromColumnFilters() {
        when(vehicleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        service.findAll("301", Map.of("available", "true", "type", "SEDAN"), PageRequest.of(0, 10));

        verify(vehicleRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void keepsDraftAssignmentsWhenMarkedUnavailableAndReportsThem() {
        UUID id = UUID.randomUUID();
        Vehicle entity = Vehicle.builder().id(id).plate("301-295").available(true).build();
        when(vehicleRepository.findById(id)).thenReturn(Optional.of(entity));
        VehicleOpenDraftTourResponseDto tour = VehicleOpenDraftTourResponseDto.builder().number("05100").year(2026).month(5).build();
        when(openDraftTourFinder.find(id)).thenReturn(List.of(tour));

        VehicleResponseDto response = service.update(id, VehicleRequestDto.builder().plate("301-295").available(false).build());

        assertThat(response.isAvailable()).isFalse();
        assertThat(response.getOpenDraftTours()).containsExactly(tour);
        verify(draftCatalogService).syncVehicle(entity);
        verify(draftCatalogService, never()).removeVehicle(any());
    }

    @Test
    void doesNotLookUpDraftsWhenTheVehicleStaysAvailable() {
        UUID id = UUID.randomUUID();
        Vehicle entity = Vehicle.builder().id(id).plate("301-295").available(true).build();
        when(vehicleRepository.findById(id)).thenReturn(Optional.of(entity));

        VehicleResponseDto response = service.update(id, VehicleRequestDto.builder().plate("301-295").available(true).build());

        assertThat(response.getOpenDraftTours()).isEmpty();
        verify(openDraftTourFinder, never()).find(any());
        verify(draftCatalogService, never()).removeVehicle(any());
    }
}
