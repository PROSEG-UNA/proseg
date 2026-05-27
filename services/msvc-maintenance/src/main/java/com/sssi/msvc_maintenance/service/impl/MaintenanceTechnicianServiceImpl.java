package com.sssi.msvc_maintenance.service.impl;

import com.sssi.msvc_maintenance.dto.request.MaintenanceTechnicianRequestDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceTechnicianResponseDto;
import com.sssi.msvc_maintenance.entity.MaintenanceRequest;
import com.sssi.msvc_maintenance.entity.MaintenanceTechnician;
import com.sssi.msvc_maintenance.exception.MaintenanceRequestException;
import com.sssi.msvc_maintenance.exception.MaintenanceTechnicianException;
import com.sssi.msvc_maintenance.mapper.MaintenanceTechnicianMapper;
import com.sssi.msvc_maintenance.repository.MaintenanceRequestRepository;
import com.sssi.msvc_maintenance.repository.MaintenanceTechnicianRepository;
import com.sssi.msvc_maintenance.service.MaintenanceTechnicianService;
import com.sssi.msvc_maintenance.specification.GenericSpecifications;
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
public class MaintenanceTechnicianServiceImpl implements MaintenanceTechnicianService {

    private final MaintenanceTechnicianRepository maintenanceTechnicianRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final MaintenanceTechnicianMapper maintenanceTechnicianMapper;

    @Override
    @Transactional
    public MaintenanceTechnicianResponseDto create(UUID maintenanceRequestId, MaintenanceTechnicianRequestDto request) {

        MaintenanceRequest maintenanceRequest = maintenanceRequestRepository.findById(maintenanceRequestId)
                .orElseThrow(MaintenanceRequestException::notFound);

        MaintenanceTechnician technician = maintenanceTechnicianMapper.toEntity(request);
        technician.setMaintenanceRequest(maintenanceRequest);

        return maintenanceTechnicianMapper.toResponse(maintenanceTechnicianRepository.save(technician));
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceTechnicianResponseDto findById(UUID id) {
        return maintenanceTechnicianRepository.findById(id)
                .map(maintenanceTechnicianMapper::toResponse)
                .orElseThrow(MaintenanceTechnicianException::notFound);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceTechnicianResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<MaintenanceTechnician> spec = Specification
                .where(GenericSpecifications.<MaintenanceTechnician>withSearch(MaintenanceTechnician.class, search))
                .and(GenericSpecifications.<MaintenanceTechnician>withColumnFilters(MaintenanceTechnician.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(MaintenanceTechnician.class, pageable.getSort())
        );

        return maintenanceTechnicianRepository.findAll(spec, sanitized).map(maintenanceTechnicianMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceTechnicianResponseDto> findByMaintenanceRequestId(UUID maintenanceRequestId, Pageable pageable) {

        if (!maintenanceRequestRepository.existsById(maintenanceRequestId)) {
            throw MaintenanceRequestException.notFound();
        }

        return maintenanceTechnicianRepository.findByMaintenanceRequestId(maintenanceRequestId, pageable)
                .map(maintenanceTechnicianMapper::toResponse);
    }

    @Override
    @Transactional
    public MaintenanceTechnicianResponseDto update(UUID id, MaintenanceTechnicianRequestDto request) {

        MaintenanceTechnician technician = maintenanceTechnicianRepository.findById(id)
                .orElseThrow(MaintenanceTechnicianException::notFound);

        maintenanceTechnicianMapper.updateEntityFromRequest(request, technician);

        return maintenanceTechnicianMapper.toResponse(maintenanceTechnicianRepository.save(technician));
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        MaintenanceTechnician technician = maintenanceTechnicianRepository.findById(id)
                .orElseThrow(MaintenanceTechnicianException::notFound);

        maintenanceTechnicianRepository.delete(technician);
    }
}

