package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.AssetModelRequestDto;
import com.sssi.msvcinventory.dto.response.AssetModelResponseDto;
import com.sssi.msvcinventory.entity.Model;
import com.sssi.msvcinventory.entity.AssetType;
import com.sssi.msvcinventory.entity.Brand;
import com.sssi.msvcinventory.exception.AssetModelException;
import com.sssi.msvcinventory.exception.AssetTypeException;
import com.sssi.msvcinventory.exception.BrandException;
import com.sssi.msvcinventory.mapper.AssetModelMapper;
import com.sssi.msvcinventory.repository.AssetModelRepository;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.repository.AssetTypeRepository;
import com.sssi.msvcinventory.repository.BrandRepository;
import com.sssi.msvcinventory.service.AssetModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetModelServiceImpl implements AssetModelService {

    private final AssetModelRepository assetModelRepository;
    private final BrandRepository brandRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final AssetRepository assetRepository;
    private final AssetModelMapper assetModelMapper;

    @Override
    @Transactional
    public AssetModelResponseDto create(AssetModelRequestDto request) {
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> BrandException.notFound(request.getBrandId().toString()));

        AssetType assetType = assetTypeRepository.findById(request.getAssetTypeId())
                .orElseThrow(() -> AssetTypeException.notFound(request.getAssetTypeId().toString()));

        if (assetModelRepository.existsByNameIgnoreCaseAndBrandId(request.getName(), request.getBrandId())) {
            throw AssetModelException.duplicateName(request.getName());
        }

        Model model = assetModelMapper.toEntity(request);
        model.setBrand(brand);
        model.setAssetType(assetType);

        return assetModelMapper.toResponse(assetModelRepository.save(model));
    }

    @Override
    @Transactional(readOnly = true)
    public AssetModelResponseDto findById(UUID id) {
        return assetModelRepository.findById(id)
                .map(assetModelMapper::toResponse)
                .orElseThrow(() -> AssetModelException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetModelResponseDto> findAll(Pageable pageable) {
        return assetModelRepository.findAll(pageable)
                .map(assetModelMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetModelResponseDto> findByBrandId(UUID brandId, Pageable pageable) {
        if (!brandRepository.existsById(brandId)) {
            throw BrandException.notFound(brandId.toString());
        }
        return assetModelRepository.findByBrandId(brandId, pageable)
                .map(assetModelMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetModelResponseDto> findByAssetTypeId(UUID assetTypeId, Pageable pageable) {
        if (!assetTypeRepository.existsById(assetTypeId)) {
            throw AssetTypeException.notFound(assetTypeId.toString());
        }
        return assetModelRepository.findByAssetTypeId(assetTypeId, pageable)
                .map(assetModelMapper::toResponse);
    }

    @Override
    @Transactional
    public AssetModelResponseDto update(UUID id, AssetModelRequestDto request) {
        Model model = assetModelRepository.findById(id)
                .orElseThrow(() -> AssetModelException.notFound(id.toString()));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> BrandException.notFound(request.getBrandId().toString()));

        AssetType assetType = assetTypeRepository.findById(request.getAssetTypeId())
                .orElseThrow(() -> AssetTypeException.notFound(request.getAssetTypeId().toString()));

        if (assetModelRepository.existsByNameIgnoreCaseAndBrandIdAndIdNot(
                request.getName(), request.getBrandId(), id)) {
            throw AssetModelException.duplicateName(request.getName());
        }

        assetModelMapper.updateEntityFromRequest(request, model);
        model.setBrand(brand);
        model.setAssetType(assetType);

        return assetModelMapper.toResponse(assetModelRepository.save(model));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Model model = assetModelRepository.findById(id)
                .orElseThrow(() -> AssetModelException.notFound(id.toString()));

        if (assetRepository.existsByAssetModelId(id)) {
            throw AssetModelException.inUse(id.toString());
        }

        assetModelRepository.delete(model);
    }
}
