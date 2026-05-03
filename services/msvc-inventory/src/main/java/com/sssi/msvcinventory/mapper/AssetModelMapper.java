package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.AssetModelRequestDto;
import com.sssi.msvcinventory.dto.response.AssetModelResponseDto;
import com.sssi.msvcinventory.entity.AssetModel;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {BrandMapper.class, AssetTypeMapper.class})
public interface AssetModelMapper {

    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "assetType", ignore = true)
    AssetModel toEntity(AssetModelRequestDto request);

    AssetModelResponseDto toResponse(AssetModel assetModel);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "assetType", ignore = true)
    void updateEntityFromRequest(AssetModelRequestDto request, @MappingTarget AssetModel assetModel);
}
