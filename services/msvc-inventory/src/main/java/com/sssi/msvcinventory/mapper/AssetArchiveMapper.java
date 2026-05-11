package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.AssetArchiveRequestDto;
import com.sssi.msvcinventory.entity.AssetArchive;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface AssetArchiveMapper {

    @Mapping(target = "asset", ignore = true)
    AssetArchive toEntity(AssetArchiveRequestDto request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "asset", ignore = true)
    void updateEntityFromRequest(AssetArchiveRequestDto request, @MappingTarget AssetArchive assetArchive);
}
