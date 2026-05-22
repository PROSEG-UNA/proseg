package com.sssi.msvc_maintenance.mapper;

import com.sssi.msvc_maintenance.dto.request.CompanyRequestDto;
import com.sssi.msvc_maintenance.dto.response.CompanyResponseDto;
import com.sssi.msvc_maintenance.entity.Company;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    Company toEntity(CompanyRequestDto request);

    CompanyResponseDto toResponse(Company company);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(CompanyRequestDto request, @MappingTarget Company company);
}