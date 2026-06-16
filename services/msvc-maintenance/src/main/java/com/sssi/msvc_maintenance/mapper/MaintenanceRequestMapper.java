package com.sssi.msvc_maintenance.mapper;

import com.sssi.msvc_maintenance.dto.request.MaintenanceRequestRequestDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.sssi.msvc_maintenance.entity.MaintenanceEmail;
import com.sssi.msvc_maintenance.entity.MaintenanceRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {CompanyMapper.class, UserCompanyMapper.class})
public interface MaintenanceRequestMapper {

    @Mapping(target = "company", ignore = true)
    @Mapping(target = "campusId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emails", ignore = true)
    @Mapping(target = "assignedTechnicians", ignore = true)
    @Mapping(target = "responsibleUserCompany", ignore = true)
    MaintenanceRequest toEntity(MaintenanceRequestRequestDto request);

    MaintenanceRequestResponseDto toResponse(MaintenanceRequest maintenanceRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "campusId", ignore = true)
    @Mapping(target = "emails", ignore = true)
    @Mapping(target = "assignedTechnicians", ignore = true)
    @Mapping(target = "responsibleUserCompany", ignore = true)
    void updateEntityFromRequest(MaintenanceRequestRequestDto request, @MappingTarget MaintenanceRequest maintenanceRequest);

    default String map(MaintenanceEmail maintenanceEmail) {
        return maintenanceEmail == null ? null : maintenanceEmail.getEmail();
    }
}