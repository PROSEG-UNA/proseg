package com.proseg.msvc_maintenance.mapper;

import com.proseg.msvc_maintenance.dto.response.MaintenanceRecordResponseDto;
import com.proseg.msvc_maintenance.entity.MaintenanceRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CompanyMapper.class})
public interface MaintenanceRecordMapper {

    @Mapping(target = "registerId", source = "maintenanceRegister.id")
    MaintenanceRecordResponseDto toResponse(MaintenanceRecord maintenanceRecord);
}
