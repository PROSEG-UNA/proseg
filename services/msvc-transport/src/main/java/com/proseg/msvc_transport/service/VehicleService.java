package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.VehicleRequestDto;
import com.proseg.msvc_transport.dto.response.VehicleResponseDto;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.specification.GenericSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DraftCatalogService draftCatalogService;
    private final VehicleOpenDraftTourFinder openDraftTourFinder;

    @Transactional
    public VehicleResponseDto create(VehicleRequestDto request) {
        if (vehicleRepository.existsByPlateIgnoreCase(request.getPlate().trim())) {
            throw TransportException.conflict("VEHICLE_DUPLICATE_PLATE", "Ya existe un vehículo con esa placa");
        }
        Vehicle entity = toEntity(request);
        Vehicle saved = vehicleRepository.save(entity);
        draftCatalogService.syncVehicle(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public VehicleResponseDto findById(UUID id) {
        return vehicleRepository.findById(id).map(this::toResponse).orElseThrow(() -> TransportException.notFound("Vehículo", id.toString()));
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Map<String, String> columnFilters = new HashMap<>();
        if (filters != null) {
            columnFilters.putAll(filters);
        }
        String available = columnFilters.remove("available");
        Specification<Vehicle> spec = Specification.where(GenericSpecifications.withSearch(Vehicle.class, search))
                .and(GenericSpecifications.withColumnFilters(Vehicle.class, columnFilters))
                .and(availableFilter(available));
        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), GenericSpecifications.sanitizeSort(Vehicle.class, pageable.getSort()));
        return vehicleRepository.findAll(spec, sanitized).map(this::toResponse);
    }

    static Specification<Vehicle> availableFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return (root, query, cb) -> null;
        }
        boolean available = Boolean.parseBoolean(raw);
        return (root, query, cb) -> cb.equal(root.get("available"), available);
    }

    @Transactional
    public VehicleResponseDto update(UUID id, VehicleRequestDto request) {
        Vehicle entity = vehicleRepository.findById(id).orElseThrow(() -> TransportException.notFound("Vehículo", id.toString()));
        if (vehicleRepository.existsByPlateIgnoreCaseAndIdNot(request.getPlate().trim(), id)) {
            throw TransportException.conflict("VEHICLE_DUPLICATE_PLATE", "Ya existe un vehículo con esa placa");
        }
        updateEntity(entity, request);
        Vehicle saved = vehicleRepository.save(entity);
        draftCatalogService.syncVehicle(saved);
        VehicleResponseDto response = toResponse(saved);
        response.setOpenDraftTours(saved.isAvailable() ? List.of() : openDraftTourFinder.find(saved.getId()));
        return response;
    }

    @Transactional
    public void delete(UUID id) {
        Vehicle entity = vehicleRepository.findById(id).orElseThrow(() -> TransportException.notFound("Vehículo", id.toString()));
        draftCatalogService.removeVehicle(id);
        vehicleRepository.delete(entity);
    }

    private Vehicle toEntity(VehicleRequestDto request) {
        Vehicle entity = new Vehicle();
        updateEntity(entity, request);
        return entity;
    }

    private void updateEntity(Vehicle entity, VehicleRequestDto request) {
        entity.setPlate(request.getPlate().trim());
        entity.setCapacity(request.getCapacity());
        entity.setType(request.getType());
        entity.setAvailable(request.isAvailable());
    }

    public VehicleResponseDto toResponse(Vehicle entity) {
        return VehicleResponseDto.builder().id(entity.getId()).plate(entity.getPlate()).capacity(entity.getCapacity()).type(entity.getType()).available(entity.isAvailable()).createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).build();
    }
}
