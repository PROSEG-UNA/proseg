package com.sssi.msvc_maintenance.mapper;

import com.sssi.msvc_maintenance.dto.response.MaintenanceRegisterResponseDto;
import com.sssi.msvc_maintenance.entity.MaintenanceRegister;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {MaintenanceRequestMapper.class})
public interface MaintenanceRegisterMapper {

    @Mapping(target = "request", source = "maintenanceRequest")
    MaintenanceRegisterResponseDto toResponse(MaintenanceRegister maintenanceRegister);
}
