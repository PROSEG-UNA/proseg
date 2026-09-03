package com.proseg.msvcinventory.mapper;

import com.proseg.msvcinventory.dto.response.FloorResponseDto;
import com.proseg.msvcinventory.entity.Floor;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {BuildingMapper.class})
public interface FloorMapper {

    FloorResponseDto toResponse(Floor floor);
}
