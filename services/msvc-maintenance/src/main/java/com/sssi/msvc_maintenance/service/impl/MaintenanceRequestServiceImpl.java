package com.sssi.msvc_maintenance.service.impl;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.msvc_maintenance.client.InventoryClient;
import com.sssi.msvc_maintenance.dto.request.MaintenanceRequestRequestDto;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryCampusResponseDto;
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
import com.sssi.msvc_maintenance.event.MaintenanceRequestNotificationDomainEvent;
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
import lombok.extern.slf4j.Slf4j;
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
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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

        UUID campusId = parseUuid(request.getCampusId(), "campusId");

        MaintenanceRequest maintenanceRequest = maintenanceRequestMapper.toEntity(request);
        maintenanceRequest.setCompany(company);
        maintenanceRequest.setStatus(MaintenanceStatus.PENDING);
        maintenanceRequest.setCampusId(campusId);
        maintenanceRequest.setBuildingId(request.getBuildingId());
        maintenanceRequest.setAssignedTechnicians(technicians);
        maintenanceRequest.setResponsibleUserCompany(resolveResponsible(request.getResponsibleUserCompanyId(), technicians));
        maintenanceRequest.setEmails(resolveEmails(request.getEmails(), campusId, request.getBuildingId()));

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

        String oldDescription = maintenanceRequest.getDescription();
        MaintenanceStatus oldStatus = maintenanceRequest.getStatus();
        java.time.LocalDate oldStartDate = maintenanceRequest.getStartDate();
        java.time.LocalDate oldEndDate = maintenanceRequest.getEndDate();
        java.time.LocalTime oldStartTime = maintenanceRequest.getStartTime();
        java.time.LocalTime oldEndTime = maintenanceRequest.getEndTime();
        UUID oldCampusId = maintenanceRequest.getCampusId();
        UUID oldBuildingId = maintenanceRequest.getBuildingId();
        String oldCampusName = resolveCampusNameSafe(oldCampusId);
        String oldBuildingName = resolveBuildingNameSafe(oldBuildingId);
        Set<UUID> oldTechnicianIds = toTechnicianIds(maintenanceRequest.getAssignedTechnicians());
        UUID oldResponsibleId = maintenanceRequest.getResponsibleUserCompany() != null
                ? maintenanceRequest.getResponsibleUserCompany().getId()
                : null;
        String oldResponsibleName = resolveResponsibleName(maintenanceRequest.getResponsibleUserCompany());
        Set<String> oldEmails = toEmailSet(maintenanceRequest.getEmails());

        UUID companyId = parseUuid(request.getCompanyId(), "companyId");

        Company company = companyRepository.findById(companyId)
                .orElseThrow(CompanyException::notFound);

        List<UserCompany> technicians = resolveAssignedTechnicians(request.getAssignedTechnicianIds(), company);

        if (request.getStatus() != null && request.getStatus() != maintenanceRequest.getStatus()) {
            MaintenanceStatusTransitions.validateOrThrow(maintenanceRequest.getStatus(), request.getStatus());
        }

        UUID campusId = parseUuid(request.getCampusId(), "campusId");

        maintenanceRequestMapper.updateEntityFromRequest(request, maintenanceRequest);
        maintenanceRequest.setCompany(company);
        maintenanceRequest.setCampusId(campusId);
        maintenanceRequest.setBuildingId(request.getBuildingId());
        maintenanceRequest.setAssignedTechnicians(technicians);
        maintenanceRequest.setResponsibleUserCompany(resolveResponsible(request.getResponsibleUserCompanyId(), technicians));
        maintenanceRequest.setEmails(resolveEmails(request.getEmails(), campusId, request.getBuildingId()));

        if (maintenanceRequest.getStatus() != MaintenanceStatus.CANCELLED) {
            maintenanceRequest.setCancellationReason(null);
        }

        MaintenanceRequest saved = maintenanceRequestRepository.save(maintenanceRequest);

        // If transitioned now to CANCELLED and previously was not CANCELLED -> send rejection notification (once)
        if (oldStatus != MaintenanceStatus.CANCELLED && saved.getStatus() == MaintenanceStatus.CANCELLED) {
            publishMaintenanceNotification(
                    saved,
                    "REJECTED",
                    "Solicitud de mantenimiento rechazada",
                    "La solicitud fue rechazada por administracion",
                    List.of(
                            formatChange("Estado", toStatusLabel(oldStatus), toStatusLabel(saved.getStatus())),
                            "Motivo: " + valueOrFallback(saved.getCancellationReason())
                    )
            );
            return maintenanceRequestMapper.toResponse(saved);
        }

        // If it was already CANCELLED and remains CANCELLED, do not resend cancellation notification
        if (oldStatus == MaintenanceStatus.CANCELLED && saved.getStatus() == MaintenanceStatus.CANCELLED) {
            return maintenanceRequestMapper.toResponse(saved);
        }

        // Default: publish an UPDATED notification
        publishMaintenanceNotification(
                saved,
                "UPDATED",
                "Solicitud de mantenimiento actualizada",
                "Se actualizo la solicitud de mantenimiento",
                buildUpdateChangedFields(
                        oldDescription,
                        oldStatus,
                        oldStartDate,
                        oldEndDate,
                        oldStartTime,
                        oldEndTime,
                        oldCampusId,
                        oldBuildingId,
                        oldCampusName,
                        oldBuildingName,
                        oldTechnicianIds,
                        oldResponsibleId,
                        oldResponsibleName,
                        oldEmails,
                        saved
                )
        );

        return maintenanceRequestMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MaintenanceRequestResponseDto cancel(UUID id, String reason) {
        MaintenanceRequest maintenanceRequest = maintenanceRequestRepository.findById(id)
                .orElseThrow(MaintenanceRequestException::notFound);
        MaintenanceStatus previousStatus = maintenanceRequest.getStatus();

        MaintenanceStatusTransitions.validateOrThrow(maintenanceRequest.getStatus(), MaintenanceStatus.CANCELLED);

        maintenanceRequest.setStatus(MaintenanceStatus.CANCELLED);
        maintenanceRequest.setCancellationReason(normalizeReason(reason));
        syncRegisterStatus(id, MaintenanceStatus.CANCELLED);

        MaintenanceRequest saved = maintenanceRequestRepository.save(maintenanceRequest);

        publishMaintenanceNotification(
                saved,
                "REJECTED",
                "Solicitud de mantenimiento rechazada",
                "La solicitud fue rechazada por administracion",
                List.of(
                        formatChange("Estado", toStatusLabel(previousStatus), toStatusLabel(saved.getStatus())),
                        "Motivo: " + valueOrFallback(saved.getCancellationReason())
                )
        );

        return maintenanceRequestMapper.toResponse(saved);
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

    private void publishMaintenanceNotification(MaintenanceRequest request,
                                                String actionType,
                                                String actionLabel,
                                                String detail,
                                                List<String> changedFields) {
        eventPublisher.publishEvent(new MaintenanceRequestNotificationDomainEvent(
                request.getId(),
                actionType,
                actionLabel,
                detail,
                request.getCompany().getName(),
                request.getCompany().getLegalId(),
                request.getDescription(),
                request.getCancellationReason(),
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getStartDate(),
                request.getEndDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getCampusId(),
                request.getBuildingId(),
                changedFields == null ? List.of() : changedFields,
                resolveNotificationEmails(request),
                System.currentTimeMillis()
        ));
    }

    private List<String> buildUpdateChangedFields(String oldDescription,
                                                  MaintenanceStatus oldStatus,
                                                  java.time.LocalDate oldStartDate,
                                                  java.time.LocalDate oldEndDate,
                                                  java.time.LocalTime oldStartTime,
                                                  java.time.LocalTime oldEndTime,
                                                  UUID oldCampusId,
                                                  UUID oldBuildingId,
                                                  String oldCampusName,
                                                  String oldBuildingName,
                                                  Set<UUID> oldTechnicianIds,
                                                  UUID oldResponsibleId,
                                                  String oldResponsibleName,
                                                  Set<String> oldEmails,
                                                  MaintenanceRequest saved) {
        List<String> changedFields = new ArrayList<>();

        if (!Objects.equals(oldDescription, saved.getDescription())) {
            changedFields.add(formatChange("Descripcion", oldDescription, saved.getDescription()));
        }
        if (!Objects.equals(oldStatus, saved.getStatus())) {
            changedFields.add(formatChange("Estado", toStatusLabel(oldStatus), toStatusLabel(saved.getStatus())));
        }
        if (!Objects.equals(oldStartDate, saved.getStartDate())) {
            changedFields.add(formatChange("Fecha inicio", valueOrFallback(oldStartDate), valueOrFallback(saved.getStartDate())));
        }
        if (!Objects.equals(oldEndDate, saved.getEndDate())) {
            changedFields.add(formatChange("Fecha fin", valueOrFallback(oldEndDate), valueOrFallback(saved.getEndDate())));
        }
        if (!Objects.equals(oldStartTime, saved.getStartTime())) {
            changedFields.add(formatChange("Hora inicio", valueOrFallback(oldStartTime), valueOrFallback(saved.getStartTime())));
        }
        if (!Objects.equals(oldEndTime, saved.getEndTime())) {
            changedFields.add(formatChange("Hora fin", valueOrFallback(oldEndTime), valueOrFallback(saved.getEndTime())));
        }
        if (!Objects.equals(oldCampusId, saved.getCampusId())) {
            changedFields.add(formatChange("Campus", oldCampusName, resolveCampusNameSafe(saved.getCampusId())));
        }
        if (!Objects.equals(oldBuildingId, saved.getBuildingId())) {
            changedFields.add(formatChange("Edificio", oldBuildingName, resolveBuildingNameSafe(saved.getBuildingId())));
        }

        Set<UUID> newTechnicianIds = toTechnicianIds(saved.getAssignedTechnicians());
        if (!Objects.equals(oldTechnicianIds, newTechnicianIds)) {
            changedFields.add("Tecnicos asignados actualizados");
        }

        UUID newResponsibleId = saved.getResponsibleUserCompany() != null
                ? saved.getResponsibleUserCompany().getId()
                : null;
        if (!Objects.equals(oldResponsibleId, newResponsibleId)) {
            changedFields.add(formatChange("Responsable", oldResponsibleName, resolveResponsibleName(saved.getResponsibleUserCompany())));
        }

        Set<String> newEmails = toEmailSet(saved.getEmails());
        if (!Objects.equals(oldEmails, newEmails)) {
            changedFields.add("Correos asociados actualizados");
        }

        return changedFields;
    }

    private List<String> resolveNotificationEmails(MaintenanceRequest request) {
        List<String> recipients = new ArrayList<>();
        recipients.addAll(resolveRequestEmails(request));
        recipients.addAll(resolveLocationEmails(request.getCampusId(), request.getBuildingId()));
        return cleanEmails(recipients);
    }

    private List<String> resolveRequestEmails(MaintenanceRequest request) {
        if (request.getEmails() == null) {
            return List.of();
        }
        return request.getEmails().stream()
                .map(MaintenanceEmail::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .toList();
    }

    private List<String> resolveLocationEmails(UUID campusId, UUID buildingId) {
        List<String> emails = new ArrayList<>();

        try {
            if (campusId != null) {
                ApiResponse<List<com.sssi.msvc_maintenance.dto.response.InventoryBuildingEmailResponseDto>> response =
                        inventoryClient.findCampusEmails(campusId);
                emails.addAll(extractLocationEmails(response));
            }
        } catch (Exception exception) {
            log.warn("No se pudieron resolver correos del campus {}: {}", campusId, exception.getMessage());
        }

        try {
            if (buildingId != null) {
                ApiResponse<List<com.sssi.msvc_maintenance.dto.response.InventoryBuildingEmailResponseDto>> response =
                        inventoryClient.findBuildingEmails(buildingId);
                emails.addAll(extractLocationEmails(response));
            }
        } catch (Exception exception) {
            log.warn("No se pudieron resolver correos del edificio {}: {}", buildingId, exception.getMessage());
        }

        return cleanEmails(emails);
    }

    private List<String> extractLocationEmails(ApiResponse<List<com.sssi.msvc_maintenance.dto.response.InventoryBuildingEmailResponseDto>> response) {
        List<com.sssi.msvc_maintenance.dto.response.InventoryBuildingEmailResponseDto> data =
                response != null ? response.getData() : null;
        if (data == null) {
            return List.of();
        }
        return data.stream()
                .map(com.sssi.msvc_maintenance.dto.response.InventoryBuildingEmailResponseDto::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .toList();
    }

    private List<String> cleanEmails(List<String> emails) {
        if (emails == null) {
            return List.of();
        }
        return emails.stream()
                .filter(email -> email != null && !email.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String resolveCampusNameSafe(UUID campusId) {
        if (campusId == null) {
            return null;
        }
        try {
            ApiResponse<InventoryCampusResponseDto> response = inventoryClient.findCampusById(campusId);
            InventoryCampusResponseDto campus = response != null ? response.getData() : null;
            return campus != null ? campus.getName() : "Recinto no disponible";
        } catch (Exception exception) {
            return "Recinto no disponible";
        }
    }

    private String resolveBuildingNameSafe(UUID buildingId) {
        if (buildingId == null) {
            return null;
        }
        try {
            ApiResponse<InventoryBuildingResponseDto> response = inventoryClient.findBuildingById(buildingId);
            InventoryBuildingResponseDto building = response != null ? response.getData() : null;
            return building != null ? building.getName() : "Edificio no disponible";
        } catch (Exception exception) {
            return "Edificio no disponible";
        }
    }

    private String resolveResponsibleName(UserCompany responsible) {
        if (responsible == null) {
            return "Sin responsable";
        }
        if (responsible.getUserEmail() != null && !responsible.getUserEmail().isBlank()) {
            return responsible.getUserEmail();
        }
        return "Responsable asignado";
    }

    private Set<UUID> toTechnicianIds(List<UserCompany> technicians) {
        if (technicians == null) {
            return Set.of();
        }
        return technicians.stream()
                .map(UserCompany::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Set<String> toEmailSet(List<MaintenanceEmail> emails) {
        if (emails == null) {
            return Set.of();
        }
        return emails.stream()
                .map(MaintenanceEmail::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private String toStatusLabel(MaintenanceStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "Pendiente";
            case COMPLETED -> "Completada";
            case CANCELLED -> "Cancelada";
        };
    }

    private String formatChange(String fieldName, String oldValue, String newValue) {
        return fieldName + ": " + valueOrFallback(oldValue) + " -> " + valueOrFallback(newValue);
    }

    private String valueOrFallback(Object value) {
        if (value == null) {
            return "Sin valor";
        }
        String text = value.toString();
        return text.isBlank() ? "Sin valor" : text;
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

    private List<MaintenanceEmail> resolveEmails(List<String> rawEmails, UUID campusId, UUID buildingId) {
        List<String> requestedEmails = cleanEmails(rawEmails);
        if (requestedEmails.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, String> registeredEmails = fetchRegisteredEmails(campusId, buildingId);

        return requestedEmails.stream()
                .map(email -> requireRegisteredEmail(email, registeredEmails))
                .distinct()
                .map(this::findOrCreateEmail)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String requireRegisteredEmail(String email, Map<String, String> registeredEmails) {
        String registeredEmail = registeredEmails.get(normalizeEmail(email));
        if (registeredEmail == null) {
            throw MaintenanceRequestException.emailNotRegistered(email);
        }
        return registeredEmail;
    }

    private Map<String, String> fetchRegisteredEmails(UUID campusId, UUID buildingId) {
        List<String> locationEmails = new ArrayList<>();
        try {
            if (campusId != null) {
                locationEmails.addAll(extractLocationEmails(inventoryClient.findCampusEmails(campusId)));
            }
            if (buildingId != null) {
                locationEmails.addAll(extractLocationEmails(inventoryClient.findBuildingEmails(buildingId)));
            }
        } catch (Exception exception) {
            log.warn("No se pudieron obtener los correos registrados del campus {} y edificio {}: {}",
                    campusId, buildingId, exception.getMessage());
            throw MaintenanceRequestException.registeredEmailsUnavailable();
        }

        return cleanEmails(locationEmails).stream()
                .collect(Collectors.toMap(this::normalizeEmail, email -> email, (existing, duplicate) -> existing));
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private MaintenanceEmail findOrCreateEmail(String email) {
        return maintenanceEmailRepository.findByEmail(email)
                .orElseGet(() -> maintenanceEmailRepository.save(
                        MaintenanceEmail.builder().email(email).build()));
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
