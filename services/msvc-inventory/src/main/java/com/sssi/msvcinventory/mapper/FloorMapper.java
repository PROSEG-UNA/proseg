package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.response.FloorResponseDto;
import com.sssi.msvcinventory.entity.Floor;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {BuildingMapper.class})
public interface FloorMapper {

    FloorResponseDto toResponse(Floor floor);
}
