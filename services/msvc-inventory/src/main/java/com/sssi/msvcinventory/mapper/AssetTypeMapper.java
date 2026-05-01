package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.AssetTypeRequestDto;
import com.sssi.msvcinventory.dto.response.AssetTypeResponseDto;
import com.sssi.msvcinventory.entity.AssetType;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AssetTypeMapper {

    AssetType toEntity(AssetTypeRequestDto request);

    AssetTypeResponseDto toResponse(AssetType assetType);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(AssetTypeRequestDto request, @MappingTarget AssetType assetType);
}
