package com.proseg.msvcinventory.mapper;

import com.proseg.msvcinventory.dto.request.AssetComponentRequestDto;
import com.proseg.msvcinventory.dto.response.AssetComponentResponseDto;
import com.proseg.msvcinventory.entity.AssetComponent;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AssetComponentMapper {

    @Mapping(target = "asset", ignore = true)
    AssetComponent toEntity(AssetComponentRequestDto request);

    @Mapping(target = "assetId", source = "asset.id")
    AssetComponentResponseDto toResponse(AssetComponent component);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "asset", ignore = true)
    void updateEntityFromRequest(AssetComponentRequestDto request, @MappingTarget AssetComponent component);
}

