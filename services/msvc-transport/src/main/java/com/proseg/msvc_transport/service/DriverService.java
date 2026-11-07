package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.DriverRequestDto;
import com.proseg.msvc_transport.dto.response.DriverResponseDto;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.DriverUnit;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.OrdinaryWindow;
import com.proseg.msvc_transport.service.driverhours.UlloaTours;
import com.proseg.msvc_transport.specification.GenericSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final DraftCatalogService draftCatalogService;

    @Transactional
    public DriverResponseDto create(DriverRequestDto request) {
        if (driverRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw TransportException.conflict("DRIVER_DUPLICATE_NAME", "Ya existe un chofer con ese nombre");
        }
        Driver driver = toEntity(request);
        Driver saved = driverRepository.save(driver);
        draftCatalogService.syncDriver(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DriverResponseDto findById(UUID id) {
        return driverRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> TransportException.notFound("Chofer", id.toString()));
    }

    @Transactional(readOnly = true)
    public Page<DriverResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Map<String, String> columnFilters = new HashMap<>(filters);
        String status = columnFilters.remove("status");
        Specification<Driver> spec = Specification
                .where(GenericSpecifications.withSearch(Driver.class, search))
                .and(GenericSpecifications.withColumnFilters(Driver.class, columnFilters));
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("status")), status.toUpperCase(Locale.ROOT)));
        }
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
        Driver saved = driverRepository.save(driver);
        draftCatalogService.syncDriver(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Driver driver = driverRepository.findById(id).orElseThrow(() -> TransportException.notFound("Chofer", id.toString()));
        draftCatalogService.removeDriver(id);
        driverRepository.delete(driver);
    }

    private Driver toEntity(DriverRequestDto request) {
        Driver driver = new Driver();
        updateEntity(driver, request);
        return driver;
    }

    private void updateEntity(Driver entity, DriverRequestDto request) {
        OrdinaryWindow window = ordinaryWindowOf(request);
        entity.setName(request.getName().trim());
        entity.setStatus(request.getStatus());
        entity.setOrganizationalUnit(request.getOrganizationalUnit() == null
                ? Optional.ofNullable(entity.getOrganizationalUnit()).orElse(DriverUnit.GENERAL_SERVICES)
                : request.getOrganizationalUnit());
        entity.setRestrictions(request.getRestrictions());
        entity.setAllowedVehicleTypes(new HashSet<>(request.getAllowedVehicleTypes()));
        entity.setOrdinaryStart(window.startTime());
        entity.setOrdinaryEnd(window.endTime());
        entity.setUlloaShifts(request.getUlloaShifts().isEmpty()
                ? new HashSet<>()
                : new HashSet<>(UlloaTours.shiftsCoveredBy(window)));
    }

    private OrdinaryWindow ordinaryWindowOf(DriverRequestDto request) {
        LocalTime start = Optional.ofNullable(request.getOrdinaryStart()).orElseGet(OrdinaryWindow.STANDARD::startTime);
        LocalTime end = Optional.ofNullable(request.getOrdinaryEnd()).orElseGet(OrdinaryWindow.STANDARD::endTime);
        return OrdinaryWindow.allowed(start, end).orElseThrow(() -> TransportException.unprocessable(
                "DRIVER_INVALID_ORDINARY_SCHEDULE",
                "El horario ordinario debe ser de " + OrdinaryWindow.allowedDescription()
        ));
    }

    public DriverResponseDto toResponse(Driver entity) {
        OrdinaryWindow window = OrdinaryWindow.of(entity);
        return DriverResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .organizationalUnit(entity.getOrganizationalUnit())
                .restrictions(entity.getRestrictions())
                .allowedVehicleTypes(entity.getAllowedVehicleTypes().stream().sorted().toList())
                .ordinaryStart(DriverHoursRules.timeOf(window.start()))
                .ordinaryEnd(DriverHoursRules.timeOf(window.end()))
                .ulloaShifts(entity.getUlloaShifts().stream().sorted().toList())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
