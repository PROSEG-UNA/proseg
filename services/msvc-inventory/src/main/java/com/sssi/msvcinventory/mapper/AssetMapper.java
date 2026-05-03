package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.AssetRequestDto;
import com.sssi.msvcinventory.dto.response.AssetResponseDto;
import com.sssi.msvcinventory.entity.Asset;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {AssetModelMapper.class, LocationMapper.class, NetworkInterfaceMapper.class})
public interface AssetMapper {

    @Mapping(target = "assetModel", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "networkInterface", ignore = true)
    Asset toEntity(AssetRequestDto request);

    @Mapping(target = "kind", constant = "GENERIC")
    AssetResponseDto toResponse(Asset asset);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "assetModel", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "networkInterface", ignore = true)
    void updateEntityFromRequest(AssetRequestDto request, @MappingTarget Asset asset);
}