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
import com.sssi.msvc_maintenance.entity.MaintenanceRegister;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatusTransitions;
import com.sssi.msvc_maintenance.event.MaintenanceRequestCreatedDomainEvent;
import com.sssi.msvc_maintenance.exception.CompanyException;
import com.sssi.msvc_maintenance.exception.MaintenanceRequestException;
import com.sssi.msvc_maintenance.mapper.MaintenanceAssetOptionMapper;
import com.sssi.msvc_maintenance.mapper.MaintenanceRequestMapper;
import com.sssi.msvc_maintenance.repository.CompanyRepository;
import com.sssi.msvc_maintenance.repository.MaintenanceEmailRepository;
import com.sssi.msvc_maintenance.repository.MaintenanceRegisterRepository;
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

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaintenanceRequestServiceImpl implements MaintenanceRequestService {

    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final MaintenanceRegisterRepository maintenanceRegisterRepository;
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
        maintenanceRequest.setResponsibleUserCompany(resolveResponsible(request.getResponsibleUserCompanyId(), technicians));
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

        String responsibleKeycloakId = saved.getResponsibleUserCompany() != null
                ? saved.getResponsibleUserCompany().getKeycloakUserId()
                : null;

        List<String> emails = saved.getEmails() == null
                ? List.of()
                : saved.getEmails().stream()
                        .map(MaintenanceEmail::getEmail)
                        .toList();

        eventPublisher.publishEvent(new MaintenanceRequestCreatedDomainEvent(
                saved.getId(),
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
                responsibleKeycloakId
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

        if (request.getStatus() != null && request.getStatus() != maintenanceRequest.getStatus()) {
            MaintenanceStatusTransitions.validateOrThrow(maintenanceRequest.getStatus(), request.getStatus());
        }

        maintenanceRequestMapper.updateEntityFromRequest(request, maintenanceRequest);
        maintenanceRequest.setCompany(company);
        maintenanceRequest.setCampusId(parseUuid(request.getCampusId(), "campusId"));
        maintenanceRequest.setBuildingId(request.getBuildingId());
        maintenanceRequest.setAssignedTechnicians(technicians);
        maintenanceRequest.setResponsibleUserCompany(resolveResponsible(request.getResponsibleUserCompanyId(), technicians));
        maintenanceRequest.setEmails(resolveEmails(request.getEmails()));

        if (maintenanceRequest.getStatus() != MaintenanceStatus.CANCELLED) {
            maintenanceRequest.setCancellationReason(null);
        }

        return maintenanceRequestMapper.toResponse(maintenanceRequestRepository.save(maintenanceRequest));
    }

    @Override
    @Transactional
    public MaintenanceRequestResponseDto accept(UUID id) {
        MaintenanceRequest maintenanceRequest = maintenanceRequestRepository.findById(id)
                .orElseThrow(MaintenanceRequestException::notFound);

        if (maintenanceRequest.getStatus() == MaintenanceStatus.CANCELLED) {
            throw MaintenanceRequestException.cannotAcceptCancelled();
        }

        MaintenanceStatusTransitions.validateOrThrow(maintenanceRequest.getStatus(), MaintenanceStatus.ACCEPTED);

        maintenanceRequest.setStatus(MaintenanceStatus.ACCEPTED);
        maintenanceRequest.setCancellationReason(null);
        syncRegisterStatus(id, MaintenanceStatus.ACCEPTED);

        return maintenanceRequestMapper.toResponse(maintenanceRequestRepository.save(maintenanceRequest));
    }

    @Override
    @Transactional
    public MaintenanceRequestResponseDto cancel(UUID id, String reason) {
        MaintenanceRequest maintenanceRequest = maintenanceRequestRepository.findById(id)
                .orElseThrow(MaintenanceRequestException::notFound);

        MaintenanceStatusTransitions.validateOrThrow(maintenanceRequest.getStatus(), MaintenanceStatus.CANCELLED);

        maintenanceRequest.setStatus(MaintenanceStatus.CANCELLED);
        maintenanceRequest.setCancellationReason(normalizeReason(reason));
        syncRegisterStatus(id, MaintenanceStatus.CANCELLED);

        return maintenanceRequestMapper.toResponse(maintenanceRequestRepository.save(maintenanceRequest));
    }

    private void syncRegisterStatus(UUID requestId, MaintenanceStatus status) {
        maintenanceRegisterRepository.findByMaintenanceRequestId(requestId)
                .ifPresent(register -> register.setStatus(status));
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason.trim();
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
            return new ArrayList<>();
        }
        List<UserCompany> found = new ArrayList<>(userCompanyRepository.findAllById(ids));
        boolean allBelongToCompany = found.stream()
                .allMatch(uc -> uc.getCompany().getId().equals(company.getId()));
        if (!allBelongToCompany) {
            throw new IllegalArgumentException("Todos los técnicos asignados deben pertenecer a la empresa de la solicitud");
        }
        return found;
    }

    private List<MaintenanceEmail> resolveEmails(List<String> rawEmails) {
        if (rawEmails == null || rawEmails.isEmpty()) {
            return new ArrayList<>();
        }
        return rawEmails.stream()
                .filter(email -> email != null && !email.isBlank())
                .map(String::trim)
                .distinct()
                .map(email -> maintenanceEmailRepository.findByEmail(email)
                        .orElseGet(() -> maintenanceEmailRepository.save(
                                MaintenanceEmail.builder().email(email).build())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private UserCompany resolveResponsible(UUID responsibleId, List<UserCompany> technicians) {
        if (responsibleId == null) {
            return null;
        }
        return technicians.stream()
                .filter(uc -> uc.getId().equals(responsibleId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El técnico responsable debe estar en la lista de técnicos asignados"));
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("El campo '" + fieldName + "' debe ser un UUID válido", ex);
        }
    }
}

