package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.BrandRequestDto;
import com.sssi.msvcinventory.dto.response.BrandResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BrandService {

    BrandResponseDto create(BrandRequestDto request);

    BrandResponseDto findById(UUID id);

    Page<BrandResponseDto> findAll(Pageable pageable);

    BrandResponseDto update(UUID id, BrandRequestDto request);

    void delete(UUID id);
}
