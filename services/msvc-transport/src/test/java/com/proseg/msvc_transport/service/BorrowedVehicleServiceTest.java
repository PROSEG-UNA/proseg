package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.BorrowedVehicleRequestDto;
import com.proseg.msvc_transport.entity.BorrowedVehicle;
import com.proseg.msvc_transport.entity.VehicleType;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.BorrowedVehicleRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BorrowedVehicleServiceTest {

    private final UUID id = UUID.randomUUID();
    private final BorrowedVehicle vehicle = BorrowedVehicle.builder().id(id).plate("PRES-1").capacity(10).type(VehicleType.MICROBUS).build();
    private final BorrowedVehicleRequestDto request = BorrowedVehicleRequestDto.builder().plate("PRES-1").capacity(12).type(VehicleType.SEDAN).build();
    private BorrowedVehicleRepository repository;
    private TourProcessingDraftAssignmentRepository assignmentRepository;
    private BorrowedVehicleService service;

    @BeforeEach
    void setUp() {
        repository = mock(BorrowedVehicleRepository.class);
        assignmentRepository = mock(TourProcessingDraftAssignmentRepository.class);
        service = new BorrowedVehicleService(repository, assignmentRepository);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.of(vehicle));
    }

    @Test
    void reactivatesARetiredVehicleWhenItsPlateIsCreatedAgain() {
        vehicle.markAsDeleted();
        when(repository.findByPlateIgnoreCase("PRES-1")).thenReturn(Optional.of(vehicle));

        var created = service.create(request);

        assertThat(created.getId()).isEqualTo(id);
        assertThat(created.getCapacity()).isEqualTo(12);
        assertThat(vehicle.isDeleted()).isFalse();
    }

    @Test
    void rejectsADuplicatePlateOfAnActiveVehicle() {
        when(repository.findByPlateIgnoreCase("PRES-1")).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> service.create(request)).isInstanceOfSatisfying(TransportException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("BORROWED_VEHICLE_DUPLICATE_PLATE"));
    }

    @Test
    void refusesDeletingAVehicleInAnOpenDraft() {
        when(assignmentRepository.isBorrowedVehicleInOpenDraft(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOfSatisfying(TransportException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("BORROWED_VEHICLE_IN_USE"));
        verify(repository, never()).delete(any(BorrowedVehicle.class));
    }

    @Test
    void deletesAVehicleOutsideOpenDrafts() {
        service.delete(id);

        verify(repository).delete(vehicle);
    }
}
