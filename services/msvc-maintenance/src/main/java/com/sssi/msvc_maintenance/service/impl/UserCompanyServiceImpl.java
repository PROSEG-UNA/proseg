package com.sssi.msvc_maintenance.service.impl;

import com.sssi.msvc_maintenance.dto.request.UserCompanyRequestDto;
import com.sssi.msvc_maintenance.dto.response.UserCompanyResponseDto;
import com.sssi.msvc_maintenance.entity.Company;
import com.sssi.msvc_maintenance.entity.UserCompany;
import com.sssi.msvc_maintenance.exception.CompanyException;
import com.sssi.msvc_maintenance.exception.UserCompanyException;
import com.sssi.msvc_maintenance.mapper.UserCompanyMapper;
import com.sssi.msvc_maintenance.repository.CompanyRepository;
import com.sssi.msvc_maintenance.repository.UserCompanyRepository;
import com.sssi.msvc_maintenance.service.UserCompanyService;
import com.sssi.msvc_maintenance.specification.GenericSpecifications;
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
public class UserCompanyServiceImpl implements UserCompanyService {

    private final UserCompanyRepository userCompanyRepository;
    private final CompanyRepository companyRepository;
    private final UserCompanyMapper userCompanyMapper;

    @Override
    @Transactional
    public UserCompanyResponseDto create(UserCompanyRequestDto request) {

        UUID companyId = parseUuid(request.getCompanyId(), "companyId");

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> CompanyException.notFound(companyId.toString()));

        if (userCompanyRepository.existsByKeycloakUserIdAndCompanyId(request.getKeycloakUserId(), companyId)) {
            throw UserCompanyException.duplicateRelation(request.getKeycloakUserId(), request.getCompanyId());
        }

        UserCompany userCompany = userCompanyMapper.toEntity(request);
        userCompany.setCompany(company);

        return userCompanyMapper.toResponse(userCompanyRepository.save(userCompany));
    }

    @Override
    @Transactional(readOnly = true)
    public UserCompanyResponseDto findById(UUID id) {
        return userCompanyRepository.findById(id)
                .map(userCompanyMapper::toResponse)
                .orElseThrow(() -> UserCompanyException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserCompanyResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<UserCompany> spec = Specification
                .where(GenericSpecifications.<UserCompany>withSearch(UserCompany.class, search))
                .and(GenericSpecifications.<UserCompany>withColumnFilters(UserCompany.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(UserCompany.class, pageable.getSort())
        );

        return userCompanyRepository.findAll(spec, sanitized).map(userCompanyMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserCompanyResponseDto> findByCompanyId(UUID companyId, Pageable pageable) {

        if (!companyRepository.existsById(companyId)) {
            throw CompanyException.notFound(companyId.toString());
        }

        return userCompanyRepository.findByCompanyId(companyId, pageable)
                .map(userCompanyMapper::toResponse);
    }

    @Override
    @Transactional
    public UserCompanyResponseDto update(UUID id, UserCompanyRequestDto request) {

        UserCompany userCompany = userCompanyRepository.findById(id)
                .orElseThrow(() -> UserCompanyException.notFound(id.toString()));

        UUID companyId = parseUuid(request.getCompanyId(), "companyId");

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> CompanyException.notFound(companyId.toString()));

        if (userCompanyRepository.existsByKeycloakUserIdAndCompanyIdAndIdNot(request.getKeycloakUserId(), companyId, id)) {
            throw UserCompanyException.duplicateRelation(request.getKeycloakUserId(), request.getCompanyId());
        }

        userCompanyMapper.updateEntityFromRequest(request, userCompany);
        userCompany.setCompany(company);

        return userCompanyMapper.toResponse(userCompanyRepository.save(userCompany));
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        UserCompany userCompany = userCompanyRepository.findById(id)
                .orElseThrow(() -> UserCompanyException.notFound(id.toString()));

        userCompanyRepository.delete(userCompany);
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("El campo '" + fieldName + "' debe ser un UUID válido");
        }
    }
}

