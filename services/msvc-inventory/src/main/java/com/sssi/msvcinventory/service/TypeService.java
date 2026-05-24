package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.TypeRequestDto;
import com.sssi.msvcinventory.dto.response.TypeResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface TypeService {

    TypeResponseDto create(TypeRequestDto request);

    TypeResponseDto findById(UUID id);

    Page<TypeResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    TypeResponseDto update(UUID id, TypeRequestDto request);

    void delete(UUID id);
}
