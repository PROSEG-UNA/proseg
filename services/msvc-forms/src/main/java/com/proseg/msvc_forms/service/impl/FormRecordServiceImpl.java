package com.proseg.msvc_forms.service.impl;

import com.proseg.msvc_forms.dto.request.FormRecordCreateRequestDto;
import com.proseg.msvc_forms.dto.response.FormRecordResponseDto;
import com.proseg.msvc_forms.entity.FormRecord;
import com.proseg.msvc_forms.entity.FormType;
import com.proseg.msvc_forms.exception.FormRecordNotFoundException;
import com.proseg.msvc_forms.exception.FormTypeNotFoundException;
import com.proseg.msvc_forms.mapper.FormRecordMapper;
import com.proseg.msvc_forms.repository.FormRecordRepository;
import com.proseg.msvc_forms.repository.FormTypeRepository;
import com.proseg.msvc_forms.service.FormRecordService;
import com.proseg.msvc_forms.validator.FormValidatorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FormRecordServiceImpl implements FormRecordService {

    private final FormRecordRepository formRecordRepository;
    private final FormTypeRepository formTypeRepository;
    private final FormRecordMapper formRecordMapper;
    private final FormValidatorRegistry formValidatorRegistry;

    @Override
    @Transactional
    public FormRecordResponseDto create(FormRecordCreateRequestDto request, Authentication authentication) {
        FormType formType = formTypeRepository.findById(request.getFormTypeId())
            .filter(ft -> !ft.isDeleted() && ft.isActive())
            .orElseThrow(() -> new FormTypeNotFoundException("El tipo de formulario no existe o está inactivo"));

        formValidatorRegistry.validate(formType.getCode(), request.getData());

        FormRecord formRecord = FormRecord.builder()
            .formType(formType)
            .data(request.getData())
            .build();

        String userId = extractUserId(authentication);
        formRecord.setCreatedBy(userId);
        formRecord.setUpdatedBy(userId);

        FormRecord saved = formRecordRepository.save(formRecord);
        return formRecordMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FormRecordResponseDto getById(UUID id) {
        FormRecord formRecord = formRecordRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new FormRecordNotFoundException("El registro de formulario no existe"));
        return formRecordMapper.toResponse(formRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FormRecordResponseDto> findAll(UUID formTypeId, String createdBy, Pageable pageable) {
        return formRecordRepository.findWithFilters(formTypeId, createdBy, pageable)
            .map(formRecordMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FormRecordResponseDto> findByFormTypeCode(String formTypeCode, Pageable pageable) {
        return formRecordRepository.findByFormTypeCode(formTypeCode, pageable)
            .map(formRecordMapper::toResponse);
    }

    @Override
    @Transactional
    public FormRecordResponseDto update(UUID id, FormRecordCreateRequestDto request, Authentication authentication) {
        FormRecord formRecord = formRecordRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new FormRecordNotFoundException("El registro de formulario no existe"));

        FormType formType = formTypeRepository.findById(request.getFormTypeId())
            .filter(ft -> !ft.isDeleted() && ft.isActive())
            .orElseThrow(() -> new FormTypeNotFoundException("El tipo de formulario no existe o está inactivo"));

        formValidatorRegistry.validate(formType.getCode(), request.getData());

        formRecord.setFormType(formType);
        formRecord.setData(request.getData());
        formRecord.setUpdatedBy(extractUserId(authentication));

        FormRecord saved = formRecordRepository.save(formRecord);
        return formRecordMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        FormRecord formRecord = formRecordRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new FormRecordNotFoundException("El registro de formulario no existe"));
        
        formRecord.markAsDeleted();
        formRecordRepository.save(formRecord);
    }

    private String extractUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getSubject();
        }
        return authentication == null ? "" : authentication.getName();
    }
}
