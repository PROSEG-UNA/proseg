package com.proseg.msvc_forms.service;

import com.proseg.msvc_forms.dto.response.FormTypeResponseDto;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

public interface FormTypeService {

    List<FormTypeResponseDto> getAllActiveTypes();

    FormTypeResponseDto getTypeById(UUID id);

    FormTypeResponseDto getTypeByCode(String code);
}
