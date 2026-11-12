package com.proseg.msvc_forms.service.impl;

import com.proseg.msvc_forms.dto.response.FormTypeResponseDto;
import com.proseg.msvc_forms.exception.FormTypeNotFoundException;
import com.proseg.msvc_forms.mapper.FormTypeMapper;
import com.proseg.msvc_forms.repository.FormTypeRepository;
import com.proseg.msvc_forms.service.FormTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormTypeServiceImpl implements FormTypeService {

    private final FormTypeRepository formTypeRepository;
    private final FormTypeMapper formTypeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FormTypeResponseDto> getAllActiveTypes() {
        return formTypeRepository.findAllByActiveAndIsDeletedFalse(true)
            .stream()
            .map(formTypeMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FormTypeResponseDto getTypeById(UUID id) {
        return formTypeRepository.findById(id)
            .filter(ft -> !ft.isDeleted())
            .map(formTypeMapper::toResponse)
            .orElseThrow(() -> new FormTypeNotFoundException("El tipo de formulario no existe"));
    }

    @Override
    @Transactional(readOnly = true)
    public FormTypeResponseDto getTypeByCode(String code) {
        return formTypeRepository.findByCodeAndIsDeletedFalse(code)
            .map(formTypeMapper::toResponse)
            .orElseThrow(() -> new FormTypeNotFoundException("El tipo de formulario no existe: " + code));
    }
}
