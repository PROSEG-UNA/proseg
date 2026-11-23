package com.proseg.msvc_forms.service;

import com.proseg.msvc_forms.dto.request.FormRecordCreateRequestDto;
import com.proseg.msvc_forms.dto.response.FormRecordResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface FormRecordService {

    FormRecordResponseDto create(FormRecordCreateRequestDto request, Authentication authentication);

    FormRecordResponseDto getById(UUID id);

    Page<FormRecordResponseDto> findAll(UUID formTypeId, String createdBy, Pageable pageable, Authentication authentication);

    Page<FormRecordResponseDto> findByFormTypeCode(String formTypeCode, Pageable pageable);

    FormRecordResponseDto update(UUID id, FormRecordCreateRequestDto request, Authentication authentication);

    void delete(UUID id);
}
