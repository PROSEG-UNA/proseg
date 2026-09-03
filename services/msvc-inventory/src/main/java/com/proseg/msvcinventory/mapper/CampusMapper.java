package com.proseg.msvcinventory.mapper;

import com.proseg.msvcinventory.dto.request.CampusRequestDto;
import com.proseg.msvcinventory.dto.response.CampusResponseDto;
import com.proseg.msvcinventory.entity.Campus;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CampusMapper {

    Campus toEntity(CampusRequestDto request);

    CampusResponseDto toResponse(Campus campus);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(CampusRequestDto request, @MappingTarget Campus campus);
}
