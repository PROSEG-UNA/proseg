package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.SiteRequestDto;
import com.sssi.msvcinventory.dto.response.SiteResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface SiteService {

    SiteResponseDto create(SiteRequestDto request);

    SiteResponseDto findById(UUID id);

    Page<SiteResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    SiteResponseDto update(UUID id, SiteRequestDto request);

    void delete(UUID id);
}
