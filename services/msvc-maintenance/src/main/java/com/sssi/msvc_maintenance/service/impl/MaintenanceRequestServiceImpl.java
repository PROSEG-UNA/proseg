package com.sssi.msvc_maintenance.service.impl;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.msvc_maintenance.client.InventoryClient;
import com.sssi.msvc_maintenance.dto.request.MaintenanceRequestRequestDto;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceAssetOptionDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.sssi.msvc_maintenance.entity.Company;
import com.sssi.msvc_maintenance.entity.MaintenanceRequest;
import com.sssi.msvc_maintenance.exception.CompanyException;
import com.sssi.msvc_maintenance.exception.MaintenanceRequestException;
import com.sssi.msvc_maintenance.mapper.MaintenanceRequestMapper;
import com.sssi.msvc_maintenance.repository.CompanyRepository;
import com.sssi.msvc_maintenance.repository.MaintenanceRequestRepository;
import com.sssi.msvc_maintenance.repository.MaintenanceTechnicianRepository;
import com.sssi.msvc_maintenance.service.MaintenanceRequestService;
import com.sssi.msvc_maintenance.specification.GenericSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceRequestServiceImpl implements MaintenanceRequestService {

    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final CompanyRepository companyRepository;
    private final MaintenanceTechnicianRepository maintenanceTechnicianRepository;
    private final MaintenanceRequestMapper maintenanceRequestMapper;
    private final InventoryClient inventoryClient;

    @Override
    @Transactional
    public MaintenanceRequestResponseDto create(MaintenanceRequestRequestDto request) {

        UUID companyId = parseUuid(request.getCompanyId(), "companyId");
        UUID assetId = parseUuid(request.getAssetId(), "assetId");

        Company company = companyRepository.findById(companyId)
                .orElseThrow(CompanyException::notFound);
        validateAssetExists(assetId);

        MaintenanceRequest maintenanceRequest = maintenanceRequestMapper.toEntity(request);
        maintenanceRequest.setCompany(company);
        maintenanceRequest.setAssetId(assetId);

        return maintenanceRequestMapper.toResponse(maintenanceRequestRepository.save(maintenanceRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceRequestResponseDto findById(UUID id) {
        return maintenanceRequestRepository.findById(id)
                .map(maintenanceRequestMapper::toResponse)
                .orElseThrow(MaintenanceRequestException::notFound);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceRequestResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<MaintenanceRequest> spec = Specification
                .where(GenericSpecifications.<MaintenanceRequest>withSearch(MaintenanceRequest.class, search))
                .and(GenericSpecifications.<MaintenanceRequest>withColumnFilters(MaintenanceRequest.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(MaintenanceRequest.class, pageable.getSort())
        );

        return maintenanceRequestRepository.findAll(spec, sanitized).map(maintenanceRequestMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceRequestResponseDto> findByCompanyId(UUID companyId, Pageable pageable) {

        if (!companyRepository.existsById(companyId)) {
            throw CompanyException.notFound();
        }

        return maintenanceRequestRepository.findByCompanyId(companyId, pageable)
                .map(maintenanceRequestMapper::toResponse);
    }

    @Override
    @Transactional
    public MaintenanceRequestResponseDto update(UUID id, MaintenanceRequestRequestDto request) {

        MaintenanceRequest maintenanceRequest = maintenanceRequestRepository.findById(id)
                .orElseThrow(MaintenanceRequestException::notFound);

        UUID companyId = parseUuid(request.getCompanyId(), "companyId");
        UUID assetId = parseUuid(request.getAssetId(), "assetId");

        Company company = companyRepository.findById(companyId)
                .orElseThrow(CompanyException::notFound);
        validateAssetExists(assetId);

        maintenanceRequestMapper.updateEntityFromRequest(request, maintenanceRequest);
        maintenanceRequest.setCompany(company);
        maintenanceRequest.setAssetId(assetId);

        return maintenanceRequestMapper.toResponse(maintenanceRequestRepository.save(maintenanceRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceAssetOptionDto> findAvailableAssets(String search, Pageable pageable) {
        List<String> sort = pageable.getSort().stream()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .toList();

        ApiResponse<PageResponse<InventoryAssetResponseDto>> response = inventoryClient.findAssets(
                search,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort.isEmpty() ? null : sort
        );

        PageResponse<InventoryAssetResponseDto> data = response != null ? response.getData() : null;
        if (data == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<MaintenanceAssetOptionDto> content = (data.getContent() == null ? List.<InventoryAssetResponseDto>of() : data.getContent())
                .stream()
                .map(this::toAssetOption)
                .toList();

        return new PageImpl<>(content, pageable, data.getTotalElements());
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        MaintenanceRequest maintenanceRequest = maintenanceRequestRepository.findById(id)
                .orElseThrow(MaintenanceRequestException::notFound);

        if (maintenanceTechnicianRepository.existsByMaintenanceRequestId(id)) {
            throw MaintenanceRequestException.inUse();
        }

        maintenanceRequestRepository.delete(maintenanceRequest);
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("El campo '" + fieldName + "' debe ser un UUID válido", ex);
        }
    }

    private void validateAssetExists(UUID assetId) {
        try {
            ApiResponse<InventoryAssetResponseDto> response = inventoryClient.findAssetById(assetId);
            if (response == null || response.getData() == null) {
                throw MaintenanceRequestException.invalidAsset();
            }
        } catch (MaintenanceRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw MaintenanceRequestException.invalidAsset();
        }
    }

    private MaintenanceAssetOptionDto toAssetOption(InventoryAssetResponseDto asset) {
        return MaintenanceAssetOptionDto.builder()
                .id(asset.getId())
                .assetNumber(asset.getAssetNumber())
                .serialNumber(asset.getSerialNumber())
                .kind(asset.getKind())
                .status(asset.getStatus())
                .modelName(asset.getModel() != null ? asset.getModel().getName() : null)
                .locationName(asset.getLocation() != null ? asset.getLocation().getName() : null)
                .build();
    }
}

