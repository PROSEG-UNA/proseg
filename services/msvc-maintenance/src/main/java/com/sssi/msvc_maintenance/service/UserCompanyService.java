package com.sssi.msvc_maintenance.service;

import com.sssi.msvc_maintenance.dto.request.UserCompanyRequestDto;
import com.sssi.msvc_maintenance.dto.response.UserCompanyResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface UserCompanyService {

    UserCompanyResponseDto create(UserCompanyRequestDto request);

    UserCompanyResponseDto findById(UUID id);

    Page<UserCompanyResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    Page<UserCompanyResponseDto> findByCompanyId(UUID companyId, Pageable pageable);

    UserCompanyResponseDto update(UUID id, UserCompanyRequestDto request);

    void delete(UUID id);
}

