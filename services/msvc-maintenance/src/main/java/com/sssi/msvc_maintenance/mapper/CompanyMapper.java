package com.sssi.msvc_maintenance.mapper;

import com.sssi.msvc_maintenance.dto.request.CompanyRequestDto;
import com.sssi.msvc_maintenance.dto.response.CompanyResponseDto;
import com.sssi.msvc_maintenance.dto.response.UserCompanyResponseDto;
import com.sssi.msvc_maintenance.entity.Company;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    @Mapping(target = "userCompanies", ignore = true)
    Company toEntity(CompanyRequestDto request);

    @Mapping(target = "keycloakUserIds", expression = "java(extractKeycloakUserIds(company))")
    @Mapping(target = "userCompanies", expression = "java(extractUserCompanies(company))")
    CompanyResponseDto toResponse(Company company);

    @Mapping(target = "userCompanies", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(CompanyRequestDto request, @MappingTarget Company company);

    default java.util.List<String> extractKeycloakUserIds(Company company) {
        if (company == null || company.getUserCompanies() == null) {
            return java.util.List.of();
        }

        return company.getUserCompanies().stream()
                .map(userCompany -> userCompany.getKeycloakUserId())
                .toList();
    }

    default java.util.List<UserCompanyResponseDto> extractUserCompanies(Company company) {
        if (company == null || company.getUserCompanies() == null) {
            return java.util.List.of();
        }

        return company.getUserCompanies().stream()
                .map(userCompany -> UserCompanyResponseDto.builder()
                        .id(userCompany.getId())
                        .keycloakUserId(userCompany.getKeycloakUserId())
                        .userEmail(userCompany.getUserEmail())
                        .build())
                .toList();
    }
}