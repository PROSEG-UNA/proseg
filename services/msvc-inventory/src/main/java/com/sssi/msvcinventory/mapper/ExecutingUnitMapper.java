package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.ExecutingUnitRequestDto;
import com.sssi.msvcinventory.dto.response.ExecutingUnitResponseDto;
import com.sssi.msvcinventory.entity.ExecutingUnit;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ExecutingUnitMapper {

    ExecutingUnit toEntity(ExecutingUnitRequestDto request);

    ExecutingUnitResponseDto toResponse(ExecutingUnit executingUnit);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(ExecutingUnitRequestDto request, @MappingTarget ExecutingUnit executingUnit);
}
