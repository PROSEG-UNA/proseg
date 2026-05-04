package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.BrandRequestDto;
import com.sssi.msvcinventory.dto.response.BrandResponseDto;
import com.sssi.msvcinventory.entity.Brand;
import com.sssi.msvcinventory.exception.BrandException;
import com.sssi.msvcinventory.mapper.BrandMapper;
import com.sssi.msvcinventory.repository.ModelRepository;
import com.sssi.msvcinventory.repository.BrandRepository;
import com.sssi.msvcinventory.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ModelRepository modelRepository;
    private final BrandMapper brandMapper;

    @Override
    @Transactional
    public BrandResponseDto create(BrandRequestDto request) {

        if (brandRepository.existsByNameIgnoreCase(request.getName())) {
            throw BrandException.duplicateName(request.getName());
        }

        Brand brand = brandMapper.toEntity(request);
        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponseDto findById(UUID id) {
        return brandRepository.findById(id)
                .map(brandMapper::toResponse)
                .orElseThrow(() -> BrandException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BrandResponseDto> findAll(Pageable pageable) {
        return brandRepository.findAll(pageable)
                .map(brandMapper::toResponse);
    }

    @Override
    @Transactional
    public BrandResponseDto update(UUID id, BrandRequestDto request) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> BrandException.notFound(id.toString()));

        if (brandRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw BrandException.duplicateName(request.getName());
        }

        brandMapper.updateEntityFromRequest(request, brand);
        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> BrandException.notFound(id.toString()));

        if (modelRepository.existsByBrandId(id)) {
            throw BrandException.inUse(id.toString());
        }

        brandRepository.delete(brand);
    }
}