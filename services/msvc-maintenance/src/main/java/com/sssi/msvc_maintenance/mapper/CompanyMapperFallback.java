package com.sssi.msvc_maintenance.mapper;

import com.sssi.msvc_maintenance.dto.request.CompanyRequestDto;
import com.sssi.msvc_maintenance.dto.response.CompanyResponseDto;
import com.sssi.msvc_maintenance.entity.Company;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class CompanyMapperFallback implements CompanyMapper {

    @Override
    public Company toEntity(CompanyRequestDto request) {
        if (request == null) {
            return null;
        }

        return Company.builder()
                .name(request.getName())
                .legalId(request.getLegalId())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .build();
    }

    @Override
    public CompanyResponseDto toResponse(Company company) {
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
                .keycloakUserIds(extractKeycloakUserIds(company))
                .userCompanies(extractUserCompanies(company))
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

    @Override
    public void updateEntityFromRequest(CompanyRequestDto request, Company company) {
        if (request == null || company == null) {
            return;
        }

        if (request.getName() != null) {
            company.setName(request.getName());
        }
        if (request.getLegalId() != null) {
            company.setLegalId(request.getLegalId());
        }
        if (request.getContactEmail() != null) {
            company.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            company.setContactPhone(request.getContactPhone());
        }
        if (request.getAddress() != null) {
            company.setAddress(request.getAddress());
        }
    }
}
