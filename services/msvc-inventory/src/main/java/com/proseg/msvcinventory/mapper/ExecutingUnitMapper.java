package com.proseg.msvcinventory.mapper;

import com.proseg.msvcinventory.dto.request.ExecutingUnitRequestDto;
import com.proseg.msvcinventory.dto.response.ExecutingUnitResponseDto;
import com.proseg.msvcinventory.entity.ExecutingUnit;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ExecutingUnitMapper {

    ExecutingUnit toEntity(ExecutingUnitRequestDto request);

    ExecutingUnitResponseDto toResponse(ExecutingUnit executingUnit);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(ExecutingUnitRequestDto request, @MappingTarget ExecutingUnit executingUnit);
}
