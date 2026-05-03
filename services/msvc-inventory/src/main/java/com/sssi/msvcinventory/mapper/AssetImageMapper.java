package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.AssetImageRequestDto;
import com.sssi.msvcinventory.dto.response.AssetImageResponseDto;
import com.sssi.msvcinventory.entity.AssetImage;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AssetImageMapper {

    @Mapping(target = "asset", ignore = true)
    AssetImage toEntity(AssetImageRequestDto request);

    @Mapping(target = "assetId", source = "asset.id")
    AssetImageResponseDto toResponse(AssetImage assetImage);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "asset", ignore = true)
    void updateEntityFromRequest(AssetImageRequestDto request, @MappingTarget AssetImage assetImage);
}
