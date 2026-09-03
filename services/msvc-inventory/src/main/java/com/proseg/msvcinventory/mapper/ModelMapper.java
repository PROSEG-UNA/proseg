package com.proseg.msvcinventory.mapper;

import com.proseg.msvcinventory.dto.request.ModelRequestDto;
import com.proseg.msvcinventory.dto.response.ModelResponseDto;
import com.proseg.msvcinventory.entity.Model;
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
