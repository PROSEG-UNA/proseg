package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.SiteRequestDto;
import com.sssi.msvcinventory.dto.response.SiteResponseDto;
import com.sssi.msvcinventory.entity.Site;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SiteMapper {

    Site toEntity(SiteRequestDto request);

    SiteResponseDto toResponse(Site site);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(SiteRequestDto request, @MappingTarget Site site);
}
