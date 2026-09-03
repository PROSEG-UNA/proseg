package com.proseg.msvcinventory.mapper;

import com.proseg.msvcinventory.dto.request.TypeRequestDto;
import com.proseg.msvcinventory.dto.response.TypeResponseDto;
import com.proseg.msvcinventory.entity.Type;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TypeMapper {

    Type toEntity(TypeRequestDto request);

    TypeResponseDto toResponse(Type type);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(TypeRequestDto request, @MappingTarget Type type);
}
