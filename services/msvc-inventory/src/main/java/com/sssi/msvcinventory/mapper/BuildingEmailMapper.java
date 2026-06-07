package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.response.BuildingEmailResponseDto;
import com.sssi.msvcinventory.entity.BuildingEmail;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {BuildingMapper.class})
public interface BuildingEmailMapper {

    BuildingEmailResponseDto toResponse(BuildingEmail buildingEmail);
}