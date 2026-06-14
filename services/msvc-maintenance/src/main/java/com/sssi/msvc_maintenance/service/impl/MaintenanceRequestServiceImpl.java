package com.sssi.msvc_maintenance.service.impl;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.msvc_maintenance.client.InventoryClient;
import com.sssi.msvc_maintenance.dto.request.MaintenanceRequestRequestDto;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceAssetOptionDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.sssi.msvc_maintenance.entity.Company;
import com.sssi.msvc_maintenance.entity.MaintenanceEmail;
import com.sssi.msvc_maintenance.entity.MaintenanceRequest;
import com.sssi.msvc_maintenance.entity.UserCompany;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import com.sssi.msvc_maintenance.event.MaintenanceRequestCreatedDomainEvent;
import com.sssi.msvc_maintenance.exception.CompanyException;
import com.sssi.msvc_maintenance.exception.MaintenanceRequestException;
import com.sssi.msvc_maintenance.mapper.MaintenanceAssetOptionMapper;
import com.sssi.msvc_maintenance.mapper.MaintenanceRequestMapper;
import com.sssi.msvc_maintenance.repository.CompanyRepository;
import com.sssi.msvc_maintenance.repository.MaintenanceEmailRepository;
import com.sssi.msvc_maintenance.repository.MaintenanceRequestRepository;
import com.sssi.msvc_maintenance.repository.UserCompanyRepository;
import com.sssi.msvc_maintenance.security.Privileges;
import com.sssi.msvc_maintenance.service.MaintenanceRequestService;
import com.sssi.msvc_maintenance.specification.GenericSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceRequestServiceImpl implements MaintenanceRequestService {

    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final MaintenanceEmailRepository maintenanceEmailRepository;
    private final MaintenanceRequestMapper maintenanceRequestMapper;
    private final InventoryClient inventoryClient;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public MaintenanceRequestResponseDto create(MaintenanceRequestRequestDto request) {

        UUID companyId = parseUuid(request.getCompanyId(), "companyId");

        enforceCompanyForRequester(companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(CompanyException::notFound);

        List<UserCompany> technicians = resolveAssignedTechnicians(request.getAssignedTechnicianIds(), company);

        MaintenanceRequest maintenanceRequest = maintenanceRequestMapper.toEntity(request);
        maintenanceRequest.setCompany(company);
        maintenanceRequest.setStatus(MaintenanceStatus.PENDING);
        maintenanceRequest.setCampusId(parseUuid(request.getCampusId(), "campusId"));
        maintenanceRequest.setBuildingId(request.getBuildingId());
        maintenanceRequest.setAssignedTechnicians(technicians);
        maintenanceRequest.setLeaderUserCompany(resolveLeader(request.getLeaderUserCompanyId(), technicians));
        maintenanceRequest.setEmails(resolveEmails(request.getEmails()));

        MaintenanceRequest saved = maintenanceRequestRepository.save(maintenanceRequest);

        publishCreatedEvent(saved);

        return maintenanceRequestMapper.toResponse(saved);
    }

    private void publishCreatedEvent(MaintenanceRequest saved) {
        List<String> technicianKeycloakIds = saved.getAssignedTechnicians() == null
                ? List.of()
                : saved.getAssignedTechnicians().stream()
                        .map(UserCompany::getKeycloakUserId)
                        .toList();

        String leaderKeycloakId = saved.getLeaderUserCompany() != null
                ? saved.getLeaderUserCompany().getKeycloakUserId()
                : null;

        List<String> emails = saved.getEmails() == null
                ? List.of()
                : saved.getEmails().stream()
                        .map(MaintenanceEmail::getEmail)
                        .toList();

        eventPublisher.publishEvent(new MaintenanceRequestCreatedDomainEvent(
                emails,
                saved.getCompany().getName(),
                saved.getCompany().getLegalId(),
                saved.getDescription(),
                saved.getStatus(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getCampusId(),
                saved.getBuildingId(),
                technicianKeycloakIds,
                leaderKeycloakId
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceRequestResponseDto findById(UUID id) {
        return maintenanceRequestRepository.findById(id)
                .map(maintenanceRequestMapper::toResponse)
                .orElseThrow(MaintenanceRequestException::notFound);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceRequestResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<MaintenanceRequest> spec = Specification
                .where(GenericSpecifications.<MaintenanceRequest>withSearch(MaintenanceRequest.class, search))
                .and(GenericSpecifications.<MaintenanceRequest>withColumnFilters(MaintenanceRequest.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(MaintenanceRequest.class, pageable.getSort())
        );

        return maintenanceRequestRepository.findAll(spec, sanitized).map(maintenanceRequestMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceRequestResponseDto> findByCompanyId(UUID companyId, Pageable pageable) {

        if (!companyRepository.existsById(companyId)) {
            throw CompanyException.notFound();
        }

        return maintenanceRequestRepository.findByCompanyId(companyId, pageable)
                .map(maintenanceRequestMapper::toResponse);
    }

    @Override
    @Transactional
    public MaintenanceRequestResponseDto update(UUID id, MaintenanceRequestRequestDto request) {

        MaintenanceRequest maintenanceRequest = maintenanceRequestRepository.findById(id)
                .orElseThrow(MaintenanceRequestException::notFound);

        UUID companyId = parseUuid(request.getCompanyId(), "companyId");

        Company company = companyRepository.findById(companyId)
                .orElseThrow(CompanyException::notFound);

        List<UserCompany> technicians = resolveAssignedTechnicians(request.getAssignedTechnicianIds(), company);

        maintenanceRequestMapper.updateEntityFromRequest(request, maintenanceRequest);
        maintenanceRequest.setCompany(company);
        maintenanceRequest.setCampusId(parseUuid(request.getCampusId(), "campusId"));
        maintenanceRequest.setBuildingId(request.getBuildingId());
        maintenanceRequest.setAssignedTechnicians(technicians);
        maintenanceRequest.setLeaderUserCompany(resolveLeader(request.getLeaderUserCompanyId(), technicians));
        maintenanceRequest.setEmails(resolveEmails(request.getEmails()));

        return maintenanceRequestMapper.toResponse(maintenanceRequestRepository.save(maintenanceRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceAssetOptionDto> findAvailableAssets(String search, Pageable pageable) {
        List<String> sort = pageable.getSort().stream()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .toList();

        ApiResponse<PageResponse<InventoryAssetResponseDto>> response = inventoryClient.findAssets(
                search,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort.isEmpty() ? null : sort
        );

        PageResponse<InventoryAssetResponseDto> data = response != null ? response.getData() : null;
        if (data == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<MaintenanceAssetOptionDto> content = (data.getContent() == null ? List.<InventoryAssetResponseDto>of() : data.getContent())
                .stream()
                .map(MaintenanceAssetOptionMapper::toOption)
                .toList();

        return new PageImpl<>(content, pageable, data.getTotalElements());
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        MaintenanceRequest maintenanceRequest = maintenanceRequestRepository.findById(id)
                .orElseThrow(MaintenanceRequestException::notFound);

        maintenanceRequestRepository.delete(maintenanceRequest);
    }

    private void enforceCompanyForRequester(UUID companyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return;
        }

        boolean canSelectAnyCompany = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(Privileges.SolicitudesMantenimiento.SELECCIONAR_EMPRESA));
        if (canSelectAnyCompany) {
            return;
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw MaintenanceRequestException.companyNotAllowed();
        }

        UUID ownCompanyId = userCompanyRepository.findAllByKeycloakUserId(jwt.getSubject()).stream()
                .findFirst()
                .map(userCompany -> userCompany.getCompany().getId())
                .orElseThrow(CompanyException::noAssociatedCompany);

        if (!ownCompanyId.equals(companyId)) {
            throw MaintenanceRequestException.companyNotAllowed();
        }
    }

    private List<UserCompany> resolveAssignedTechnicians(List<UUID> ids, Company company) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<UserCompany> found = userCompanyRepository.findAllById(ids);
        boolean allBelongToCompany = found.stream()
                .allMatch(uc -> uc.getCompany().getId().equals(company.getId()));
        if (!allBelongToCompany) {
            throw new IllegalArgumentException("Todos los técnicos asignados deben pertenecer a la empresa de la solicitud");
        }
        return found;
    }

    private List<MaintenanceEmail> resolveEmails(List<String> rawEmails) {
        if (rawEmails == null || rawEmails.isEmpty()) {
            return List.of();
        }
        return rawEmails.stream()
                .filter(email -> email != null && !email.isBlank())
                .map(String::trim)
                .distinct()
                .map(email -> maintenanceEmailRepository.findByEmail(email)
                        .orElseGet(() -> maintenanceEmailRepository.save(
                                MaintenanceEmail.builder().email(email).build())))
                .toList();
    }

    private UserCompany resolveLeader(UUID leaderId, List<UserCompany> technicians) {
        if (leaderId == null) {
            return null;
        }
        return technicians.stream()
                .filter(uc -> uc.getId().equals(leaderId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El técnico encargado debe estar en la lista de técnicos asignados"));
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("El campo '" + fieldName + "' debe ser un UUID válido", ex);
        }
    }
}

