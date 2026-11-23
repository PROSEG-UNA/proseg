package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.ExternalVehicleRequestDto;
import com.proseg.msvc_transport.entity.ExternalVehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.ExternalVehicleRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalVehicleServiceTest {

    private final UUID id = UUID.randomUUID();
    private final ExternalVehicle vehicle = ExternalVehicle.builder().id(id).provider("Anca").plate("HB4380").capacity(13).build();
    private final ExternalVehicleRequestDto request = ExternalVehicleRequestDto.builder()
            .provider("Anca Tours").plate("hb4380").capacity(15).available(false).build();
    private ExternalVehicleRepository repository;
    private TourProcessingDraftAssignmentRepository assignmentRepository;
    private ExternalVehicleService service;

    @BeforeEach
    void setUp() {
        repository = mock(ExternalVehicleRepository.class);
        assignmentRepository = mock(TourProcessingDraftAssignmentRepository.class);
        service = new ExternalVehicleService(repository, assignmentRepository);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.of(vehicle));
    }

    @Test
    void reactivatesARetiredVehicleWhenItsPlateIsCreatedAgain() {
        vehicle.markAsDeleted();
        when(repository.findByPlateIgnoreCase("hb4380")).thenReturn(Optional.of(vehicle));

        var created = service.create(request);

        assertThat(created.getId()).isEqualTo(id);
        assertThat(created.getProvider()).isEqualTo("Anca Tours");
        assertThat(created.getCapacity()).isEqualTo(15);
        assertThat(created.isAvailable()).isFalse();
        assertThat(vehicle.isDeleted()).isFalse();
    }

    @Test
    void rejectsADuplicatePlateOfAnActiveVehicle() {
        when(repository.findByPlateIgnoreCase("hb4380")).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> service.create(request)).isInstanceOfSatisfying(TransportException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("EXTERNAL_VEHICLE_DUPLICATE_PLATE"));
    }

    @Test
    void rejectsChangingThePlateToOneUsedByAnotherVehicle() {
        when(repository.existsByPlateIgnoreCaseAndIdNot("hb4380", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, request)).isInstanceOfSatisfying(TransportException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("EXTERNAL_VEHICLE_DUPLICATE_PLATE"));
        verify(assignmentRepository, never()).renameExternalVehicle(any(), any(), any());
    }

    @Test
    void carriesTheNewProviderAndPlateToTheAssignmentsOfOpenDrafts() {
        service.update(id, request);

        verify(assignmentRepository).renameExternalVehicle("HB4380", "hb4380", "Anca Tours");
    }

    @Test
    void ordersByProviderThenPlateWhenNoColumnIsSorted() {
        assertThat(ExternalVehicleService.stableSort(Sort.unsorted()))
                .containsExactly(Sort.Order.asc("provider"), Sort.Order.asc("plate"));
    }

    @Test
    void breaksTiesByPlateWhenSortingByAnotherColumn() {
        assertThat(ExternalVehicleService.stableSort(Sort.by(Sort.Order.desc("capacity"))))
                .containsExactly(Sort.Order.desc("capacity"), Sort.Order.asc("plate"));
    }

    @Test
    void keepsTheRequestedDirectionWhenSortingByPlate() {
        assertThat(ExternalVehicleService.stableSort(Sort.by(Sort.Order.desc("plate"))))
                .containsExactly(Sort.Order.desc("plate"));
    }

    @Test
    void refusesDeletingAVehicleInAnOpenDraft() {
        when(assignmentRepository.isExternalVehicleInOpenDraft("HB4380")).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOfSatisfying(TransportException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("EXTERNAL_VEHICLE_IN_USE"));
        verify(repository, never()).delete(any(ExternalVehicle.class));
    }

    @Test
    void deletesAVehicleOutsideOpenDrafts() {
        service.delete(id);

        verify(repository).delete(vehicle);
    }
}
