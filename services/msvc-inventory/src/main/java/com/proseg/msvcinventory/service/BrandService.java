package com.proseg.msvcinventory.service;

import com.proseg.msvcinventory.dto.request.BrandRequestDto;
import com.proseg.msvcinventory.dto.response.BrandResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface BrandService {

    BrandResponseDto create(BrandRequestDto request);

    BrandResponseDto findById(UUID id);

    Page<BrandResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    BrandResponseDto update(UUID id, BrandRequestDto request);

    void delete(UUID id);
}
