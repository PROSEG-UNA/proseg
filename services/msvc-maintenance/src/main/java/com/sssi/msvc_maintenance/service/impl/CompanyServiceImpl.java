package com.sssi.msvc_maintenance.service.impl;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.msvc_maintenance.client.AuthClient;
import com.sssi.msvc_maintenance.dto.request.CompanyRequestDto;
import com.sssi.msvc_maintenance.dto.response.CompanyResponseDto;
import com.sssi.msvc_maintenance.dto.response.KeycloakUserDto;
import com.sssi.msvc_maintenance.entity.Company;
import com.sssi.msvc_maintenance.exception.CompanyException;
import com.sssi.msvc_maintenance.mapper.CompanyMapper;
import com.sssi.msvc_maintenance.repository.CompanyRepository;
import com.sssi.msvc_maintenance.repository.MaintenanceRequestRepository;
import com.sssi.msvc_maintenance.repository.UserCompanyRepository;
import com.sssi.msvc_maintenance.service.CompanyService;
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
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final CompanyMapper companyMapper;
    private final CompanyUserManagementService companyUserManagementService;
    private final AuthClient authClient;

    @Override
    @Transactional
    public CompanyResponseDto create(CompanyRequestDto request) {
        if (companyRepository.existsByLegalIdIgnoreCase(request.getLegalId())) {
            throw CompanyException.duplicateLegalId(request.getLegalId());
        }
        if (companyRepository.existsByNameIgnoreCase(request.getName())) {
            throw CompanyException.duplicateName(request.getName());
        }
        validateKeycloakUsers(request);
        Company company = companyMapper.toEntity(request);
        Company savedCompany = companyRepository.save(company);
        savedCompany.setUserCompanies(
                companyUserManagementService.syncUsers(
                        savedCompany,
                        request.getKeycloakUserIds()
                )
        );
        return companyMapper.toResponse(savedCompany);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponseDto findById(UUID id) {
        return companyRepository.findById(id)
                .map(companyMapper::toResponse)
                .orElseThrow(CompanyException::notFound);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<Company> spec = Specification
                .where(GenericSpecifications.<Company>withSearch(Company.class, search))
                .and(GenericSpecifications.<Company>withColumnFilters(Company.class, filters));
        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(Company.class, pageable.getSort())
        );
        return companyRepository.findAll(spec, sanitized).map(companyMapper::toResponse);
    }

    @Override
    @Transactional
    public CompanyResponseDto update(UUID id, CompanyRequestDto request) {
        Company company = companyRepository.findById(id)
                .orElseThrow(CompanyException::notFound);
        if (companyRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw CompanyException.duplicateName(request.getName());
        }
        if (companyRepository.existsByLegalIdIgnoreCaseAndIdNot(request.getLegalId(), id)) {
            throw CompanyException.duplicateLegalId(request.getLegalId());
        }
        companyMapper.updateEntityFromRequest(request, company);
        Company savedCompany = companyRepository.save(company);
        savedCompany.setUserCompanies(companyUserManagementService.syncUsers(savedCompany, request.getKeycloakUserIds()));
        return companyMapper.toResponse(savedCompany);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(CompanyException::notFound);
        if (userCompanyRepository.existsByCompanyId(id) || maintenanceRequestRepository.existsByCompanyId(id)) {
            throw CompanyException.inUse(company.getName());
        }
        companyRepository.delete(company);
    }

    private void validateKeycloakUsers(CompanyRequestDto request) {
        if (request.getKeycloakUserIds() == null || request.getKeycloakUserIds().isEmpty()) {
            return;
        }
        for (String userId : request.getKeycloakUserIds()) {
            try {
                ApiResponse<KeycloakUserDto> response =
                        authClient.getUserById(userId);
                if (response == null || response.getData() == null) {
                    throw CompanyException.invalidKeycloakUser(userId);
                }
            } catch (Exception ex) {
                throw CompanyException.invalidKeycloakUser(userId);
            }
        }
    }
}

