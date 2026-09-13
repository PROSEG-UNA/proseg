package com.proseg.msvc_forms.mapper;

import com.proseg.msvc_forms.dto.response.FormRecordResponseDto;
import com.proseg.msvc_forms.entity.FormRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Mapper(componentModel = "spring")
public interface FormRecordMapper {

    @Mapping(target = "formTypeId", source = "formType.id")
    @Mapping(target = "formTypeName", source = "formType.name")
    @Mapping(target = "formTypeCode", source = "formType.code")
    @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(formRecord.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toOffsetDateTime(formRecord.getUpdatedAt()))")
    FormRecordResponseDto toResponse(FormRecord formRecord);

    default OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.of("America/Costa_Rica")).toOffsetDateTime();
    }
}
