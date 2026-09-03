package com.proseg.msvcinventory.service.impl;

import com.proseg.msvcinventory.dto.request.ModelRequestDto;
import com.proseg.msvcinventory.dto.response.ModelResponseDto;
import com.proseg.msvcinventory.entity.Model;
import com.proseg.msvcinventory.entity.Type;
import com.proseg.msvcinventory.entity.Brand;
import com.proseg.msvcinventory.exception.ModelException;
import com.proseg.msvcinventory.exception.TypeException;
import com.proseg.msvcinventory.exception.BrandException;
import com.proseg.msvcinventory.mapper.ModelMapper;
import com.proseg.msvcinventory.repository.ModelRepository;
import com.proseg.msvcinventory.repository.AssetRepository;
import com.proseg.msvcinventory.repository.TypeRepository;
import com.proseg.msvcinventory.repository.BrandRepository;
import com.proseg.msvcinventory.service.ModelService;
import com.proseg.msvcinventory.specification.GenericSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final ModelRepository modelRepository;
    private final BrandRepository brandRepository;
    private final TypeRepository typeRepository;
    private final AssetRepository assetRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ModelResponseDto create(ModelRequestDto request) {
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> BrandException.notFound(request.getBrandId().toString()));

        Type type = typeRepository.findById(request.getTypeId())
                .orElseThrow(() -> TypeException.notFound(request.getTypeId().toString()));

        if (modelRepository.existsByNameIgnoreCaseAndBrandId(request.getName(), request.getBrandId())) {
            throw ModelException.duplicateName(request.getName());
        }

        Model model = modelMapper.toEntity(request);
        model.setBrand(brand);
        model.setType(type);

        return modelMapper.toResponse(modelRepository.save(model));
    }

    @Override
    @Transactional(readOnly = true)
    public ModelResponseDto findById(UUID id) {
        return modelRepository.findById(id)
                .map(modelMapper::toResponse)
                .orElseThrow(() -> ModelException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ModelResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<Model> spec = Specification
                .where(GenericSpecifications.<Model>withSearch(Model.class, search))
                .and(GenericSpecifications.<Model>withColumnFilters(Model.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(Model.class, pageable.getSort())
        );

        return modelRepository.findAll(spec, sanitized).map(modelMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ModelResponseDto> findByBrandId(UUID brandId, Pageable pageable) {
        if (!brandRepository.existsById(brandId)) {
            throw BrandException.notFound(brandId.toString());
        }
        return modelRepository.findByBrandId(brandId, pageable)
                .map(modelMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ModelResponseDto> findByTypeId(UUID typeId, Pageable pageable) {
        if (!typeRepository.existsById(typeId)) {
            throw TypeException.notFound(typeId.toString());
        }
        return modelRepository.findByTypeId(typeId, pageable)
                .map(modelMapper::toResponse);
    }

    @Override
    @Transactional
    public ModelResponseDto update(UUID id, ModelRequestDto request) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> ModelException.notFound(id.toString()));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> BrandException.notFound(request.getBrandId().toString()));

        Type type = typeRepository.findById(request.getTypeId())
                .orElseThrow(() -> TypeException.notFound(request.getTypeId().toString()));

        if (modelRepository.existsByNameIgnoreCaseAndBrandIdAndIdNot(
                request.getName(), request.getBrandId(), id)) {
            throw ModelException.duplicateName(request.getName());
        }

        modelMapper.updateEntityFromRequest(request, model);
        model.setBrand(brand);
        model.setType(type);

        return modelMapper.toResponse(modelRepository.save(model));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> ModelException.notFound(id.toString()));

        if (assetRepository.existsByModelId(id)) {
            throw ModelException.inUse(model.getName());
        }

        modelRepository.delete(model);
    }
}