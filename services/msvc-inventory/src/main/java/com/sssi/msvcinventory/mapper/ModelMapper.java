package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.ModelRequestDto;
import com.sssi.msvcinventory.dto.response.ModelResponseDto;
import com.sssi.msvcinventory.entity.Model;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {BrandMapper.class, TypeMapper.class})
public interface ModelMapper {

    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "type", ignore = true)
    Model toEntity(ModelRequestDto request);

    ModelResponseDto toResponse(Model model);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "type", ignore = true)
    void updateEntityFromRequest(ModelRequestDto request, @MappingTarget Model model);
}
