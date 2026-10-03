package com.proseg.msvc_forms.mapper;

import com.proseg.msvc_forms.dto.response.FormTypeResponseDto;
import com.proseg.msvc_forms.entity.FormType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FormTypeMapper {

    FormTypeResponseDto toResponse(FormType formType);
}
