package com.sssi.msvc_maintenance.mapper;

import com.sssi.msvc_maintenance.dto.request.UserCompanyRequestDto;
import com.sssi.msvc_maintenance.dto.response.UserCompanyResponseDto;
import com.sssi.msvc_maintenance.entity.UserCompany;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserCompanyMapper {

    @Mapping(target = "company", ignore = true)
    UserCompany toEntity(UserCompanyRequestDto request);

    @Mapping(target = "company", ignore = true)
    UserCompanyResponseDto toResponse(UserCompany userCompany);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "company", ignore = true)
    void updateEntityFromRequest(UserCompanyRequestDto request, @MappingTarget UserCompany userCompany);
}