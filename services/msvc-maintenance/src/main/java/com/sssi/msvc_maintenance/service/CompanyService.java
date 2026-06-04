package com.sssi.msvc_maintenance.service;

import com.sssi.msvc_maintenance.dto.request.CompanyRequestDto;
import com.sssi.msvc_maintenance.dto.request.CreateManagedUserRequestDto;
import com.sssi.msvc_maintenance.dto.response.CompanyResponseDto;
import com.sssi.msvc_maintenance.dto.response.CreateManagedUserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CompanyService {

    CompanyResponseDto create(CompanyRequestDto request);

    CreateManagedUserResponseDto createManagedUser(CreateManagedUserRequestDto request);

    CompanyResponseDto findById(UUID id);

    Page<CompanyResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    CompanyResponseDto update(UUID id, CompanyRequestDto request);

    void delete(UUID id);
}

