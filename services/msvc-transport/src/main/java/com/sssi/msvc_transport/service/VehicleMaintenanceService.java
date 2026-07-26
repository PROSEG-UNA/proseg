package com.sssi.msvc_transport.service;

import com.sssi.msvc_transport.dto.request.VehicleMaintenanceRequestDto;
import com.sssi.msvc_transport.dto.response.VehicleMaintenanceResponseDto;
import com.sssi.msvc_transport.entity.Vehicle;
import com.sssi.msvc_transport.entity.VehicleMaintenance;
import com.sssi.msvc_transport.exception.TransportException;
import com.sssi.msvc_transport.repository.VehicleMaintenanceRepository;
import com.sssi.msvc_transport.repository.VehicleRepository;
import com.sssi.msvc_transport.specification.GenericSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleMaintenanceService {

    private static final Set<String> ACTIVE_MAINTENANCE = Set.of("PLANNED", "IN_PROGRESS");

    private final VehicleMaintenanceRepository repository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public VehicleMaintenanceResponseDto create(VehicleMaintenanceRequestDto request) {
        Vehicle vehicle = findVehicle(request.getVehicleId());
        VehicleMaintenance entity = toEntity(request, vehicle);
        VehicleMaintenance saved = repository.save(entity);
        updateVehicleMaintenanceFlag(vehicle);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public VehicleMaintenanceResponseDto findById(UUID id) {
        return repository.findById(id).map(this::toResponse).orElseThrow(() -> TransportException.notFound("Mantenimiento", id.toString()));
    }

    @Transactional(readOnly = true)
    public Page<VehicleMaintenanceResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<VehicleMaintenance> spec = Specification.where(GenericSpecifications.withSearch(VehicleMaintenance.class, search)).and(GenericSpecifications.withColumnFilters(VehicleMaintenance.class, filters));
        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), GenericSpecifications.sanitizeSort(VehicleMaintenance.class, pageable.getSort()));
        return repository.findAll(spec, sanitized).map(this::toResponse);
    }

    @Transactional
    public VehicleMaintenanceResponseDto update(UUID id, VehicleMaintenanceRequestDto request) {
        VehicleMaintenance entity = repository.findById(id).orElseThrow(() -> TransportException.notFound("Mantenimiento", id.toString()));
        Vehicle vehicle = findVehicle(request.getVehicleId());
        updateEntity(entity, request, vehicle);
        VehicleMaintenance saved = repository.save(entity);
        updateVehicleMaintenanceFlag(vehicle);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        VehicleMaintenance entity = repository.findById(id).orElseThrow(() -> TransportException.notFound("Mantenimiento", id.toString()));
        Vehicle vehicle = entity.getVehicle();
        repository.delete(entity);
        updateVehicleMaintenanceFlag(vehicle);
    }

    private Vehicle findVehicle(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId).orElseThrow(() -> TransportException.notFound("Vehículo", vehicleId.toString()));
    }

    private VehicleMaintenance toEntity(VehicleMaintenanceRequestDto request, Vehicle vehicle) {
        VehicleMaintenance entity = new VehicleMaintenance();
        updateEntity(entity, request, vehicle);
        return entity;
    }

    private void updateEntity(VehicleMaintenance entity, VehicleMaintenanceRequestDto request, Vehicle vehicle) {
        entity.setVehicle(vehicle);
        entity.setTitle(request.getTitle());
        entity.setType(request.getType());
        entity.setScheduledDate(request.getScheduledDate());
        entity.setCost(request.getCost());
        entity.setStatus(request.getStatus());
        entity.setNotes(request.getNotes());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setReason(request.getReason());
    }

    private void updateVehicleMaintenanceFlag(Vehicle vehicle) {
        boolean hasActive = repository.existsByVehicleIdAndStatusIn(vehicle.getId(), ACTIVE_MAINTENANCE);
        vehicle.setUnderMaintenance(hasActive);
        if (!hasActive) {
            vehicle.setAvailable(true);
        }
        vehicleRepository.save(vehicle);
    }

    public VehicleMaintenanceResponseDto toResponse(VehicleMaintenance entity) {
        return VehicleMaintenanceResponseDto.builder().id(entity.getId()).vehicleId(entity.getVehicle().getId()).vehiclePlate(entity.getVehicle().getPlate()).title(entity.getTitle()).type(entity.getType()).scheduledDate(entity.getScheduledDate()).cost(entity.getCost()).status(entity.getStatus()).notes(entity.getNotes()).startDate(entity.getStartDate()).endDate(entity.getEndDate()).reason(entity.getReason()).createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).build();
    }
}
