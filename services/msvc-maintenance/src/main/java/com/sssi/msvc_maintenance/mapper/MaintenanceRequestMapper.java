package com.sssi.msvc_maintenance.mapper;

import com.sssi.msvc_maintenance.dto.request.MaintenanceRequestRequestDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.sssi.msvc_maintenance.entity.MaintenanceRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {CompanyMapper.class, MaintenanceTechnicianMapper.class})
public interface MaintenanceRequestMapper {

    @Mapping(target = "company", ignore = true)
    @Mapping(target = "assetId", ignore = true)
    @Mapping(target = "technicians", ignore = true)
    MaintenanceRequest toEntity(MaintenanceRequestRequestDto request);

    MaintenanceRequestResponseDto toResponse(MaintenanceRequest maintenanceRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "assetId", ignore = true)
    @Mapping(target = "technicians", ignore = true)
    void updateEntityFromRequest(MaintenanceRequestRequestDto request, @MappingTarget MaintenanceRequest maintenanceRequest);
}