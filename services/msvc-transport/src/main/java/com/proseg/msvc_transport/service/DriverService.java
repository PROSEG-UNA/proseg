package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.DriverRequestDto;
import com.proseg.msvc_transport.dto.response.DriverResponseDto;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.specification.GenericSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;

    @Transactional
    public DriverResponseDto create(DriverRequestDto request) {
        if (driverRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw TransportException.conflict("DRIVER_DUPLICATE_NAME", "Ya existe un chofer con ese nombre");
        }
        Driver driver = toEntity(request);
        return toResponse(driverRepository.save(driver));
    }

    @Transactional(readOnly = true)
    public DriverResponseDto findById(UUID id) {
        return driverRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> TransportException.notFound("Chofer", id.toString()));
    }

    @Transactional(readOnly = true)
    public Page<DriverResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<Driver> spec = Specification
                .where(GenericSpecifications.withSearch(Driver.class, search))
                .and(GenericSpecifications.withColumnFilters(Driver.class, filters));
        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), GenericSpecifications.sanitizeSort(Driver.class, pageable.getSort()));
        return driverRepository.findAll(spec, sanitized).map(this::toResponse);
    }

    @Transactional
    public DriverResponseDto update(UUID id, DriverRequestDto request) {
        Driver driver = driverRepository.findById(id).orElseThrow(() -> TransportException.notFound("Chofer", id.toString()));
        if (driverRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw TransportException.conflict("DRIVER_DUPLICATE_NAME", "Ya existe un chofer con ese nombre");
        }
        updateEntity(driver, request);
        return toResponse(driverRepository.save(driver));
    }

    @Transactional
    public void delete(UUID id) {
        Driver driver = driverRepository.findById(id).orElseThrow(() -> TransportException.notFound("Chofer", id.toString()));
        driverRepository.delete(driver);
    }

    private Driver toEntity(DriverRequestDto request) {
        Driver driver = new Driver();
        updateEntity(driver, request);
        return driver;
    }

    private void updateEntity(Driver entity, DriverRequestDto request) {
        entity.setName(request.getName().trim());
        entity.setStatus(request.getStatus());
        entity.setRestrictions(request.getRestrictions());
        entity.setAllowedVehicleTypes(new HashSet<>(request.getAllowedVehicleTypes()));
    }

    public DriverResponseDto toResponse(Driver entity) {
        return DriverResponseDto.builder().id(entity.getId()).name(entity.getName()).status(entity.getStatus()).restrictions(entity.getRestrictions()).allowedVehicleTypes(entity.getAllowedVehicleTypes().stream().sorted().toList()).createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).build();
    }
}
