package com.sssi.msvc_maintenance.mapper;

import com.sssi.msvc_maintenance.dto.request.MaintenanceRequestRequestDto;
import com.sssi.msvc_maintenance.dto.response.CompanyResponseDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.sssi.msvc_maintenance.entity.Company;
import com.sssi.msvc_maintenance.entity.MaintenanceRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Primary
public class MaintenanceRequestMapperManual implements MaintenanceRequestMapper {

    private final MaintenanceTechnicianMapper maintenanceTechnicianMapper;

    public MaintenanceRequestMapperManual(MaintenanceTechnicianMapper maintenanceTechnicianMapper) {
        this.maintenanceTechnicianMapper = maintenanceTechnicianMapper;
    }

    @Override
    public MaintenanceRequest toEntity(MaintenanceRequestRequestDto request) {
        if (request == null) {
            return null;
        }

        return MaintenanceRequest.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .priority(request.getPriority())
                .scheduledDate(request.getScheduledDate())
                .observations(request.getObservations())
                .build();
    }

    @Override
    public MaintenanceRequestResponseDto toResponse(MaintenanceRequest maintenanceRequest) {
        if (maintenanceRequest == null) {
            return null;
        }

        return MaintenanceRequestResponseDto.builder()
                .id(maintenanceRequest.getId())
                .company(toCompanyResponse(maintenanceRequest.getCompany()))
                .assetId(maintenanceRequest.getAssetId())
                .title(maintenanceRequest.getTitle())
                .description(maintenanceRequest.getDescription())
                .status(maintenanceRequest.getStatus())
                .priority(maintenanceRequest.getPriority())
                .scheduledDate(maintenanceRequest.getScheduledDate())
                .observations(maintenanceRequest.getObservations())
                .technicians(maintenanceRequest.getTechnicians() == null
                        ? List.of()
                        : maintenanceRequest.getTechnicians().stream()
                            .map(maintenanceTechnicianMapper::toResponse)
                            .toList())
                .createdAt(maintenanceRequest.getCreatedAt())
                .updatedAt(maintenanceRequest.getUpdatedAt())
                .build();
    }

    @Override
    public void updateEntityFromRequest(MaintenanceRequestRequestDto request, MaintenanceRequest maintenanceRequest) {
        if (request == null || maintenanceRequest == null) {
            return;
        }

        if (request.getTitle() != null) {
            maintenanceRequest.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            maintenanceRequest.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            maintenanceRequest.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            maintenanceRequest.setPriority(request.getPriority());
        }
        if (request.getScheduledDate() != null) {
            maintenanceRequest.setScheduledDate(request.getScheduledDate());
        }
        if (request.getObservations() != null) {
            maintenanceRequest.setObservations(request.getObservations());
        }
    }

    private CompanyResponseDto toCompanyResponse(Company company) {
        if (company == null) {
            return null;
        }

        return CompanyResponseDto.builder()
                .id(company.getId())
                .name(company.getName())
                .legalId(company.getLegalId())
                .contactEmail(company.getContactEmail())
                .contactPhone(company.getContactPhone())
                .address(company.getAddress())
                .keycloakUserIds(company.getUserCompanies() == null
                        ? List.of()
                        : company.getUserCompanies().stream()
                            .map(userCompany -> userCompany.getKeycloakUserId())
                            .filter(id -> id != null && !id.isBlank())
                            .toList())
                .userCompanies(company.getUserCompanies() == null
                        ? List.of()
                        : company.getUserCompanies().stream()
                            .map(userCompany -> com.sssi.msvc_maintenance.dto.response.UserCompanyResponseDto.builder()
                                    .id(userCompany.getId())
                                    .keycloakUserId(userCompany.getKeycloakUserId())
                                    .userEmail(userCompany.getUserEmail())
                                    .build())
                            .toList())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}
