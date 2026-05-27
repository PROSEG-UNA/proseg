package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.CampusRequestDto;
import com.sssi.msvcinventory.dto.response.CampusResponseDto;
import com.sssi.msvcinventory.entity.Campus;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CampusMapper {

    Campus toEntity(CampusRequestDto request);

    CampusResponseDto toResponse(Campus campus);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(CampusRequestDto request, @MappingTarget Campus campus);
}
