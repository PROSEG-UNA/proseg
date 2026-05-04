package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.TypeRequestDto;
import com.sssi.msvcinventory.dto.response.TypeResponseDto;
import com.sssi.msvcinventory.entity.Type;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TypeMapper {

    Type toEntity(TypeRequestDto request);

    TypeResponseDto toResponse(Type type);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(TypeRequestDto request, @MappingTarget Type type);
}
