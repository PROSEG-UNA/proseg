package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.BrandRequestDto;
import com.sssi.msvcinventory.dto.response.BrandResponseDto;
import com.sssi.msvcinventory.entity.Brand;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    Brand toEntity(BrandRequestDto request);

    BrandResponseDto toResponse(Brand brand);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(BrandRequestDto request, @MappingTarget Brand brand);
}
