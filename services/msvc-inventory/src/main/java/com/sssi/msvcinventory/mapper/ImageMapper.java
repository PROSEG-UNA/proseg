package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.ImageRequestDto;
import com.sssi.msvcinventory.dto.response.ImageResponseDto;
import com.sssi.msvcinventory.entity.Image;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ImageMapper {

    @Mapping(target = "asset", ignore = true)
    Image toEntity(ImageRequestDto request);

    @Mapping(target = "assetId", source = "asset.id")
    ImageResponseDto toResponse(Image image);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "asset", ignore = true)
    void updateEntityFromRequest(ImageRequestDto request, @MappingTarget Image image);
}
