package com.proseg.msvc_maintenance.service.impl;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_maintenance.client.InventoryClient;
import com.proseg.msvc_maintenance.dto.response.InventoryBuildingEmailResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryCampusResponseDto;
import com.proseg.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.proseg.msvc_maintenance.entity.Company;
import com.proseg.msvc_maintenance.entity.MaintenanceEmail;
import com.proseg.msvc_maintenance.entity.MaintenanceRequest;
import com.proseg.msvc_maintenance.entity.UserCompany;
import com.proseg.msvc_maintenance.mapper.MaintenanceRequestMapper;
import com.proseg.msvc_maintenance.repository.CompanyRepository;
import com.proseg.msvc_maintenance.repository.MaintenanceEmailRepository;
import com.proseg.msvc_maintenance.repository.UserCompanyRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

final class MaintenanceRequestServiceImplTestSupport {

    private MaintenanceRequestServiceImplTestSupport() {
    }

    static void stubUpdateDependencies(
            CompanyRepository companyRepository,
            UserCompanyRepository userCompanyRepository,
            MaintenanceEmailRepository maintenanceEmailRepository,
            InventoryClient inventoryClient,
            MaintenanceRequestMapper maintenanceRequestMapper,
            UUID companyId,
            UUID campusId,
            UUID technicianId) {
        Company company = Company.builder().id(companyId).build();
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        UserCompany technician = UserCompany.builder()
                .id(technicianId)
                .company(company)
                .build();
        when(userCompanyRepository.findAllById(List.of(technicianId))).thenReturn(List.of(technician));

        stubCampusEmails(inventoryClient, campusId, List.of("a@b.com"));

        when(maintenanceEmailRepository.findByEmail(eq("a@b.com")))
                .thenReturn(Optional.of(MaintenanceEmail.builder().email("a@b.com").build()));

        when(maintenanceRequestMapper.toResponse(any(MaintenanceRequest.class)))
                .thenReturn(MaintenanceRequestResponseDto.builder().build());
    }

    static void stubCampusEmails(InventoryClient inventoryClient, UUID campusId, List<String> emails) {
        List<InventoryBuildingEmailResponseDto> dtoList = emails.stream()
                .map(email -> InventoryBuildingEmailResponseDto.builder().email(email).build())
                .toList();
        when(inventoryClient.findCampusEmails(campusId))
                .thenReturn(new ApiResponse<>(null, dtoList, 200));
    }

    static void stubCampusName(InventoryClient inventoryClient, UUID campusId, String name) {
        when(inventoryClient.findCampusById(campusId))
                .thenReturn(new ApiResponse<>(null, InventoryCampusResponseDto.builder().name(name).build(), 200));
    }

    static void stubBuildingName(InventoryClient inventoryClient, UUID buildingId, String name) {
        when(inventoryClient.findBuildingById(buildingId))
                .thenReturn(new ApiResponse<>(null, InventoryBuildingResponseDto.builder().name(name).build(), 200));
    }
}
