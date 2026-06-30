package com.sssi.msvc_maintenance.service.impl;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.msvc_maintenance.client.InventoryClient;
import com.sssi.msvc_maintenance.dto.request.MaintenanceRegisterUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceAssetOptionDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRegisterResponseDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.sssi.msvc_maintenance.entity.MaintenanceRegister;
import com.sssi.msvc_maintenance.entity.MaintenanceRequest;
import com.sssi.msvc_maintenance.entity.UserCompany;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatusTransitions;
import com.sssi.msvc_maintenance.exception.MaintenanceRegisterException;
import com.sssi.msvc_maintenance.exception.MaintenanceRequestException;
import com.sssi.msvc_maintenance.mapper.MaintenanceAssetOptionMapper;
import com.sssi.msvc_maintenance.mapper.MaintenanceRegisterMapper;
import com.sssi.msvc_maintenance.mapper.MaintenanceRequestMapper;
import com.sssi.msvc_maintenance.repository.MaintenanceRegisterRepository;
import com.sssi.msvc_maintenance.repository.MaintenanceRequestRepository;
import com.sssi.msvc_maintenance.repository.UserCompanyRepository;
import com.sssi.msvc_maintenance.service.MaintenanceRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceRegisterServiceImpl implements MaintenanceRegisterService {

    private final MaintenanceRegisterRepository maintenanceRegisterRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final InventoryClient inventoryClient;
    private final MaintenanceRegisterMapper maintenanceRegisterMapper;
    private final MaintenanceRequestMapper maintenanceRequestMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceRequestResponseDto> findAssigned(String keycloakUserId, MaintenanceStatus status, Pageable pageable) {
        Page<MaintenanceRequest> requests = status != null
                ? maintenanceRequestRepository.findByAssignedTechnicians_KeycloakUserIdAndStatus(keycloakUserId, status, pageable)
                : maintenanceRequestRepository.findByAssignedTechnicians_KeycloakUserIdAndStatusIn(
                        keycloakUserId, List.of(MaintenanceStatus.PENDING, MaintenanceStatus.ACCEPTED), pageable);
        return requests.map(maintenanceRequestMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceRequestResponseDto> findHistory(String keycloakUserId, boolean companyWide, Pageable pageable) {
        if (!companyWide) {
            return maintenanceRequestRepository.findByAssignedTechnicians_KeycloakUserId(keycloakUserId, pageable)
                    .map(maintenanceRequestMapper::toResponse);
        }

        List<UUID> companyIds = userCompanyRepository.findAllByKeycloakUserId(keycloakUserId).stream()
                .map(uc -> uc.getCompany().getId())
                .distinct()
                .toList();

        if (companyIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        return maintenanceRequestRepository.findByCompany_IdIn(companyIds, pageable)
                .map(maintenanceRequestMapper::toResponse);
    }

    @Override
    @Transactional
    public MaintenanceRegisterResponseDto getOrCreateByRequest(UUID requestId) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
                .orElseThrow(MaintenanceRequestException::notFound);

        MaintenanceRegister register = maintenanceRegisterRepository.findByMaintenanceRequestId(requestId)
                .orElseGet(() -> maintenanceRegisterRepository.save(buildFromRequest(request)));

        return maintenanceRegisterMapper.toResponse(register);
    }

    @Override
    @Transactional
    public MaintenanceRegisterResponseDto update(UUID id, MaintenanceRegisterUpdateRequestDto request) {
        MaintenanceRegister register = findRegister(id);
        register.setStartDate(request.getStartDate());
        register.setEndDate(request.getEndDate());
        register.setStartTime(request.getStartTime());
        register.setEndTime(request.getEndTime());
        return maintenanceRegisterMapper.toResponse(maintenanceRegisterRepository.save(register));
    }

    @Override
    @Transactional
    public MaintenanceRegisterResponseDto finalizeRegister(UUID id) {
        MaintenanceRegister register = findRegister(id);
        MaintenanceStatusTransitions.validateOrThrow(register.getMaintenanceRequest().getStatus(), MaintenanceStatus.COMPLETED);
        register.setStatus(MaintenanceStatus.COMPLETED);
        register.getMaintenanceRequest().setStatus(MaintenanceStatus.COMPLETED);
        return maintenanceRegisterMapper.toResponse(maintenanceRegisterRepository.save(register));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceAssetOptionDto> findRegisterAssets(UUID id, String search, Map<String, String> filters, Pageable pageable) {
        MaintenanceRegister register = findRegister(id);
        MaintenanceRequest request = register.getMaintenanceRequest();

        List<String> sort = pageable.getSort().stream()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .toList();

        ApiResponse<PageResponse<InventoryAssetResponseDto>> response = request.getBuildingId() != null
                ? inventoryClient.findAssetsByBuilding(request.getBuildingId(), search, pageable.getPageNumber(), pageable.getPageSize(), sort, filters)
                : inventoryClient.findAssetsByCampus(request.getCampusId(), search, pageable.getPageNumber(), pageable.getPageSize(), sort, filters);

        PageResponse<InventoryAssetResponseDto> data = response != null ? response.getData() : null;
        if (data == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<MaintenanceAssetOptionDto> content = (data.getContent() == null ? List.<InventoryAssetResponseDto>of() : data.getContent())
                .stream()
                .map(MaintenanceAssetOptionMapper::toOption)
                .toList();

        return new PageImpl<>(content, pageable, data.getTotalElements());
    }

    private MaintenanceRegister buildFromRequest(MaintenanceRequest request) {
        return MaintenanceRegister.builder()
                .maintenanceRequest(request)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(MaintenanceStatus.PENDING)
                .build();
    }

    private MaintenanceRegister findRegister(UUID id) {
        return maintenanceRegisterRepository.findById(id)
                .orElseThrow(MaintenanceRegisterException::notFound);
    }
}
