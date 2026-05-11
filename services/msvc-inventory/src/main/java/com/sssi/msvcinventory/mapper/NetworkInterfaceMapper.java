package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.NetworkInterfaceEmbeddedRequestDto;
import com.sssi.msvcinventory.dto.request.NetworkInterfaceRequestDto;
import com.sssi.msvcinventory.dto.response.NetworkInterfaceResponseDto;
import com.sssi.msvcinventory.entity.NetworkInterface;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface NetworkInterfaceMapper {

    @Mapping(target = "asset", ignore = true)
    NetworkInterface toEntity(NetworkInterfaceRequestDto request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "asset", ignore = true)
    NetworkInterface fromEmbedded(NetworkInterfaceEmbeddedRequestDto dto);

    @Mapping(target = "assetId", source = "asset.id")
    NetworkInterfaceResponseDto toResponse(NetworkInterface networkInterface);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "asset", ignore = true)
    void updateEntityFromRequest(NetworkInterfaceRequestDto request, @MappingTarget NetworkInterface networkInterface);
}