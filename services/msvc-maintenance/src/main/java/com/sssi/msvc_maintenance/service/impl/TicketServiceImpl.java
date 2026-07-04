package com.sssi.msvc_maintenance.service.impl;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.ArchiveUploadInitResponseDto;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.response.PagedResponse;
import com.sssi.common.api.response.PresignedUrlResponseDto;
import com.sssi.common.api.exception.AssetException;
import com.sssi.common.api.exception.BuildingException;
import com.sssi.common.api.exception.FloorException;
import com.sssi.common.api.exception.LocationException;
import com.sssi.common.api.exception.CampusException;
import com.sssi.msvc_maintenance.client.AuthClient;
import com.sssi.msvc_maintenance.client.InventoryClient;
import com.sssi.msvc_maintenance.config.MaintenanceNotificationProperties;
import com.sssi.msvc_maintenance.dto.request.TicketAssignedToUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCommentCreateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCommentUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCreateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketPriorityUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketStatusUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetFloorResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetLocationResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryCampusResponseDto;
import com.sssi.msvc_maintenance.dto.response.KeycloakUserResponse;
import com.sssi.msvc_maintenance.dto.response.TicketAssetResponseDto;
import com.sssi.msvc_maintenance.dto.response.TicketCommentResponseDto;
import com.sssi.msvc_maintenance.dto.response.TicketListResponseDto;
import com.sssi.msvc_maintenance.dto.response.TicketPhotoResponseDto;
import com.sssi.msvc_maintenance.dto.response.TicketResponseDto;
import com.sssi.msvc_maintenance.exception.TicketException;
import com.sssi.msvc_maintenance.entity.Ticket;
import com.sssi.msvc_maintenance.entity.TicketAsset;
import com.sssi.msvc_maintenance.entity.TicketComment;
import com.sssi.msvc_maintenance.entity.TicketPhoto;
import com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType;
import com.sssi.msvc_maintenance.entity.enums.TicketPriority;
import com.sssi.msvc_maintenance.entity.enums.TicketStatus;
import com.sssi.msvc_maintenance.event.TicketNotificationDomainEvent;
import com.sssi.msvc_maintenance.repository.TicketAssetRepository;
import com.sssi.msvc_maintenance.repository.TicketCommentRepository;
import com.sssi.msvc_maintenance.repository.TicketPhotoRepository;
import com.sssi.msvc_maintenance.repository.TicketRepository;
import com.sssi.msvc_maintenance.security.Privileges;
import com.sssi.msvc_maintenance.service.TicketService;
import com.sssi.msvc_maintenance.websocket.TicketWebSocketEventDto;
import com.sssi.msvc_maintenance.websocket.TicketWebSocketManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final ZoneId TICKET_TIME_ZONE = ZoneId.of("America/Costa_Rica");
    private static final int ASSIGNEE_PAGE_SIZE = 200;

    private final TicketRepository ticketRepository;
    private final TicketAssetRepository ticketAssetRepository;
    private final TicketPhotoRepository ticketPhotoRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final com.sssi.msvc_maintenance.repository.TicketHistoryChangeRepository ticketHistoryChangeRepository;
    private final InventoryClient inventoryClient;
    private final RestTemplate restTemplate;
    private final TicketWebSocketManager webSocketManager;
    private final AuthClient authClient;
    private final MaintenanceNotificationProperties notificationProperties;
    private final ApplicationEventPublisher eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);

    private static final int ARCHIVE_CHUNK_SIZE_BYTES = 5 * 1024 * 1024;

    @org.springframework.beans.factory.annotation.Value("${GATEWAY_BASE_URL:http://localhost:8081}")
    private String archiveBaseUrl;

    @Override
    @Transactional
    public TicketResponseDto create(TicketCreateRequestDto request, List<MultipartFile> photos,
                                    Authentication authentication) {
        InventoryCampusResponseDto site = null;
        if (request.getSiteId() != null) {
            site = requireCampus(request.getSiteId());
        }

        InventoryBuildingResponseDto building = null;
        if (request.getBuildingId() != null) {
            building = requireBuilding(request.getBuildingId());

            if (site == null) {
                throw new BuildingException(
                        HttpStatus.BAD_REQUEST,
                        "BUILDING_CAMPUS_REQUIRED",
                        "Debe seleccionar un recinto para indicar un edificio");
            }

            if (!belongsToCampus(building.getId(), site.getId())) {
                throw BuildingException.campusDoesNotMatch(building.getId(), site.getId());
            }
        }

        InventoryAssetFloorResponseDto floor = null;
        if (request.getFloorId() != null) {
            floor = requireFloor(request.getFloorId());
            if (building == null) {
                throw new FloorException(
                        HttpStatus.BAD_REQUEST,
                        "FLOOR_BUILDING_REQUIRED",
                        "Debe seleccionar un edificio para indicar un piso");
            }
            if (floor.getBuilding() == null || !floor.getBuilding().getId().equals(building.getId())) {
                throw new FloorException(
                        HttpStatus.BAD_REQUEST,
                        "FLOOR_BUILDING_MISMATCH",
                        "El piso " + floor.getName() + " no pertenece al edificio indicado");
            }
        }

        InventoryAssetLocationResponseDto location = null;
        if (request.getLocationId() != null) {
            location = requireLocation(request.getLocationId());
            if (location.getFloor() == null || location.getFloor().getId() == null) {
                throw new LocationException(
                        HttpStatus.BAD_REQUEST,
                        "LOCATION_FLOOR_REQUIRED",
                        "La ubicación indicada no tiene un piso asociado");
            }

            if (floor == null) {
                floor = requireFloor(location.getFloor().getId());
            }

            if (building == null || floor.getBuilding() == null || !floor.getBuilding().getId().equals(building.getId())) {
                throw new LocationException(
                        HttpStatus.BAD_REQUEST,
                        "LOCATION_BUILDING_MISMATCH",
                        "La ubicación " + location.getId() + " no pertenece al edificio indicado");
            }

            if (!location.getFloor().getId().equals(floor.getId())) {
                throw new LocationException(
                        HttpStatus.BAD_REQUEST,
                        "LOCATION_FLOOR_MISMATCH",
                        "La ubicación " + location.getId() + " no pertenece al piso indicado");
            }
        }

        List<UUID> assetIds = resolveValidAssetIds(request.getAssetIds(), location);

        String createdBy = extractUserId(authentication);

        TicketPriority effectivePriority = Optional.ofNullable(request.getPriority())
                .filter(priority -> isAdmin(authentication) || hasSetPriorityPermission(authentication))
                .orElse(TicketPriority.LOW);

        Ticket ticket = Ticket.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .priority(effectivePriority)
                .status(TicketStatus.OPEN)
                .siteId(site != null ? site.getId() : null)
                .buildingId(building != null ? building.getId() : null)
                .floorId(floor != null ? floor.getId() : null)
                .locationId(location != null ? location.getId() : null)
                .build();

        ticket.setCreatedBy(createdBy);
        ticket.setUpdatedBy(createdBy);

        Ticket saved = ticketRepository.save(ticket);

        saveTicketAssets(saved, assetIds);
        saveTicketPhotos(saved, photos, authentication);

        TicketResponseDto response = toResponse(saved);

        webSocketManager.broadcast(TicketWebSocketEventDto.builder()
                .type("ticket.created")
                .ticketId(saved.getId())
                .createdAt(toOffsetDateTime(saved.getCreatedAt()))
                .updatedAt(toOffsetDateTime(saved.getUpdatedAt()))
                .status(saved.getStatus())
                .priority(saved.getPriority())
                .build());

        publishTicketNotification(
                saved,
                "CREATED",
                "Ticket creado",
                createdBy,
                "Se creo un nuevo ticket",
                List.of()
        );

        return response;
    }

    @Override
    @Transactional
    public TicketResponseDto update(UUID id, TicketCreateRequestDto request, List<MultipartFile> photos,
                                    Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(TicketException::notFound);

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !Objects.equals(ticket.getCreatedBy(), extractUserId(authentication))) {
            throw TicketException.accessDenied();
        }

        String actorId = extractUserId(authentication);
        String oldTitle = ticket.getTitle();
        String oldDescription = ticket.getDescription();
        TicketPriority oldPriority = ticket.getPriority();
        UUID oldSiteId = ticket.getSiteId();
        UUID oldBuildingId = ticket.getBuildingId();
        UUID oldFloorId = ticket.getFloorId();
        UUID oldLocationId = ticket.getLocationId();
        String oldSiteName = resolveCampusNameSafe(oldSiteId);
        String oldBuildingName = resolveBuildingNameSafe(oldBuildingId);
        String oldFloorName = resolveFloorNameSafe(oldFloorId);
        String oldLocationName = resolveLocationNameSafe(oldLocationId);
        List<TicketPhoto> currentPhotos = new ArrayList<>(ticket.getTicketPhotos());
        Map<UUID, TicketPhoto> currentPhotosById = currentPhotos.stream()
                .collect(Collectors.toMap(TicketPhoto::getId, Function.identity()));

        InventoryCampusResponseDto site = null;
        if (request.getSiteId() != null) {
            site = requireCampus(request.getSiteId());
        }

        InventoryBuildingResponseDto building = null;
        if (request.getBuildingId() != null) {
            building = requireBuilding(request.getBuildingId());

            if (site == null) {
                throw new BuildingException(
                        HttpStatus.BAD_REQUEST,
                        "BUILDING_CAMPUS_REQUIRED",
                        "Debe seleccionar un recinto para indicar un edificio");
            }

            if (!belongsToCampus(building.getId(), site.getId())) {
                throw BuildingException.campusDoesNotMatch(building.getId(), site.getId());
            }
        }

        InventoryAssetFloorResponseDto floor = null;
        if (request.getFloorId() != null) {
            floor = requireFloor(request.getFloorId());
            if (building == null) {
                throw new FloorException(
                        HttpStatus.BAD_REQUEST,
                        "FLOOR_BUILDING_REQUIRED",
                        "Debe seleccionar un edificio para indicar un piso");
            }
            if (floor.getBuilding() == null || !floor.getBuilding().getId().equals(building.getId())) {
                throw new FloorException(
                        HttpStatus.BAD_REQUEST,
                        "FLOOR_BUILDING_MISMATCH",
                        "El piso " + floor.getName() + " no pertenece al edificio indicado");
            }
        }

        InventoryAssetLocationResponseDto location = null;
        if (request.getLocationId() != null) {
            location = requireLocation(request.getLocationId());
            if (location.getFloor() == null || location.getFloor().getId() == null) {
                throw new LocationException(
                        HttpStatus.BAD_REQUEST,
                        "LOCATION_FLOOR_REQUIRED",
                        "La ubicación indicada no tiene un piso asociado");
            }

            if (floor == null) {
                floor = requireFloor(location.getFloor().getId());
            }

            if (building == null || floor.getBuilding() == null || !floor.getBuilding().getId().equals(building.getId())) {
                throw new LocationException(
                        HttpStatus.BAD_REQUEST,
                        "LOCATION_BUILDING_MISMATCH",
                        "La ubicación " + location.getId() + " no pertenece al edificio indicado");
            }

            if (!location.getFloor().getId().equals(floor.getId())) {
                throw new LocationException(
                        HttpStatus.BAD_REQUEST,
                        "LOCATION_FLOOR_MISMATCH",
                        "La ubicación " + location.getId() + " no pertenece al piso indicado");
            }
        }

        List<UUID> assetIds = resolveValidAssetIds(request.getAssetIds(), location);
        List<TicketAsset> currentAssets = new ArrayList<>(ticket.getTicketAssets());
        Set<UUID> oldAssetIds = currentAssets.stream()
                .map(TicketAsset::getAssetId)
                .collect(Collectors.toSet());
        Map<UUID, String> oldAssetLabels = oldAssetIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::buildAssetHistoryLabel));

        List<String> removedPhotoNames = new ArrayList<>();
        if (request.getRemovedPhotoIds() != null && !request.getRemovedPhotoIds().isEmpty()) {
            for (UUID photoId : request.getRemovedPhotoIds()) {
                TicketPhoto existingPhoto = currentPhotosById.get(photoId);
                if (existingPhoto == null) {
                    continue;
                }

                removedPhotoNames.add(resolvePhotoHistoryLabel(existingPhoto));
                ticket.getTicketPhotos().remove(existingPhoto);
                ticketPhotoRepository.delete(existingPhoto);
            }
        }

        ticket.setTitle(request.getTitle().trim());
        ticket.setDescription(request.getDescription().trim());
        ticket.setSiteId(site != null ? site.getId() : null);
        ticket.setBuildingId(building != null ? building.getId() : null);
        ticket.setFloorId(floor != null ? floor.getId() : null);
        ticket.setLocationId(location != null ? location.getId() : null);
        ticket.setUpdatedBy(actorId);

        if (request.getPriority() != null && (isAdmin(authentication) || hasSetPriorityPermission(authentication))) {
            ticket.setPriority(request.getPriority());
        }

        ticketAssetRepository.deleteAll(currentAssets);
        ticket.getTicketAssets().clear();

        saveTicketAssets(ticket, assetIds);
        List<String> addedPhotoNames = saveTicketPhotos(ticket, photos, authentication);

        Ticket saved = ticketRepository.save(ticket);
        Set<UUID> newAssetIds = assetIds.stream().collect(Collectors.toSet());

        List<String> changedFields = new ArrayList<>();

        if (!Objects.equals(oldTitle, saved.getTitle())) {
            saveHistory(saved, TicketHistoryChangeType.EDITED, "title", oldTitle,
                    saved.getTitle(), actorId, null);
            changedFields.add(formatChange("Titulo", oldTitle, saved.getTitle()));
        }

        if (!Objects.equals(oldDescription, saved.getDescription())) {
            saveHistory(saved, TicketHistoryChangeType.EDITED, "description",
                    oldDescription, saved.getDescription(), actorId, null);
            changedFields.add(formatChange("Descripcion", oldDescription, saved.getDescription()));
        }

        if (!Objects.equals(oldPriority, saved.getPriority())) {
            saveHistory(saved, TicketHistoryChangeType.PRIORITY_CHANGED,
                    "priority",
                    oldPriority == null ? null : oldPriority.name(),
                    saved.getPriority() == null ? null : saved.getPriority().name(),
                    actorId, null);
            changedFields.add(formatChange(
                    "Prioridad",
                    toPriorityLabel(oldPriority),
                    toPriorityLabel(saved.getPriority()))
            );
        }

        if (!Objects.equals(oldSiteId, saved.getSiteId())) {
            saveHistory(saved, TicketHistoryChangeType.EDITED, "site",
                    oldSiteName, site != null ? site.getName() : null, actorId, null);
            changedFields.add(formatChange("Recinto", oldSiteName, site != null ? site.getName() : null));
        }

        if (!Objects.equals(oldBuildingId, saved.getBuildingId())) {
            saveHistory(saved, TicketHistoryChangeType.EDITED, "building",
                    oldBuildingName, building != null ? building.getName() : null, actorId, null);
            changedFields.add(formatChange("Edificio", oldBuildingName, building != null ? building.getName() : null));
        }

        if (!Objects.equals(oldFloorId, saved.getFloorId())) {
            saveHistory(saved, TicketHistoryChangeType.EDITED, "floor",
                    oldFloorName, floor != null ? floor.getName() : null, actorId, null);
            changedFields.add(formatChange("Piso", oldFloorName, floor != null ? floor.getName() : null));
        }

        if (!Objects.equals(oldLocationId, saved.getLocationId())) {
            saveHistory(saved, TicketHistoryChangeType.EDITED, "location",
                    oldLocationName, location != null ? location.getDescription() : null, actorId, null);
            changedFields.add(formatChange(
                    "Ubicacion",
                    oldLocationName,
                    location != null ? location.getDescription() : null)
            );
        }

        for (UUID assetId : newAssetIds) {
            if (!oldAssetIds.contains(assetId)) {
                String assetLabel = buildAssetHistoryLabel(assetId);
                saveHistory(saved, TicketHistoryChangeType.EDITED, "asset",
                        null, assetLabel, actorId, null);
                changedFields.add("Activo agregado: " + assetLabel);
            }
        }

        for (UUID oldAssetId : oldAssetIds) {
            if (!newAssetIds.contains(oldAssetId)) {
                String oldAssetLabel = oldAssetLabels.get(oldAssetId);
                saveHistory(saved, TicketHistoryChangeType.EDITED, "asset",
                        oldAssetLabel, null, actorId, null);
                changedFields.add("Activo removido: " + valueOrFallback(oldAssetLabel));
            }
        }

        for (String removedPhotoName : removedPhotoNames) {
            saveHistory(saved, TicketHistoryChangeType.ATTACHMENT_REMOVED,
                    "attachment", removedPhotoName, null, actorId, null);
            changedFields.add("Adjunto removido: " + valueOrFallback(removedPhotoName));
        }

        for (String addedPhotoName : addedPhotoNames) {
            saveHistory(saved, TicketHistoryChangeType.ATTACHMENT_ADDED,
                    "attachment", null, addedPhotoName, actorId, null);
            changedFields.add("Adjunto agregado: " + valueOrFallback(addedPhotoName));
        }

        webSocketManager.broadcast(TicketWebSocketEventDto.builder()
                .type("ticket.updated")
                .ticketId(saved.getId())
                .createdAt(toOffsetDateTime(saved.getCreatedAt()))
                .updatedAt(toOffsetDateTime(saved.getUpdatedAt()))
                .status(saved.getStatus())
                .priority(saved.getPriority())
                .build());

        publishTicketNotification(
                saved,
                "UPDATED",
                "Ticket actualizado",
                actorId,
                "Se actualizo la informacion general del ticket",
                changedFields
        );

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketListResponseDto> findAll(TicketStatus status, Pageable pageable, Authentication authentication) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"))
        );

        Page<Ticket> tickets;

        if (isAdmin(authentication) || hasViewAllTickets(authentication)) {
            tickets = status != null
                    ? ticketRepository.findByStatus(status, sortedPageable)
                    : ticketRepository.findAll(sortedPageable);
        } else {
            String userId = extractUserId(authentication);

            tickets = status != null
                    ? ticketRepository.findByCreatedByAndStatus(userId, status, sortedPageable)
                    : ticketRepository.findByCreatedBy(userId, sortedPageable);
        }

        Map<String, String> authorNames = resolveTicketAuthorNames(tickets);

        return tickets.map(ticket -> toListResponse(ticket, authorNames));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDto findById(UUID id, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(TicketException::notFound);

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !Objects.equals(ticket.getCreatedBy(), extractUserId(authentication))) {
            throw TicketException.accessDenied();
        }

        return toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponseDto updatePriority(UUID id, TicketPriorityUpdateRequestDto request,
                                            Authentication authentication) {
        if (!isAdmin(authentication) && !hasSetPriorityPermission(authentication)) {
            throw TicketException.priorityForbidden();
        }

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(TicketException::notFound);

        com.sssi.msvc_maintenance.entity.enums.TicketPriority oldPriority = ticket.getPriority();
        ticket.setPriority(request.getPriority());
        ticket.setUpdatedBy(extractUserId(authentication));
        Ticket saved = ticketRepository.save(ticket);

        if (oldPriority == null || !oldPriority.equals(saved.getPriority())) {
            saveHistory(saved, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType.PRIORITY_CHANGED,
                    "priority", oldPriority == null ? null : oldPriority.name(),
                    saved.getPriority() == null ? null : saved.getPriority().name(), extractUserId(authentication),
                    null);
        }

        webSocketManager.broadcast(TicketWebSocketEventDto.builder()
                .type("ticket.priority.updated")
                .ticketId(saved.getId())
                .createdAt(toOffsetDateTime(saved.getCreatedAt()))
                .updatedAt(toOffsetDateTime(saved.getUpdatedAt()))
                .status(saved.getStatus())
                .priority(saved.getPriority())
                .build());

        String actorId = extractUserId(authentication);
        publishTicketNotification(
                saved,
                "PRIORITY_CHANGED",
                "Prioridad del ticket actualizada",
                actorId,
                "Se cambio la prioridad del ticket",
                List.of(formatChange("Prioridad", toPriorityLabel(oldPriority), toPriorityLabel(saved.getPriority())))
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public List<KeycloakUserResponse> findAssignableUsers(Authentication authentication) {
        if (!isAdmin(authentication) && !hasAssignTicketPermission(authentication)) {
            throw TicketException.assignableUsersForbidden();
        }

        List<KeycloakUserResponse> users = fetchAllUsersFromAuth();

        Map<String, KeycloakUserResponse> uniqueUsers = new LinkedHashMap<>();
        users.stream()
                .filter(user -> user != null && user.id() != null && !user.id().isBlank())
                .forEach(user -> uniqueUsers.putIfAbsent(user.id(), user));

        return uniqueUsers.values().stream()
                .sorted(Comparator.comparing(user -> buildAuthorName(user, user.id()), String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private List<KeycloakUserResponse> fetchAllUsersFromAuth() {
        List<KeycloakUserResponse> users = new ArrayList<>();
        int pageNumber = 0;

        while (true) {
            ApiResponse<PagedResponse<KeycloakUserResponse>> response = authClient.findUsers(pageNumber, ASSIGNEE_PAGE_SIZE);
            PagedResponse<KeycloakUserResponse> page = response != null ? response.getData() : null;
            List<KeycloakUserResponse> pageContent = page != null && page.content() != null
                ? page.content()
                    : List.of();

            if (pageContent.isEmpty()) {
                break;
            }

            users.addAll(pageContent);

            if (page.totalPages() <= 0 || pageNumber + 1 >= page.totalPages()) {
                break;
            }

            pageNumber++;
        }

        return users;
    }

    @Override
    @Transactional
    public TicketResponseDto updateAssignedTo(UUID id, TicketAssignedToUpdateRequestDto request,
                                              Authentication authentication) {
        if (!isAdmin(authentication) && !hasAssignTicketPermission(authentication)) {
            throw TicketException.assignForbidden();
        }

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(TicketException::notFound);

        UUID oldAssignedTo = ticket.getAssignedTo();
        UUID newAssignedTo = request.getAssignedTo();
        UUID assignedBy = parseUuidOrNull(extractUserId(authentication));

        ticket.setAssignedTo(newAssignedTo);
        ticket.setAssignedBy(assignedBy);
        ticket.setUpdatedBy(extractUserId(authentication));

        Ticket saved = ticketRepository.save(ticket);

        if (!java.util.Objects.equals(oldAssignedTo, saved.getAssignedTo())) {
            String oldValue = oldAssignedTo != null ? resolveAuthorName(oldAssignedTo.toString()) : "Sin asignar";
            String newValue = saved.getAssignedTo() != null ? resolveAuthorName(saved.getAssignedTo().toString()) : "Sin asignar";

            saveHistory(saved, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType.EDITED,
                    "assignedTo", oldValue, newValue, extractUserId(authentication), null);
        }

        webSocketManager.broadcast(TicketWebSocketEventDto.builder()
                .type("ticket.assignedTo.updated")
                .ticketId(saved.getId())
                .createdAt(toOffsetDateTime(saved.getCreatedAt()))
                .updatedAt(toOffsetDateTime(saved.getUpdatedAt()))
                .status(saved.getStatus())
                .priority(saved.getPriority())
                .build());

        String actorId = extractUserId(authentication);
        String oldValue = oldAssignedTo != null ? resolveAuthorName(oldAssignedTo.toString()) : "Sin asignar";
        String newValue = saved.getAssignedTo() != null ? resolveAuthorName(saved.getAssignedTo().toString()) : "Sin asignar";

        publishTicketNotification(
                saved,
                "ASSIGNED_TO_CHANGED",
                "Asignacion del ticket actualizada",
                actorId,
                "Se actualizo la persona asignada al ticket",
                List.of(formatChange("Asignado a", oldValue, newValue))
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TicketCommentResponseDto addComment(UUID id, TicketCommentCreateRequestDto request,
                                               Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(TicketException::notFound);

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !Objects.equals(ticket.getCreatedBy(), extractUserId(authentication))) {
            throw TicketException.accessDenied();
        }

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .authorId(extractUserId(authentication))
                .content(request.getContent().trim())
                .build();

        TicketComment saved = ticketCommentRepository.save(comment);
        ticket.getTicketComments().add(saved);
        String actorId = extractUserId(authentication);
        String actorName = resolveAuthorName(actorId);

        saveHistory(ticket, TicketHistoryChangeType.COMMENT_ADDED, "comment",
                null, saved.getContent(), actorId, null);

        publishTicketNotification(
                ticket,
                "COMMENT_ADDED",
                "Comentario agregado en ticket",
                actorId,
                "Se agrego un nuevo comentario",
                List.of(formatCommentAdded(actorName, saved.getContent()))
        );

        return toCommentResponse(saved);
    }

    @Override
    @Transactional
    public TicketCommentResponseDto updateComment(UUID ticketId, UUID commentId, TicketCommentUpdateRequestDto request,
                                                  Authentication authentication) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(TicketException::notFound);

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !Objects.equals(ticket.getCreatedBy(), extractUserId(authentication))) {
            throw TicketException.accessDenied();
        }

        TicketComment comment = findCommentInTicket(ticket, commentId);
        String actorId = extractUserId(authentication);
        String actorName = resolveAuthorName(actorId);
        if (!actorId.equals(comment.getAuthorId())) {
            throw TicketException.commentEditForbidden();
        }

        String oldContent = comment.getContent();
        String newContent = request.getContent().trim();
        comment.setContent(newContent);
        TicketComment updated = ticketCommentRepository.save(comment);

        if (!Objects.equals(oldContent, newContent)) {
            saveHistory(ticket, TicketHistoryChangeType.COMMENT_EDITED,
                    "comment", oldContent, newContent, actorId, null);
        }

        publishTicketNotification(
                ticket,
                "COMMENT_EDITED",
                "Comentario actualizado en ticket",
                actorId,
                "Se modifico un comentario",
                List.of(formatCommentEdited(actorName, oldContent, newContent))
        );

        return toCommentResponse(updated);
    }

    @Override
    @Transactional
    public void deleteComment(UUID ticketId, UUID commentId, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(TicketException::notFound);

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !Objects.equals(ticket.getCreatedBy(), extractUserId(authentication))) {
            throw TicketException.accessDenied();
        }

        TicketComment comment = findCommentInTicket(ticket, commentId);
        String actorId = extractUserId(authentication);
        if (!actorId.equals(comment.getAuthorId())) {
            throw TicketException.commentDeleteForbidden();
        }

        String oldContent = comment.getContent();
        String actorName = resolveAuthorName(actorId);
        ticket.getTicketComments().remove(comment);
        ticketCommentRepository.delete(comment);

        saveHistory(ticket, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType.COMMENT_REMOVED,
                "eliminó un comentario", oldContent, null, actorId, null);

        publishTicketNotification(
                ticket,
                "COMMENT_REMOVED",
                "Comentario eliminado en ticket",
                actorId,
                "Se elimino un comentario",
                List.of(formatCommentRemoved(actorName, oldContent))
        );
    }

    @Override
    @Transactional
    public TicketResponseDto updateStatus(UUID id, TicketStatusUpdateRequestDto request,
                                          Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(TicketException::notFound);

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !Objects.equals(ticket.getCreatedBy(), extractUserId(authentication))) {
            throw TicketException.accessDenied();
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(request.getStatus());
        ticket.setUpdatedBy(extractUserId(authentication));
        Ticket saved = ticketRepository.save(ticket);

        if (!java.util.Objects.equals(oldStatus, saved.getStatus())) {
            saveHistory(saved, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType.STATUS_CHANGED,
                    "status",
                    oldStatus == null ? null : oldStatus.name(),
                    saved.getStatus() == null ? null : saved.getStatus().name(),
                    extractUserId(authentication), null);
        }

        webSocketManager.broadcast(TicketWebSocketEventDto.builder()
                .type("ticket.status.updated")
                .ticketId(saved.getId())
                .createdAt(toOffsetDateTime(saved.getCreatedAt()))
                .updatedAt(toOffsetDateTime(saved.getUpdatedAt()))
                .status(saved.getStatus())
                .priority(saved.getPriority())
                .build());

        String actorId = extractUserId(authentication);
        publishTicketNotification(
                saved,
                "STATUS_CHANGED",
                "Estado del ticket actualizado",
                actorId,
                "Se cambio el estado del ticket",
                List.of(formatChange("Estado", toStatusLabel(oldStatus), toStatusLabel(saved.getStatus())))
        );

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.sssi.msvc_maintenance.dto.response.TicketPhotoResponseDto> getPhotos(UUID id,
                                                                                         Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(TicketException::notFound);

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !Objects.equals(ticket.getCreatedBy(), extractUserId(authentication))) {
            throw TicketException.accessDenied();
        }

        return ticket.getTicketPhotos().stream()
                .map(photo -> {
                    String imageUrl = null;
                    try {
                        imageUrl = getPresignedUrl(photo.getObjectName());
                    } catch (Exception ignored) {
                    }
                    return com.sssi.msvc_maintenance.dto.response.TicketPhotoResponseDto.builder()
                            .id(photo.getId())
                            .objectName(photo.getObjectName())
                            .fileName(photo.getFileName())
                            .contentType(photo.getContentType())
                            .imageUrl(imageUrl)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<com.sssi.msvc_maintenance.dto.response.TicketHistoryChangeResponseDto> findHistoryByTicket(
            UUID ticketId, Pageable pageable, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(TicketException::notFound);

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !Objects.equals(ticket.getCreatedBy(), extractUserId(authentication))) {
            throw TicketException.accessDenied();
        }

        return ticketHistoryChangeRepository.findByTicketIdOrderByCreatedAtDesc(ticketId, pageable)
                .map(this::toHistoryResponse);
    }

    private com.sssi.msvc_maintenance.dto.response.TicketHistoryChangeResponseDto toHistoryResponse(
            com.sssi.msvc_maintenance.entity.TicketHistoryChange history) {
        return com.sssi.msvc_maintenance.dto.response.TicketHistoryChangeResponseDto.builder()
                .id(history.getId())
                .type(history.getType())
                .fieldName(history.getFieldName())
                .oldValue(history.getOldValue())
                .newValue(history.getNewValue())
                .authorId(history.getAuthorId())
                .authorName(resolveAuthorName(history.getAuthorId()))
                .createdAt(toOffsetDateTime(history.getCreatedAt()))
                .updatedAt(toOffsetDateTime(history.getUpdatedAt()))
                .build();
    }

    private void saveHistory(Ticket ticket, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType type,
                             String fieldName, String oldValue, String newValue, String authorId, String metadata) {
        com.sssi.msvc_maintenance.entity.TicketHistoryChange history = com.sssi.msvc_maintenance.entity.TicketHistoryChange
                .builder()
                .ticket(ticket)
                .type(type)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .authorId(authorId)
                .metadata(metadata)
                .build();

        ticketHistoryChangeRepository.save(history);
    }

    private void publishTicketNotification(Ticket ticket,
                                           String actionType,
                                           String actionLabel,
                                           String actorId,
                                           String detail,
                                           List<String> changedFields) {
        eventPublisher.publishEvent(new TicketNotificationDomainEvent(
                ticket.getId(),
                actionType,
                actionLabel,
                actorId,
                resolveAuthorName(actorId),
                ticket.getTitle(),
                ticket.getDescription(),
                toStatusLabel(ticket.getStatus()),
                toPriorityLabel(ticket.getPriority()),
                resolveCampusNameSafe(ticket.getSiteId()),
                resolveBuildingNameSafe(ticket.getBuildingId()),
                detail,
                changedFields == null ? List.of() : changedFields,
                resolveTicketNotificationEmails(ticket, actorId),
                cleanEmails(notificationProperties.getExtraEmails()),
                System.currentTimeMillis()
        ));
    }

    private List<String> resolveTicketNotificationEmails(Ticket ticket, String actorId) {
        Set<String> userIds = new HashSet<>();
        addAuthorId(userIds, actorId);
        addAuthorId(userIds, ticket.getCreatedBy());
        if (ticket.getAssignedTo() != null) {
            addAuthorId(userIds, ticket.getAssignedTo().toString());
        }
        if (ticket.getAssignedBy() != null) {
            addAuthorId(userIds, ticket.getAssignedBy().toString());
        }
        ticket.getTicketComments().stream()
                .map(TicketComment::getAuthorId)
                .forEach(authorId -> addAuthorId(userIds, authorId));

        List<String> recipients = new ArrayList<>(resolveUserEmails(userIds));
        recipients.addAll(resolveLocationEmails(ticket.getSiteId(), ticket.getBuildingId()));
        return cleanEmails(recipients);
    }

    private List<String> resolveUserEmails(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        try {
            ApiResponse<List<KeycloakUserResponse>> response = authClient.findUsersByKeycloakIds(new ArrayList<>(userIds));
            List<KeycloakUserResponse> users = response != null && response.getData() != null
                    ? response.getData()
                    : List.of();

            return users.stream()
                    .map(KeycloakUserResponse::email)
                    .filter(email -> email != null && !email.isBlank())
                    .toList();
        } catch (Exception exception) {
            log.warn("No se pudieron resolver emails de usuarios de ticket: {}", exception.getMessage());
            return List.of();
        }
    }

    private List<String> resolveLocationEmails(UUID siteId, UUID buildingId) {
        List<String> emails = new ArrayList<>();
        try {
            if (siteId != null) {
                ApiResponse<List<com.sssi.msvc_maintenance.dto.response.InventoryBuildingEmailResponseDto>> response =
                        inventoryClient.findCampusEmails(siteId);
                emails.addAll(extractLocationEmails(response));
            }
        } catch (Exception exception) {
            log.warn("No se pudieron resolver correos del recinto {}: {}", siteId, exception.getMessage());
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

    private String toStatusLabel(TicketStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case OPEN -> "Abierto";
            case IN_PROGRESS -> "En progreso";
            case RESOLVED -> "Resuelto";
            case REOPENED -> "Reabierto";
            case CANCELLED -> "Cancelado";
        };
    }

    private String toPriorityLabel(TicketPriority priority) {
        if (priority == null) {
            return null;
        }
        return switch (priority) {
            case LOW -> "Baja";
            case MEDIUM -> "Media";
            case HIGH -> "Alta";
        };
    }

    private String formatChange(String fieldName, String oldValue, String newValue) {
        return fieldName + ": " + valueOrFallback(oldValue) + " -> " + valueOrFallback(newValue);
    }

    private String formatCommentAdded(String actorName, String newValue) {
        return "Comentario de " + valueOrFallback(actorName) + ": " + valueOrFallback(newValue);
    }

    private String formatCommentEdited(String actorName, String oldValue, String newValue) {
        return "Comentario editado por " + valueOrFallback(actorName)
                + ": " + valueOrFallback(oldValue) + " - " + valueOrFallback(newValue);
    }

    private String formatCommentRemoved(String actorName, String oldValue) {
        return "Comentario eliminado por " + valueOrFallback(actorName) + ": " + valueOrFallback(oldValue);
    }

    private String valueOrFallback(String value) {
        if (value == null || value.isBlank()) {
            return "Sin valor";
        }
        return value;
    }

    private void saveTicketAssets(Ticket ticket, List<UUID> assetIds) {
        for (UUID assetId : assetIds) {
            TicketAsset ticketAsset = TicketAsset.builder()
                    .ticket(ticket)
                    .assetId(assetId)
                    .build();

            ticketAssetRepository.save(ticketAsset);
            ticket.getTicketAssets().add(ticketAsset);
        }
    }

    private List<String> saveTicketPhotos(Ticket ticket, List<MultipartFile> photos, Authentication authentication) {
        List<String> addedPhotoNames = new ArrayList<>();

        if (photos == null || photos.isEmpty()) {
            return addedPhotoNames;
        }

        for (MultipartFile file : photos) {
            String objectName = uploadFileToArchive(file, authentication);

            TicketPhoto photo = TicketPhoto.builder()
                    .ticket(ticket)
                    .objectName(objectName)
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .build();

            ticketPhotoRepository.save(photo);
            ticket.getTicketPhotos().add(photo);
            addedPhotoNames.add(resolvePhotoHistoryLabel(photo));
        }

        return addedPhotoNames;
    }

    private String resolvePhotoHistoryLabel(TicketPhoto photo) {
        if (photo.getFileName() != null && !photo.getFileName().isBlank()) {
            return photo.getFileName();
        }

        return photo.getObjectName();
    }

    private String buildAssetHistoryLabel(InventoryAssetResponseDto asset) {
        if (asset == null) {
            return "Activo";
        }

        String assetNumber = asset.getAssetNumber() != null && !asset.getAssetNumber().isBlank()
                ? asset.getAssetNumber()
                : asset.getId().toString();
        String modelName = asset.getModel() != null ? asset.getModel().getName() : null;

        if (modelName == null || modelName.isBlank()) {
            return assetNumber;
        }

        return assetNumber + " - " + modelName;
    }

    private String buildAssetHistoryLabel(UUID assetId) {
        if (assetId == null) {
            return "Activo";
        }

        try {
            return buildAssetHistoryLabel(requireAsset(assetId));
        } catch (Exception exception) {
            return assetId.toString();
        }
    }

    private String resolveCampusNameSafe(UUID campusId) {
        if (campusId == null) {
            return null;
        }

        try {
            return requireCampus(campusId).getName();
        } catch (Exception exception) {
            return campusId.toString();
        }
    }

    private String resolveBuildingNameSafe(UUID buildingId) {
        if (buildingId == null) {
            return null;
        }

        try {
            return requireBuilding(buildingId).getName();
        } catch (Exception exception) {
            return buildingId.toString();
        }
    }

    private String resolveFloorNameSafe(UUID floorId) {
        if (floorId == null) {
            return null;
        }

        try {
            return requireFloor(floorId).getName();
        } catch (Exception exception) {
            return floorId.toString();
        }
    }

    private String resolveLocationNameSafe(UUID locationId) {
        if (locationId == null) {
            return null;
        }

        try {
            return requireLocation(locationId).getDescription();
        } catch (Exception exception) {
            return locationId.toString();
        }
    }

    private List<UUID> resolveValidAssetIds(List<UUID> requestAssetIds, InventoryAssetLocationResponseDto location) {
        List<UUID> assetIds = new ArrayList<>();

        if (requestAssetIds == null || requestAssetIds.isEmpty()) {
            return assetIds;
        }

        for (UUID assetId : requestAssetIds) {
            InventoryAssetResponseDto asset = requireAsset(assetId);

            if (location != null && asset.getLocation() != null
                    && !asset.getLocation().getId().equals(location.getId())) {
                throw new AssetException(
                        HttpStatus.BAD_REQUEST,
                        "ASSET_LOCATION_MISMATCH",
                        "El activo " + asset.getId() + " no pertenece a la ubicación indicada");
            }

            assetIds.add(assetId);
        }

        return assetIds;
    }

    private TicketListResponseDto toListResponse(Ticket ticket, Map<String, String> authorNames) {
        return TicketListResponseDto.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .createdBy(ticket.getCreatedBy())
                .createdByName(getAuthorName(authorNames, ticket.getCreatedBy(), ticket.getCreatedBy()))
                .createdAt(toOffsetDateTime(ticket.getCreatedAt()))
                .updatedAt(toOffsetDateTime(ticket.getUpdatedAt()))
                .build();
    }

    private TicketResponseDto toResponse(Ticket ticket) {
        InventoryCampusResponseDto site = resolveCampusForResponse(ticket.getSiteId());
        InventoryBuildingResponseDto building = resolveBuildingForResponse(ticket.getBuildingId());
        InventoryAssetFloorResponseDto floor = resolveFloorForResponse(ticket.getFloorId());
        InventoryAssetLocationResponseDto location = resolveLocationForResponse(ticket.getLocationId());

        List<TicketAssetResponseDto> assets = ticket.getTicketAssets().stream()
                .map(ticketAsset -> {
                    InventoryAssetResponseDto asset = resolveAssetForResponse(ticketAsset.getAssetId());

                    return TicketAssetResponseDto.builder()
                            .id(ticketAsset.getId())
                            .assetId(ticketAsset.getAssetId())
                            .assetNumber(asset != null ? asset.getAssetNumber() : null)
                            .serialNumber(asset != null ? asset.getSerialNumber() : null)
                            .assetName(asset != null && asset.getModel() != null ? asset.getModel().getName() : null)
                            .modelName(asset != null && asset.getModel() != null ? asset.getModel().getName() : null)
                            .type(asset != null && asset.getModel() != null && asset.getModel().getType() != null
                                ? asset.getModel().getType().getName()
                                : null)
                            .brand(asset != null && asset.getModel() != null && asset.getModel().getBrand() != null
                                ? asset.getModel().getBrand().getName()
                                : null)
                            .locationDescription(asset != null && asset.getLocation() != null ? asset.getLocation().getDescription() : null)
                            .build();
                })
                .collect(Collectors.toList());

        List<TicketPhotoResponseDto> photos = ticket.getTicketPhotos().stream()
                .map(photo -> TicketPhotoResponseDto.builder()
                        .id(photo.getId())
                        .objectName(photo.getObjectName())
                        .fileName(photo.getFileName())
                        .contentType(photo.getContentType())
                        .imageUrl(resolvePresignedUrlForResponse(photo.getObjectName()))
                        .build())
                .collect(Collectors.toList());

        List<TicketComment> sortedTicketComments = ticket.getTicketComments().stream()
                .sorted((a, b) -> {
                    LocalDateTime aDate = a.getCreatedAt();
                    LocalDateTime bDate = b.getCreatedAt();

                    if (aDate == null && bDate == null) {
                        return 0;
                    }
                    if (aDate == null) {
                        return 1;
                    }
                    if (bDate == null) {
                        return -1;
                    }

                    return bDate.compareTo(aDate);
                })
                .collect(Collectors.toList());

        Set<String> authorIds = new HashSet<>();
        addAuthorId(authorIds, ticket.getCreatedBy());

        if (ticket.getAssignedTo() != null) {
            addAuthorId(authorIds, ticket.getAssignedTo().toString());
        }

        if (ticket.getAssignedBy() != null) {
            addAuthorId(authorIds, ticket.getAssignedBy().toString());
        }

        sortedTicketComments.stream()
                .map(TicketComment::getAuthorId)
                .forEach(authorId -> addAuthorId(authorIds, authorId));

        Map<String, String> authorNames = resolveAuthorNames(authorIds);

        List<TicketCommentResponseDto> comments = sortedTicketComments.stream()
                .map(comment -> toCommentResponse(comment, authorNames))
                .collect(Collectors.toList());

        String assignedTo = ticket.getAssignedTo() != null ? ticket.getAssignedTo().toString() : null;
        String assignedBy = ticket.getAssignedBy() != null ? ticket.getAssignedBy().toString() : null;

        return TicketResponseDto.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .createdBy(ticket.getCreatedBy())
                .createdByName(getAuthorName(authorNames, ticket.getCreatedBy(), ticket.getCreatedBy()))
                .assignedTo(ticket.getAssignedTo())
                .assignedBy(ticket.getAssignedBy())
                .assignedToName(assignedTo != null ? getAuthorName(authorNames, assignedTo, assignedTo) : null)
                .assignedByName(assignedBy != null ? getAuthorName(authorNames, assignedBy, assignedBy) : null)
                .siteId(ticket.getSiteId())
                .siteName(site != null ? site.getName() : null)
                .buildingId(ticket.getBuildingId())
                .buildingName(building != null ? building.getName() : null)
                .floorId(ticket.getFloorId())
                .floorName(floor != null ? floor.getName() : null)
                .locationId(ticket.getLocationId())
                .locationDescription(location != null ? location.getDescription() : null)
                .createdAt(toOffsetDateTime(ticket.getCreatedAt()))
                .updatedAt(toOffsetDateTime(ticket.getUpdatedAt()))
                .assets(assets)
                .photos(photos)
                .comments(comments)
                .build();
    }

    private InventoryCampusResponseDto resolveCampusForResponse(UUID id) {
        if (id == null) {
            return null;
        }

        try {
            return requireCampus(id);
        } catch (Exception exception) {
            log.warn("No se pudo resolver el campus asociado al ticket. campusId={}", id, exception);
            return null;
        }
    }

    private InventoryBuildingResponseDto resolveBuildingForResponse(UUID id) {
        if (id == null) {
            return null;
        }

        try {
            return requireBuilding(id);
        } catch (Exception exception) {
            log.warn("No se pudo resolver el edificio asociado al ticket. buildingId={}", id, exception);
            return null;
        }
    }

    private InventoryAssetFloorResponseDto resolveFloorForResponse(UUID id) {
        if (id == null) {
            return null;
        }

        try {
            return requireFloor(id);
        } catch (Exception exception) {
            log.warn("No se pudo resolver el piso asociado al ticket. floorId={}", id, exception);
            return null;
        }
    }

    private InventoryAssetLocationResponseDto resolveLocationForResponse(UUID id) {
        if (id == null) {
            return null;
        }

        try {
            return requireLocation(id);
        } catch (Exception exception) {
            log.warn("No se pudo resolver la ubicación asociada al ticket. locationId={}", id, exception);
            return null;
        }
    }

    private InventoryAssetResponseDto resolveAssetForResponse(UUID id) {
        if (id == null) {
            return null;
        }

        try {
            return requireAsset(id);
        } catch (Exception exception) {
            log.warn("No se pudo resolver el activo asociado al ticket. assetId={}", id, exception);
            return null;
        }
    }

    private String resolvePresignedUrlForResponse(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }

        try {
            return getPresignedUrl(objectName);
        } catch (Exception exception) {
            log.warn("No se pudo obtener la URL del adjunto asociado al ticket. objectName={}", objectName, exception);
            return null;
        }
    }

    private TicketCommentResponseDto toCommentResponse(TicketComment comment) {
        Set<String> authorIds = new HashSet<>();
        addAuthorId(authorIds, comment.getAuthorId());
        return toCommentResponse(comment, resolveAuthorNames(authorIds));
    }

    private TicketCommentResponseDto toCommentResponse(TicketComment comment, Map<String, String> authorNames) {
        return TicketCommentResponseDto.builder()
                .id(comment.getId())
                .authorId(comment.getAuthorId())
                .authorName(getAuthorName(authorNames, comment.getAuthorId(), comment.getAuthorId()))
                .content(comment.getContent())
                .createdAt(toOffsetDateTime(comment.getCreatedAt()))
                .updatedAt(toOffsetDateTime(comment.getUpdatedAt()))
                .build();
    }

    private TicketComment findCommentInTicket(Ticket ticket, UUID commentId) {
        return ticket.getTicketComments().stream()
                .filter(comment -> comment.getId().equals(commentId))
                .findFirst()
                .orElseThrow(TicketException::commentNotFound);
    }

    private Map<String, String> resolveTicketAuthorNames(Page<Ticket> tickets) {
        Set<String> authorIds = new HashSet<>();

        tickets.getContent().forEach(ticket -> {
            addAuthorId(authorIds, ticket.getCreatedBy());

            if (ticket.getAssignedTo() != null) {
                authorIds.add(ticket.getAssignedTo().toString());
            }
        });

        return resolveAuthorNames(authorIds);
    }

    private Map<String, String> resolveAuthorNames(Collection<String> authorIds) {
        Set<String> ids = authorIds.stream()
                .filter(authorId -> authorId != null && !authorId.isBlank() && !"null".equals(authorId))
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Map.of();
        }

        try {
            ApiResponse<List<KeycloakUserResponse>> response = authClient.findUsersByKeycloakIds(new ArrayList<>(ids));
            List<KeycloakUserResponse> users = response != null && response.getData() != null
                    ? response.getData()
                    : List.of();

            Map<String, String> names = users.stream()
                    .filter(user -> user != null && user.id() != null && !user.id().isBlank())
                    .collect(Collectors.toMap(
                            KeycloakUserResponse::id,
                            user -> buildAuthorName(user, user.id()),
                            (current, replacement) -> current,
                            HashMap::new
                    ));

            ids.forEach(id -> names.putIfAbsent(id, id));

            return names;
        } catch (Exception exception) {
            return ids.stream().collect(Collectors.toMap(
                    Function.identity(),
                    Function.identity()
            ));
        }
    }

    private String resolveAuthorName(String authorId) {
        if (authorId == null || authorId.isBlank() || "null".equals(authorId)) {
            return "Usuario";
        }

        return resolveAuthorNames(List.of(authorId)).getOrDefault(authorId, authorId);
    }

    private String buildAuthorName(KeycloakUserResponse user, String fallback) {
        String fullName = ((user.firstName() != null ? user.firstName() : "") + " "
                + (user.lastName() != null ? user.lastName() : "")).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }

        if (user.username() != null && !user.username().isBlank()) {
            return user.username();
        }

        if (user.email() != null && !user.email().isBlank()) {
            return user.email();
        }

        return fallback;
    }

    private void addAuthorId(Set<String> authorIds, String authorId) {
        if (authorId != null && !authorId.isBlank() && !"null".equals(authorId)) {
            authorIds.add(authorId);
        }
    }

    private String getAuthorName(Map<String, String> authorNames, String authorId, String fallback) {
        if (authorId == null || authorId.isBlank() || "null".equals(authorId)) {
            return fallback;
        }

        return authorNames.getOrDefault(authorId, fallback);
    }

    private InventoryCampusResponseDto requireCampus(UUID id) {
        ApiResponse<InventoryCampusResponseDto> response = inventoryClient.findCampusById(id);
        InventoryCampusResponseDto campus = response != null ? response.getData() : null;

        if (campus == null) {
            throw CampusException.notFound(id.toString());
        }

        return campus;
    }

    private InventoryBuildingResponseDto requireBuilding(UUID id) {
        ApiResponse<InventoryBuildingResponseDto> response = inventoryClient.findBuildingById(id);
        InventoryBuildingResponseDto building = response != null ? response.getData() : null;

        if (building == null) {
            throw BuildingException.notFound(id.toString());
        }

        return building;
    }

    private InventoryAssetFloorResponseDto requireFloor(UUID id) {
        ApiResponse<InventoryAssetFloorResponseDto> response = inventoryClient.findFloorById(id);
        InventoryAssetFloorResponseDto floor = response != null ? response.getData() : null;

        if (floor == null) {
            throw FloorException.notFound(id.toString());
        }

        return floor;
    }

    private InventoryAssetLocationResponseDto requireLocation(UUID id) {
        ApiResponse<InventoryAssetLocationResponseDto> response = inventoryClient.findLocationById(id);
        InventoryAssetLocationResponseDto location = response != null ? response.getData() : null;

        if (location == null) {
            throw LocationException.notFound(id.toString());
        }

        return location;
    }

    private InventoryAssetResponseDto requireAsset(UUID id) {
        ApiResponse<InventoryAssetResponseDto> response = inventoryClient.findAssetById(id);
        InventoryAssetResponseDto asset = response != null ? response.getData() : null;

        if (asset == null) {
            throw AssetException.notFound(id.toString());
        }

        return asset;
    }

    private boolean belongsToCampus(UUID buildingId, UUID campusId) {
        ApiResponse<PageResponse<InventoryBuildingResponseDto>> response = inventoryClient
                .findBuildingsByCampus(campusId, 0, 1000);
        PageResponse<InventoryBuildingResponseDto> body = response != null ? response.getData() : null;

        if (body == null || body.getContent() == null) {
            return false;
        }

        return body.getContent().stream()
                .anyMatch(building -> building.getId() != null && building.getId().equals(buildingId));
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> {
                    String auth = authority.getAuthority();

                    return auth.equalsIgnoreCase("admin")
                            || auth.equalsIgnoreCase("ADMIN")
                            || auth.equalsIgnoreCase("administrador")
                            || auth.equalsIgnoreCase("ADMINISTRADOR");
                });
    }

    private boolean hasViewAllTickets(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> {
                    String auth = authority.getAuthority();

                    return auth.equalsIgnoreCase(Privileges.Tickets.LEER_TODOS)
                            || auth.equalsIgnoreCase("MAINTENANCE_TICKETS_VIEW_ALL")
                            || auth.equalsIgnoreCase("maintenance.tickets.view_all")
                            || auth.equalsIgnoreCase("maintenance:tickets:view_all");
                });
    }

    private boolean hasSetPriorityPermission(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> {
                    String auth = authority.getAuthority();

                    return auth.equalsIgnoreCase(Privileges.Tickets.ASIGNAR_PRIORIDAD)
                            || auth.equalsIgnoreCase("MAINTENANCE_TICKETS_SET_PRIORITY")
                            || auth.equalsIgnoreCase("maintenance.tickets.set_priority")
                            || auth.equalsIgnoreCase("maintenance:tickets:set_priority");
                });
    }

    private boolean hasAssignTicketPermission(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> {
                    String auth = authority.getAuthority();

                    return auth.equalsIgnoreCase(Privileges.Tickets.ASIGNAR_TICKET)
                            || auth.equalsIgnoreCase("MAINTENANCE_TICKETS_ASSIGN")
                            || auth.equalsIgnoreCase("maintenance.tickets.assign")
                            || auth.equalsIgnoreCase("maintenance:tickets:assign");
                });
    }

    private String extractUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getSubject();
        }

        return authentication == null ? "" : authentication.getName();
    }

    private UUID parseUuidOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @SneakyThrows
    private String uploadFileToArchive(MultipartFile file, Authentication authentication) {
        String initiateUrl = UriComponentsBuilder.fromHttpUrl(archiveBaseUrl)
                .path("/api/v1/archive/files/initiate")
                .queryParam("filename", file.getOriginalFilename())
                .queryParam("contentType", file.getContentType())
                .queryParam("totalSize", file.getSize())
                .toUriString();

        HttpHeaders initHeaders = new HttpHeaders();
        initHeaders.setBearerAuth(resolveBearerToken(authentication));

        ResponseEntity<ApiResponse<ArchiveUploadInitResponseDto>> initResponse = restTemplate.exchange(
                initiateUrl,
                HttpMethod.POST,
                new HttpEntity<>(initHeaders),
                new ParameterizedTypeReference<>() {
                });

        ArchiveUploadInitResponseDto init = initResponse.getBody() != null ? initResponse.getBody().getData() : null;

        if (init == null) {
            throw TicketException.archiveUploadFailed();
        }

        String partUrl = UriComponentsBuilder.fromHttpUrl(archiveBaseUrl)
                .path("/api/v1/archive/files/part")
                .toUriString();

        byte[] bytes = file.getBytes();
        int totalParts = Math.max(1, (int) Math.ceil((double) bytes.length / ARCHIVE_CHUNK_SIZE_BYTES));

        for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
            int start = (partNumber - 1) * ARCHIVE_CHUNK_SIZE_BYTES;
            int end = Math.min(start + ARCHIVE_CHUNK_SIZE_BYTES, bytes.length);
            byte[] chunkBytes = java.util.Arrays.copyOfRange(bytes, start, end);

            MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
            multipart.add("uploadId", init.getUploadId());
            multipart.add("objectName", init.getObjectName());
            multipart.add("partNumber", String.valueOf(partNumber));
            multipart.add("file", new ByteArrayResource(chunkBytes) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpHeaders uploadHeaders = new HttpHeaders();
            uploadHeaders.setBearerAuth(resolveBearerToken(authentication));
            uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> uploadEntity = new HttpEntity<>(multipart, uploadHeaders);
            restTemplate.exchange(partUrl, HttpMethod.POST, uploadEntity, String.class);
        }

        String completeUrl = UriComponentsBuilder.fromHttpUrl(archiveBaseUrl)
                .path("/api/v1/archive/files/complete")
                .queryParam("uploadId", init.getUploadId())
                .queryParam("objectName", init.getObjectName())
                .queryParam("totalSize", file.getSize())
                .toUriString();

        HttpHeaders completeHeaders = new HttpHeaders();
        completeHeaders.setBearerAuth(resolveBearerToken(authentication));

        restTemplate.exchange(completeUrl, HttpMethod.POST, new HttpEntity<>(completeHeaders), String.class);

        return init.getObjectName();
    }

    private String resolveBearerToken(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getTokenValue();
        }

        throw TicketException.missingAuthenticationToken();
    }

    private String resolveBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getTokenValue();
        }

        throw TicketException.missingAuthenticationToken();
    }

    private String getPresignedUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        return "/api/v1/archive/files/" + objectName;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atZone(TICKET_TIME_ZONE).toOffsetDateTime();
    }
}
