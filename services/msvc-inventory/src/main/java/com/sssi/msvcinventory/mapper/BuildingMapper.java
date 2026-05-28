package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.BuildingRequestDto;
import com.sssi.msvcinventory.dto.response.BuildingResponseDto;
import com.sssi.msvcinventory.entity.Building;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {CampusMapper.class})
public interface BuildingMapper {

    @Mapping(target = "campus", ignore = true)
    Building toEntity(BuildingRequestDto request);

    BuildingResponseDto toResponse(Building building);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "campus", ignore = true)
    void updateEntityFromRequest(BuildingRequestDto request, @MappingTarget Building building);
}
