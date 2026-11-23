package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.ExternalVehicleRequestDto;
import com.proseg.msvc_transport.dto.response.ExternalVehicleResponseDto;
import com.proseg.msvc_transport.entity.ExternalVehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.ExternalVehicleRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftAssignmentRepository;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.specification.DisplayLabels;
import com.proseg.msvc_transport.specification.DisplaySearch;
import com.proseg.msvc_transport.specification.GenericSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExternalVehicleService {

    private static final Set<String> SORTABLE_NON_FILTERABLE_FIELDS = Set.of("available");

    private final ExternalVehicleRepository externalVehicleRepository;
    private final TourProcessingDraftAssignmentRepository assignmentRepository;

    @Transactional
    public ExternalVehicleResponseDto create(ExternalVehicleRequestDto request) {
        ExternalVehicle entity = externalVehicleRepository.findByPlateIgnoreCase(request.getPlate().trim()).orElse(null);
        if (entity != null && !entity.isDeleted()) {
            throw TransportException.conflict("EXTERNAL_VEHICLE_DUPLICATE_PLATE", "Ya existe un vehículo de contratación externa con esa placa");
        }
        if (entity == null) {
            entity = new ExternalVehicle();
        } else {
            entity.markAsActive();
        }
        updateEntity(entity, request);
        return toResponse(externalVehicleRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public ExternalVehicleResponseDto findById(UUID id) {
        return externalVehicleRepository.findByIdAndIsDeletedFalse(id).map(this::toResponse).orElseThrow(() -> notFound(id));
    }

    @Transactional(readOnly = true)
    public Page<ExternalVehicleResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Map<String, String> columnFilters = new HashMap<>();
        if (filters != null) {
            columnFilters.putAll(filters);
        }
        String available = columnFilters.remove("available");
        Specification<ExternalVehicle> notDeleted = (root, query, builder) -> builder.isFalse(root.get("isDeleted"));
        Specification<ExternalVehicle> spec = notDeleted
                .and(GenericSpecifications.withSearch(
                        ExternalVehicle.class,
                        search,
                        DisplaySearch.labelMatching("available", DisplayLabels.VEHICLE_AVAILABILITY, search)))
                .and(GenericSpecifications.withColumnFilters(ExternalVehicle.class, columnFilters))
                .and(availableFilter(available));
        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), stableSort(GenericSpecifications.sanitizeSort(ExternalVehicle.class, pageable.getSort(), SORTABLE_NON_FILTERABLE_FIELDS)));
        return externalVehicleRepository.findAll(spec, sanitized).map(this::toResponse);
    }

    static Sort stableSort(Sort requested) {
        Sort base = requested.isSorted() ? requested : Sort.by("provider");
        return base.getOrderFor("plate") == null ? base.and(Sort.by("plate")) : base;
    }

    static Specification<ExternalVehicle> availableFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return (root, query, cb) -> null;
        }
        boolean available = Boolean.parseBoolean(raw);
        return (root, query, cb) -> cb.equal(root.get("available"), available);
    }

    @Transactional
    public ExternalVehicleResponseDto update(UUID id, ExternalVehicleRequestDto request) {
        ExternalVehicle entity = externalVehicleRepository.findByIdAndIsDeletedFalse(id).orElseThrow(() -> notFound(id));
        if (externalVehicleRepository.existsByPlateIgnoreCaseAndIdNot(request.getPlate().trim(), id)) {
            throw TransportException.conflict("EXTERNAL_VEHICLE_DUPLICATE_PLATE", "Ya existe un vehículo de contratación externa con esa placa");
        }
        String previousPlateKey = DriverHoursRules.plateKey(entity.getPlate());
        updateEntity(entity, request);
        ExternalVehicle saved = externalVehicleRepository.save(entity);
        assignmentRepository.renameExternalVehicle(previousPlateKey, saved.getPlate(), saved.getProvider());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        ExternalVehicle entity = externalVehicleRepository.findByIdAndIsDeletedFalse(id).orElseThrow(() -> notFound(id));
        if (assignmentRepository.isExternalVehicleInOpenDraft(DriverHoursRules.plateKey(entity.getPlate()))) {
            throw TransportException.conflict("EXTERNAL_VEHICLE_IN_USE", "No se puede eliminar un vehículo de contratación externa asignado a un borrador abierto");
        }
        externalVehicleRepository.delete(entity);
    }

    private TransportException notFound(UUID id) {
        return TransportException.notFound("Vehículo de contratación externa", id.toString());
    }

    private void updateEntity(ExternalVehicle entity, ExternalVehicleRequestDto request) {
        entity.setProvider(request.getProvider().trim());
        entity.setPlate(request.getPlate().trim());
        entity.setCapacity(request.getCapacity());
        entity.setAvailable(request.isAvailable());
    }

    public ExternalVehicleResponseDto toResponse(ExternalVehicle entity) {
        return ExternalVehicleResponseDto.builder()
                .id(entity.getId())
                .provider(entity.getProvider())
                .plate(entity.getPlate())
                .capacity(entity.getCapacity())
                .available(entity.isAvailable())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
