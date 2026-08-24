package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.EmployeeRequestDto;
import com.sssi.msvcinventory.dto.response.EmployeeResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface EmployeeService {

    EmployeeResponseDto create(EmployeeRequestDto request);

    EmployeeResponseDto findById(UUID id);

    Page<EmployeeResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    EmployeeResponseDto update(UUID id, EmployeeRequestDto request);

    void delete(UUID id);
}
