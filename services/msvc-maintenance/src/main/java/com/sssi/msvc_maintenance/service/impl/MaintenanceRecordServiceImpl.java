package com.sssi.msvc_maintenance.service.impl;

import com.sssi.msvc_maintenance.dto.request.MaintenanceRecordRequestDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRecordResponseDto;
import com.sssi.msvc_maintenance.entity.Company;
import com.sssi.msvc_maintenance.entity.MaintenanceRecord;
import com.sssi.msvc_maintenance.entity.MaintenanceRegister;
import com.sssi.msvc_maintenance.entity.MaintenanceRequest;
import com.sssi.msvc_maintenance.entity.UserCompany;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import com.sssi.msvc_maintenance.exception.MaintenanceRegisterException;
import com.sssi.msvc_maintenance.mapper.MaintenanceRecordMapper;
import com.sssi.msvc_maintenance.repository.MaintenanceRecordRepository;
import com.sssi.msvc_maintenance.repository.MaintenanceRegisterRepository;
import com.sssi.msvc_maintenance.service.MaintenanceRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceRecordServiceImpl implements MaintenanceRecordService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final MaintenanceRegisterRepository maintenanceRegisterRepository;
    private final MaintenanceRecordMapper maintenanceRecordMapper;

    @Override
    @Transactional
    public MaintenanceRecordResponseDto create(UUID registerId, MaintenanceRecordRequestDto request,
                                               String keycloakUserId, String userEmail) {
        MaintenanceRegister register = maintenanceRegisterRepository.findById(registerId)
                .orElseThrow(MaintenanceRegisterException::notFound);

        MaintenanceRequest maintenanceRequest = register.getMaintenanceRequest();

        if (maintenanceRequest.getStatus() != MaintenanceStatus.PENDING) {
            throw MaintenanceRegisterException.notPending();
        }

        Company company = maintenanceRequest.getCompany();

        boolean isAssignedTechnician = maintenanceRequest.getAssignedTechnicians() != null
                && maintenanceRequest.getAssignedTechnicians().stream()
                        .map(UserCompany::getKeycloakUserId)
                        .anyMatch(id -> id != null && id.equals(keycloakUserId));

        if (!isAssignedTechnician) {
            throw MaintenanceRegisterException.userWithoutCompany();
        }

        MaintenanceRecord record = MaintenanceRecord.builder()
                .maintenanceRegister(register)
                .assetId(request.getAssetId())
                .company(company)
                .keycloakUserId(keycloakUserId)
                .userEmail(userEmail)
                .description(request.getDescription())
                .build();

        return maintenanceRecordMapper.toResponse(maintenanceRecordRepository.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceRecordResponseDto> findByRegister(UUID registerId, UUID assetId, Pageable pageable) {
        Page<MaintenanceRecord> records = assetId != null
                ? maintenanceRecordRepository.findByMaintenanceRegisterIdAndAssetId(registerId, assetId, pageable)
                : maintenanceRecordRepository.findByMaintenanceRegisterId(registerId, pageable);
        return records.map(maintenanceRecordMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceRecordResponseDto> findByAsset(UUID assetId, Pageable pageable) {
        return maintenanceRecordRepository
                .findByAssetIdOrderByCreatedAtDesc(assetId, pageable)
                .map(maintenanceRecordMapper::toResponse);
    }
}
