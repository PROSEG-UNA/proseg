package com.proseg.msvcinventory.mapper;

import com.proseg.msvcinventory.dto.request.BrandRequestDto;
import com.proseg.msvcinventory.dto.response.BrandResponseDto;
import com.proseg.msvcinventory.entity.Brand;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    Brand toEntity(BrandRequestDto request);

    BrandResponseDto toResponse(Brand brand);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(BrandRequestDto request, @MappingTarget Brand brand);
}
