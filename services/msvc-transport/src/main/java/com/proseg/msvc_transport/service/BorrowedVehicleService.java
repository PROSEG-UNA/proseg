package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.BorrowedVehicleRequestDto;
import com.proseg.msvc_transport.dto.response.BorrowedVehicleResponseDto;
import com.proseg.msvc_transport.entity.BorrowedVehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.BorrowedVehicleRepository;
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
public class BorrowedVehicleService {

    private final BorrowedVehicleRepository borrowedVehicleRepository;

    @Transactional
    public BorrowedVehicleResponseDto create(BorrowedVehicleRequestDto request) {
        if (borrowedVehicleRepository.existsByPlateIgnoreCase(request.getPlate().trim())) {
            throw TransportException.conflict("BORROWED_VEHICLE_DUPLICATE_PLATE", "Ya existe un vehículo prestado con esa placa");
        }
        BorrowedVehicle entity = toEntity(request);
        return toResponse(borrowedVehicleRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public BorrowedVehicleResponseDto findById(UUID id) {
        return borrowedVehicleRepository.findById(id).map(this::toResponse).orElseThrow(() -> TransportException.notFound("Vehículo prestado", id.toString()));
    }

    @Transactional(readOnly = true)
    public Page<BorrowedVehicleResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<BorrowedVehicle> spec = Specification.where(GenericSpecifications.withSearch(BorrowedVehicle.class, search)).and(GenericSpecifications.withColumnFilters(BorrowedVehicle.class, filters));
        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), GenericSpecifications.sanitizeSort(BorrowedVehicle.class, pageable.getSort()));
        return borrowedVehicleRepository.findAll(spec, sanitized).map(this::toResponse);
    }

    @Transactional
    public BorrowedVehicleResponseDto update(UUID id, BorrowedVehicleRequestDto request) {
        BorrowedVehicle entity = borrowedVehicleRepository.findById(id).orElseThrow(() -> TransportException.notFound("Vehículo prestado", id.toString()));
        if (borrowedVehicleRepository.existsByPlateIgnoreCaseAndIdNot(request.getPlate().trim(), id)) {
            throw TransportException.conflict("BORROWED_VEHICLE_DUPLICATE_PLATE", "Ya existe un vehículo prestado con esa placa");
        }
        updateEntity(entity, request);
        return toResponse(borrowedVehicleRepository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        BorrowedVehicle entity = borrowedVehicleRepository.findById(id).orElseThrow(() -> TransportException.notFound("Vehículo prestado", id.toString()));
        borrowedVehicleRepository.delete(entity);
    }

    private BorrowedVehicle toEntity(BorrowedVehicleRequestDto request) {
        BorrowedVehicle entity = new BorrowedVehicle();
        updateEntity(entity, request);
        return entity;
    }

    private void updateEntity(BorrowedVehicle entity, BorrowedVehicleRequestDto request) {
        entity.setPlate(request.getPlate().trim());
        entity.setCapacity(request.getCapacity());
        entity.setType(request.getType());
    }

    public BorrowedVehicleResponseDto toResponse(BorrowedVehicle entity) {
        return BorrowedVehicleResponseDto.builder().id(entity.getId()).plate(entity.getPlate()).capacity(entity.getCapacity()).type(entity.getType()).createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).build();
    }
}
