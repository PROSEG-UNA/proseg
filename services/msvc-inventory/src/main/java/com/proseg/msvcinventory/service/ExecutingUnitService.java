package com.proseg.msvcinventory.service;

import com.proseg.msvcinventory.dto.request.ExecutingUnitRequestDto;
import com.proseg.msvcinventory.dto.response.ExecutingUnitResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface ExecutingUnitService {

    ExecutingUnitResponseDto create(ExecutingUnitRequestDto request);

    ExecutingUnitResponseDto findById(UUID id);

    Page<ExecutingUnitResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    ExecutingUnitResponseDto update(UUID id, ExecutingUnitRequestDto request);

    void delete(UUID id);
}
