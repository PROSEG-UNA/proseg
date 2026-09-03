package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.TourRequestDto;
import com.proseg.msvc_transport.dto.response.TourResponseDto;
import com.proseg.msvc_transport.entity.Tour;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.AssignmentRepository;
import com.proseg.msvc_transport.repository.TourRepository;
import com.proseg.msvc_transport.specification.GenericSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourService {

    private final TourRepository repository;
    private final AssignmentRepository assignmentRepository;

    @Transactional
    public TourResponseDto create(TourRequestDto request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw TransportException.badRequest("TOUR_INVALID_RANGE", "La fecha fin no puede ser menor a la fecha inicio");
        }
        Tour entity = toEntity(request);
        return toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public TourResponseDto findById(UUID id) {
        return repository.findById(id).map(this::toResponse).orElseThrow(() -> TransportException.notFound("Gira", id.toString()));
    }

    @Transactional(readOnly = true)
    public Page<TourResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<Tour> spec = Specification.where(GenericSpecifications.withSearch(Tour.class, search)).and(GenericSpecifications.withColumnFilters(Tour.class, filters));
        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), GenericSpecifications.sanitizeSort(Tour.class, pageable.getSort()));
        return repository.findAll(spec, sanitized).map(this::toResponse);
    }

    @Transactional
    public TourResponseDto update(UUID id, TourRequestDto request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw TransportException.badRequest("TOUR_INVALID_RANGE", "La fecha fin no puede ser menor a la fecha inicio");
        }
        Tour entity = repository.findById(id).orElseThrow(() -> TransportException.notFound("Gira", id.toString()));
        updateEntity(entity, request);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        Tour entity = repository.findById(id).orElseThrow(() -> TransportException.notFound("Gira", id.toString()));
        long assignmentsUsingTour = assignmentRepository.countByTourId(id);
        if (assignmentsUsingTour > 0) {
            throw TransportException.conflict(
                    "TOUR_IN_USE_ASSIGNMENTS",
                    "No se puede eliminar la gira porque tiene " + assignmentsUsingTour + " asignaciones relacionadas"
            );
        }
        repository.delete(entity);
    }

    private Tour toEntity(TourRequestDto request) {
        Tour entity = new Tour();
        updateEntity(entity, request);
        return entity;
    }

    private void updateEntity(Tour entity, TourRequestDto request) {
        entity.setName(request.getName());
        entity.setExternalNumber(request.getExternalNumber());
        entity.setOrigin(request.getOrigin());
        entity.setDestination(request.getDestination());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setStatus(request.getStatus());
        entity.setPriority(request.getPriority());
        entity.setPassengers(request.getPassengers());
        entity.setRequestedVehicle(request.getRequestedVehicle());
        entity.setRequestedVehicleType(request.getRequestedVehicleType());
        entity.setRequestedDriver(request.getRequestedDriver());
        entity.setResponsible(request.getResponsible());
        entity.setExecutingUnit(request.getExecutingUnit());
        entity.setModality(request.getModality());
        entity.setDurationDays(request.getDurationDays());
        entity.setDepartureTime(request.getDepartureTime());
        entity.setReturnTime(request.getReturnTime());
        entity.setObservations(request.getObservations());
    }

    public TourResponseDto toResponse(Tour entity) {
        return TourResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .externalNumber(entity.getExternalNumber())
                .origin(entity.getOrigin())
                .destination(entity.getDestination())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .passengers(entity.getPassengers())
                .requestedVehicle(entity.getRequestedVehicle())
                .requestedVehicleType(entity.getRequestedVehicleType())
                .requestedDriver(entity.getRequestedDriver())
                .responsible(entity.getResponsible())
                .executingUnit(entity.getExecutingUnit())
                .modality(entity.getModality())
                .durationDays(entity.getDurationDays())
                .departureTime(entity.getDepartureTime())
                .returnTime(entity.getReturnTime())
                .observations(entity.getObservations())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
