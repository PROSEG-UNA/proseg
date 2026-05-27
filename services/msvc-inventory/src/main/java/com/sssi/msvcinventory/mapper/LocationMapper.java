package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.LocationRequestDto;
import com.sssi.msvcinventory.dto.response.LocationResponseDto;
import com.sssi.msvcinventory.entity.Location;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {FloorMapper.class})
public interface LocationMapper {

    @Mapping(target = "floor", ignore = true)
    Location toEntity(LocationRequestDto request);

    LocationResponseDto toResponse(Location location);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "floor", ignore = true)
    void updateEntityFromRequest(LocationRequestDto request, @MappingTarget Location location);
}
