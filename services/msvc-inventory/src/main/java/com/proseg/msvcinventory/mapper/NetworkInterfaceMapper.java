package com.proseg.msvcinventory.mapper;

import com.proseg.msvcinventory.dto.request.NetworkInterfaceEmbeddedRequestDto;
import com.proseg.msvcinventory.dto.request.NetworkInterfaceRequestDto;
import com.proseg.msvcinventory.dto.response.NetworkInterfaceResponseDto;
import com.proseg.msvcinventory.entity.NetworkInterface;
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