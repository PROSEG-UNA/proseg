package com.sssi.msvc_transport.service;

import com.sssi.msvc_transport.dto.request.DriverRequestDto;
import com.sssi.msvc_transport.dto.response.DriverResponseDto;
import com.sssi.msvc_transport.entity.Driver;
import com.sssi.msvc_transport.exception.TransportException;
import com.sssi.msvc_transport.repository.AssignmentRepository;
import com.sssi.msvc_transport.repository.DriverRepository;
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
public class DriverService {

    private final DriverRepository driverRepository;
    private final AssignmentRepository assignmentRepository;

    @Transactional
    public DriverResponseDto create(DriverRequestDto request) {
        if (driverRepository.existsByDocumentIdIgnoreCase(request.getDocumentId())) {
            throw TransportException.conflict("DRIVER_DUPLICATE_DOCUMENT", "Ya existe un chofer con ese documento");
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
        if (driverRepository.existsByDocumentIdIgnoreCaseAndIdNot(request.getDocumentId(), id)) {
            throw TransportException.conflict("DRIVER_DUPLICATE_DOCUMENT", "Ya existe un chofer con ese documento");
        }
        updateEntity(driver, request);
        return toResponse(driverRepository.save(driver));
    }

    @Transactional
    public void delete(UUID id) {
        Driver driver = driverRepository.findById(id).orElseThrow(() -> TransportException.notFound("Chofer", id.toString()));
        long assignmentsUsingDriver = assignmentRepository.countByDriverId(id);
        if (assignmentsUsingDriver > 0) {
            throw TransportException.conflict(
                    "DRIVER_IN_USE",
                    "No se puede eliminar el chofer porque tiene " + assignmentsUsingDriver + " asignaciones relacionadas"
            );
        }
        driverRepository.delete(driver);
    }

    private Driver toEntity(DriverRequestDto request) {
        Driver driver = new Driver();
        updateEntity(driver, request);
        return driver;
    }

    private void updateEntity(Driver entity, DriverRequestDto request) {
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setDocumentId(request.getDocumentId());
        entity.setLicenseNumber(request.getLicenseNumber());
        entity.setPhone(request.getPhone());
        entity.setEmail(request.getEmail());
        entity.setStatus(request.getStatus());
        entity.setRestrictions(request.getRestrictions());
        entity.setAvailability(request.isAvailability());
        entity.setAccumulatedHours(request.getAccumulatedHours());
        entity.setFreeWeekendsCount(request.getFreeWeekendsCount());
        entity.setOvertimeHours(request.getOvertimeHours());
        entity.setSurplusHours(request.getSurplusHours());
        entity.setJornadaHoursPerDay(request.getJornadaHoursPerDay());
    }

    public DriverResponseDto toResponse(Driver entity) {
        return DriverResponseDto.builder().id(entity.getId()).firstName(entity.getFirstName()).lastName(entity.getLastName()).documentId(entity.getDocumentId()).licenseNumber(entity.getLicenseNumber()).phone(entity.getPhone()).email(entity.getEmail()).status(entity.getStatus()).restrictions(entity.getRestrictions()).availability(entity.isAvailability()).accumulatedHours(entity.getAccumulatedHours()).freeWeekendsCount(entity.getFreeWeekendsCount()).overtimeHours(entity.getOvertimeHours()).surplusHours(entity.getSurplusHours()).jornadaHoursPerDay(entity.getJornadaHoursPerDay()).createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).build();
    }
}
