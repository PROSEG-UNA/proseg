package com.sssi.msvc_maintenance.mapper;

import com.sssi.msvc_maintenance.dto.request.MaintenanceTechnicianRequestDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceTechnicianResponseDto;
import com.sssi.msvc_maintenance.entity.MaintenanceTechnician;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MaintenanceTechnicianMapper {

    @Mapping(target = "maintenanceRequest", ignore = true)
    MaintenanceTechnician toEntity(MaintenanceTechnicianRequestDto request);

    MaintenanceTechnicianResponseDto toResponse(MaintenanceTechnician technician);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "maintenanceRequest", ignore = true)
    void updateEntityFromRequest(MaintenanceTechnicianRequestDto request, @MappingTarget MaintenanceTechnician technician);
}