package com.proseg.msvcinventory.mapper;

import com.proseg.msvcinventory.dto.response.BuildingEmailResponseDto;
import com.proseg.msvcinventory.entity.BuildingEmail;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {BuildingMapper.class})
public interface BuildingEmailMapper {

    BuildingEmailResponseDto toResponse(BuildingEmail buildingEmail);
}