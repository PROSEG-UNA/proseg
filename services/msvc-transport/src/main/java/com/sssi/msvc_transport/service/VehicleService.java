package com.sssi.msvc_transport.service;

import com.sssi.msvc_transport.dto.request.VehicleRequestDto;
import com.sssi.msvc_transport.dto.response.VehicleResponseDto;
import com.sssi.msvc_transport.entity.Vehicle;
import com.sssi.msvc_transport.exception.TransportException;
import com.sssi.msvc_transport.repository.AssignmentRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final AssignmentRepository assignmentRepository;
    private final VehicleMaintenanceRepository vehicleMaintenanceRepository;

    @Transactional
    public VehicleResponseDto create(VehicleRequestDto request) {
        if (vehicleRepository.existsByPlateIgnoreCase(request.getPlate())) {
            throw TransportException.conflict("VEHICLE_DUPLICATE_PLATE", "Ya existe un vehículo con esa placa");
        }
        Vehicle entity = toEntity(request);
        return toResponse(vehicleRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public VehicleResponseDto findById(UUID id) {
        return vehicleRepository.findById(id).map(this::toResponse).orElseThrow(() -> TransportException.notFound("Vehículo", id.toString()));
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<Vehicle> spec = Specification.where(GenericSpecifications.withSearch(Vehicle.class, search)).and(GenericSpecifications.withColumnFilters(Vehicle.class, filters));
        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), GenericSpecifications.sanitizeSort(Vehicle.class, pageable.getSort()));
        return vehicleRepository.findAll(spec, sanitized).map(this::toResponse);
    }

    @Transactional
    public VehicleResponseDto update(UUID id, VehicleRequestDto request) {
        Vehicle entity = vehicleRepository.findById(id).orElseThrow(() -> TransportException.notFound("Vehículo", id.toString()));
        if (vehicleRepository.existsByPlateIgnoreCaseAndIdNot(request.getPlate(), id)) {
            throw TransportException.conflict("VEHICLE_DUPLICATE_PLATE", "Ya existe un vehículo con esa placa");
        }
        updateEntity(entity, request);
        return toResponse(vehicleRepository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        Vehicle entity = vehicleRepository.findById(id).orElseThrow(() -> TransportException.notFound("Vehículo", id.toString()));
        long assignmentsUsingVehicle = assignmentRepository.countByVehicleId(id);
        if (assignmentsUsingVehicle > 0) {
            throw TransportException.conflict(
                    "VEHICLE_IN_USE_ASSIGNMENTS",
                    "No se puede eliminar el vehículo porque tiene " + assignmentsUsingVehicle + " asignaciones relacionadas"
            );
        }
        long maintenanceRecords = vehicleMaintenanceRepository.countByVehicleId(id);
        if (maintenanceRecords > 0) {
            throw TransportException.conflict(
                    "VEHICLE_IN_USE_MAINTENANCE",
                    "No se puede eliminar el vehículo porque tiene " + maintenanceRecords + " mantenimientos relacionados"
            );
        }
        vehicleRepository.delete(entity);
    }

    private Vehicle toEntity(VehicleRequestDto request) {
        Vehicle entity = new Vehicle();
        updateEntity(entity, request);
        return entity;
    }

    private void updateEntity(Vehicle entity, VehicleRequestDto request) {
        entity.setPlate(request.getPlate());
        entity.setBrand(request.getBrand());
        entity.setModel(request.getModel());
        entity.setYear(request.getYear());
        entity.setCapacity(request.getCapacity());
        entity.setStatus(request.getStatus());
        entity.setType(request.getType());
        entity.setAvailable(request.isAvailable());
        entity.setUnderMaintenance(request.isUnderMaintenance());
        entity.setLastMaintenanceDate(request.getLastMaintenanceDate());
    }

    public VehicleResponseDto toResponse(Vehicle entity) {
        return VehicleResponseDto.builder().id(entity.getId()).plate(entity.getPlate()).brand(entity.getBrand()).model(entity.getModel()).year(entity.getYear()).capacity(entity.getCapacity()).status(entity.getStatus()).type(entity.getType()).available(entity.isAvailable()).underMaintenance(entity.isUnderMaintenance()).lastMaintenanceDate(entity.getLastMaintenanceDate()).createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).build();
    }
}
