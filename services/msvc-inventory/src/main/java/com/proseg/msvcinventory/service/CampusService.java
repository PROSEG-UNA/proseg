package com.proseg.msvcinventory.service;

import com.proseg.msvcinventory.dto.request.CampusRequestDto;
import com.proseg.msvcinventory.dto.response.CampusResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CampusService {

    CampusResponseDto create(CampusRequestDto request);

    CampusResponseDto findById(UUID id);

    Page<CampusResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    CampusResponseDto update(UUID id, CampusRequestDto request);

    void delete(UUID id);
}
