package com.sssi.msvc_maintenance.service.impl;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.msvc_archive.dto.ArchiveUploadInitResponseDto;
import com.sssi.msvc_maintenance.client.AuthClient;
import com.sssi.msvc_maintenance.client.InventoryClient;
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
import com.sssi.msvc_maintenance.entity.Ticket;
import com.sssi.msvc_maintenance.entity.TicketAsset;
import com.sssi.msvc_maintenance.entity.TicketComment;
import com.sssi.msvc_maintenance.entity.TicketPhoto;
import com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType;
import com.sssi.msvc_maintenance.entity.enums.TicketPriority;
import com.sssi.msvc_maintenance.entity.enums.TicketStatus;
import com.sssi.msvc_maintenance.repository.TicketAssetRepository;
import com.sssi.msvc_maintenance.repository.TicketCommentRepository;
import com.sssi.msvc_maintenance.repository.TicketPhotoRepository;
import com.sssi.msvc_maintenance.repository.TicketRepository;
import com.sssi.msvc_maintenance.security.Privileges;
import com.sssi.msvc_maintenance.service.TicketService;
import com.sssi.msvc_maintenance.websocket.TicketWebSocketEventDto;
import com.sssi.msvc_maintenance.websocket.TicketWebSocketManager;
import com.sssi.msvcinventory.exception.AssetException;
import com.sssi.msvcinventory.exception.BuildingException;
import com.sssi.msvcinventory.exception.CampusException;
import com.sssi.msvcinventory.exception.FloorException;
import com.sssi.msvcinventory.exception.LocationException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
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
import org.springframework.web.server.ResponseStatusException;
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

    private final TicketRepository ticketRepository;
    private final TicketAssetRepository ticketAssetRepository;
    private final TicketPhotoRepository ticketPhotoRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final com.sssi.msvc_maintenance.repository.TicketHistoryChangeRepository ticketHistoryChangeRepository;
    private final InventoryClient inventoryClient;
    private final RestTemplate restTemplate;
    private final TicketWebSocketManager webSocketManager;
    private final AuthClient authClient;

    private final String archiveBaseUrl = System.getProperty("archive.base-url", "http://localhost:8081");

    @Override
    @Transactional
    public TicketResponseDto create(TicketCreateRequestDto request, List<MultipartFile> photos,
                                    Authentication authentication) {
        InventoryCampusResponseDto site = requireCampus(request.getSiteId());

        InventoryBuildingResponseDto building = requireBuilding(request.getBuildingId());
        if (!belongsToCampus(building.getId(), site.getId())) {
            throw BuildingException.campusDoesNotMatch(building.getId(), site.getId());
        }

        InventoryAssetFloorResponseDto floor = null;
        if (request.getFloorId() != null) {
            floor = requireFloor(request.getFloorId());
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
            if (floor == null || location.getFloor() == null || !location.getFloor().getId().equals(floor.getId())) {
                throw new LocationException(
                        HttpStatus.BAD_REQUEST,
                        "LOCATION_FLOOR_MISMATCH",
                        "La ubicaciÃ³n " + location.getId() + " no pertenece al piso indicado");
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
                .siteId(site.getId())
                .buildingId(building.getId())
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

        return response;
    }

    @Override
    @Transactional
    public TicketResponseDto update(UUID id, TicketCreateRequestDto request, List<MultipartFile> photos,
                                    Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !ticket.getCreatedBy().equals(extractUserId(authentication))) {
            throw new RuntimeException("Acceso denegado");
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

        InventoryCampusResponseDto site = requireCampus(request.getSiteId());

        InventoryBuildingResponseDto building = requireBuilding(request.getBuildingId());
        if (!belongsToCampus(building.getId(), site.getId())) {
            throw BuildingException.campusDoesNotMatch(building.getId(), site.getId());
        }

        InventoryAssetFloorResponseDto floor = null;
        if (request.getFloorId() != null) {
            floor = requireFloor(request.getFloorId());
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
            if (floor == null || location.getFloor() == null || !location.getFloor().getId().equals(floor.getId())) {
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
        ticket.setSiteId(site.getId());
        ticket.setBuildingId(building.getId());
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

        if (!Objects.equals(oldTitle, saved.getTitle())) {
            saveHistory(saved, TicketHistoryChangeType.EDITED, "title", oldTitle,
                    saved.getTitle(), actorId, null);
        }

        if (!Objects.equals(oldDescription, saved.getDescription())) {
            saveHistory(saved, TicketHistoryChangeType.EDITED, "description",
                    oldDescription, saved.getDescription(), actorId, null);
        }

        if (!Objects.equals(oldPriority, saved.getPriority())) {
            saveHistory(saved, TicketHistoryChangeType.PRIORITY_CHANGED,
                    "priority",
                    oldPriority == null ? null : oldPriority.name(),
                    saved.getPriority() == null ? null : saved.getPriority().name(),
                    actorId, null);
        }

        if (!Objects.equals(oldSiteId, saved.getSiteId())) {
            saveHistory(saved, TicketHistoryChangeType.EDITED, "site",
                    oldSiteName, site.getName(), actorId, null);
        }

        if (!Objects.equals(oldBuildingId, saved.getBuildingId())) {
            saveHistory(saved, TicketHistoryChangeType.EDITED, "building",
                    oldBuildingName, building.getName(), actorId, null);
        }

        if (!Objects.equals(oldFloorId, saved.getFloorId())) {
            saveHistory(saved, TicketHistoryChangeType.EDITED, "floor",
                    oldFloorName, floor != null ? floor.getName() : null, actorId, null);
        }

        if (!Objects.equals(oldLocationId, saved.getLocationId())) {
            saveHistory(saved, TicketHistoryChangeType.EDITED, "location",
                    oldLocationName, location != null ? location.getDescription() : null, actorId, null);
        }

        for (UUID assetId : newAssetIds) {
            if (!oldAssetIds.contains(assetId)) {
                saveHistory(saved, TicketHistoryChangeType.EDITED, "asset",
                        null, buildAssetHistoryLabel(assetId), actorId, null);
            }
        }

        for (UUID oldAssetId : oldAssetIds) {
            if (!newAssetIds.contains(oldAssetId)) {
                saveHistory(saved, TicketHistoryChangeType.EDITED, "asset",
                        oldAssetLabels.get(oldAssetId), null, actorId, null);
            }
        }

        for (String removedPhotoName : removedPhotoNames) {
            saveHistory(saved, TicketHistoryChangeType.ATTACHMENT_REMOVED,
                    "attachment", removedPhotoName, null, actorId, null);
        }

        for (String addedPhotoName : addedPhotoNames) {
            saveHistory(saved, TicketHistoryChangeType.ATTACHMENT_ADDED,
                    "attachment", null, addedPhotoName, actorId, null);
        }

        webSocketManager.broadcast(TicketWebSocketEventDto.builder()
                .type("ticket.updated")
                .ticketId(saved.getId())
                .createdAt(toOffsetDateTime(saved.getCreatedAt()))
                .updatedAt(toOffsetDateTime(saved.getUpdatedAt()))
                .status(saved.getStatus())
                .priority(saved.getPriority())
                .build());

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

        if (isAdmin(authentication) || hasViewAllTickets(authentication)) {
            if (status != null) {
                return ticketRepository.findByStatus(status, sortedPageable).map(this::toListResponse);
            }

            return ticketRepository.findAll(sortedPageable).map(this::toListResponse);
        }

        String userId = extractUserId(authentication);

        if (status != null) {
            return ticketRepository.findByCreatedByAndStatus(userId, status, sortedPageable).map(this::toListResponse);
        }

        return ticketRepository.findByCreatedBy(userId, sortedPageable).map(this::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDto findById(UUID id, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !ticket.getCreatedBy().equals(extractUserId(authentication))) {
            throw new RuntimeException("Acceso denegado");
        }

        return toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponseDto updatePriority(UUID id, TicketPriorityUpdateRequestDto request,
                                            Authentication authentication) {
        if (!isAdmin(authentication) && !hasSetPriorityPermission(authentication)) {
            throw new RuntimeException("Solo administradores o usuarios con permiso pueden cambiar la prioridad");
        }

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

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

        return toResponse(saved);
    }

    @Override
    @Transactional
    public List<KeycloakUserResponse> findAssignableUsers(Authentication authentication) {
        if (!isAdmin(authentication) && !hasAssignTicketPermission(authentication)) {
            throw new RuntimeException("No tienes permisos para consultar usuarios asignables");
        }

        ApiResponse<PageResponse<KeycloakUserResponse>> response = authClient.findUsers(0, 200, null);
        PageResponse<KeycloakUserResponse> page = response != null ? response.getData() : null;
        List<KeycloakUserResponse> users = page != null && page.getContent() != null ? page.getContent() : List.of();

        return users.stream()
                .filter(user -> user != null && user.status() != null && "APPROVED".equalsIgnoreCase(user.status()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TicketResponseDto updateAssignedTo(UUID id, TicketAssignedToUpdateRequestDto request,
                                              Authentication authentication) {
        if (!isAdmin(authentication) && !hasAssignTicketPermission(authentication)) {
            throw new RuntimeException("Solo administradores o usuarios con permiso pueden asignar tickets");
        }

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

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

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponseDto resolve(UUID id, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (!isAdmin(authentication)) {
            if (ticket.getAssignedTo() == null) {
                throw new RuntimeException("No se puede resolver ticket sin usuario asignado");
            }

            UUID actorUuid = parseUuidOrNull(extractUserId(authentication));
            boolean canResolve = actorUuid != null && actorUuid.equals(ticket.getAssignedTo());

            if (!canResolve) {
                throw new RuntimeException("Usuario no autorizado para resolver este ticket");
            }
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setUpdatedBy(extractUserId(authentication));
        Ticket saved = ticketRepository.save(ticket);

        if (oldStatus == null || !oldStatus.equals(saved.getStatus())) {
            saveHistory(saved, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType.STATUS_CHANGED, "status",
                    oldStatus == null ? null : oldStatus.name(),
                    saved.getStatus() == null ? null : saved.getStatus().name(), extractUserId(authentication), null);
        }

        webSocketManager.broadcast(TicketWebSocketEventDto.builder()
                .type("ticket.resolved")
                .ticketId(saved.getId())
                .createdAt(toOffsetDateTime(saved.getCreatedAt()))
                .updatedAt(toOffsetDateTime(saved.getUpdatedAt()))
                .status(saved.getStatus())
                .priority(saved.getPriority())
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TicketCommentResponseDto addComment(UUID id, TicketCommentCreateRequestDto request,
                                               Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !ticket.getCreatedBy().equals(extractUserId(authentication))) {
            throw new RuntimeException("Acceso denegado");
        }

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .authorId(extractUserId(authentication))
                .content(request.getContent().trim())
                .build();

        TicketComment saved = ticketCommentRepository.save(comment);
        ticket.getTicketComments().add(saved);

        saveHistory(ticket, TicketHistoryChangeType.COMMENT_ADDED, "comment",
                null, saved.getContent(), extractUserId(authentication), null);

        return toCommentResponse(saved);
    }

    @Override
    @Transactional
    public TicketCommentResponseDto updateComment(UUID ticketId, UUID commentId, TicketCommentUpdateRequestDto request,
                                                  Authentication authentication) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !ticket.getCreatedBy().equals(extractUserId(authentication))) {
            throw new RuntimeException("Acceso denegado");
        }

        TicketComment comment = findCommentInTicket(ticket, commentId);
        String actorId = extractUserId(authentication);
        if (!actorId.equals(comment.getAuthorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo puedes editar tus propios comentarios");
        }

        String oldContent = comment.getContent();
        String newContent = request.getContent().trim();
        comment.setContent(newContent);
        TicketComment updated = ticketCommentRepository.save(comment);

        if (!Objects.equals(oldContent, newContent)) {
            saveHistory(ticket, TicketHistoryChangeType.EDITED,
                    "comment", oldContent, newContent, actorId, null);
        }

        return toCommentResponse(updated);
    }

    @Override
    @Transactional
    public void deleteComment(UUID ticketId, UUID commentId, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !ticket.getCreatedBy().equals(extractUserId(authentication))) {
            throw new RuntimeException("Acceso denegado");
        }

        TicketComment comment = findCommentInTicket(ticket, commentId);
        String actorId = extractUserId(authentication);
        if (!actorId.equals(comment.getAuthorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo puedes eliminar tus propios comentarios");
        }

        String oldContent = comment.getContent();
        ticket.getTicketComments().remove(comment);
        ticketCommentRepository.delete(comment);

        saveHistory(ticket, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType.OTHER,
                "eliminó un comentario", oldContent, null, actorId, null);
    }

    @Override
    @Transactional
    public TicketResponseDto updateStatus(UUID id, TicketStatusUpdateRequestDto request,
                                          Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !ticket.getCreatedBy().equals(extractUserId(authentication))) {
            throw new RuntimeException("Acceso denegado");
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

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.sssi.msvc_maintenance.dto.response.TicketPhotoResponseDto> getPhotos(UUID id,
                                                                                         Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !ticket.getCreatedBy().equals(extractUserId(authentication))) {
            throw new RuntimeException("Acceso denegado");
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
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication)
            && !ticket.getCreatedBy().equals(extractUserId(authentication))) {
            throw new RuntimeException("Acceso denegado");
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

        return assetNumber + " · " + modelName;
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
                        "El activo " + asset.getId() + " no pertenece a la ubicaciÃ³n indicada");
            }

            assetIds.add(assetId);
        }

        return assetIds;
    }

    private TicketListResponseDto toListResponse(Ticket ticket) {
        return TicketListResponseDto.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .createdBy(ticket.getCreatedBy())
                .createdByName(resolveAuthorName(ticket.getCreatedBy()))
                .createdAt(toOffsetDateTime(ticket.getCreatedAt()))
                .updatedAt(toOffsetDateTime(ticket.getUpdatedAt()))
                .build();
    }

    private TicketResponseDto toResponse(Ticket ticket) {
        InventoryCampusResponseDto site = ticket.getSiteId() != null ? requireCampus(ticket.getSiteId()) : null;
        InventoryBuildingResponseDto building = ticket.getBuildingId() != null ? requireBuilding(ticket.getBuildingId())
                : null;
        InventoryAssetFloorResponseDto floor = ticket.getFloorId() != null ? requireFloor(ticket.getFloorId()) : null;
        InventoryAssetLocationResponseDto location = ticket.getLocationId() != null
                ? requireLocation(ticket.getLocationId())
                : null;

        List<TicketAssetResponseDto> assets = ticket.getTicketAssets().stream()
                .map(ticketAsset -> {
                    InventoryAssetResponseDto asset = requireAsset(ticketAsset.getAssetId());

                    return TicketAssetResponseDto.builder()
                            .id(ticketAsset.getId())
                            .assetId(asset.getId())
                            .assetNumber(asset.getAssetNumber())
                            .serialNumber(asset.getSerialNumber())
                            .assetName(asset.getModel() != null ? asset.getModel().getName() : null)
                            .locationDescription(
                                    asset.getLocation() != null ? asset.getLocation().getDescription() : null)
                            .build();
                })
                .collect(Collectors.toList());

        List<TicketPhotoResponseDto> photos = ticket.getTicketPhotos().stream()
                .map(photo -> {
                    String imageUrl = null;

                    try {
                        imageUrl = getPresignedUrl(photo.getObjectName());
                    } catch (Exception ignored) {
                    }

                    return TicketPhotoResponseDto.builder()
                            .id(photo.getId())
                            .objectName(photo.getObjectName())
                            .fileName(photo.getFileName())
                            .contentType(photo.getContentType())
                            .imageUrl(imageUrl)
                            .build();
                })
                .collect(Collectors.toList());

        List<TicketComment> sortedTicketComments = ticket.getTicketComments().stream()
                .sorted((a, b) -> {
                    LocalDateTime aDate = a.getCreatedAt();
                    LocalDateTime bDate = b.getCreatedAt();

                    if (aDate == null && bDate == null)
                        return 0;
                    if (aDate == null)
                        return 1;
                    if (bDate == null)
                        return -1;

                    return bDate.compareTo(aDate);
                })
                .collect(Collectors.toList());

        Map<String, String> authorNames = resolveAuthorNames(sortedTicketComments);

        List<TicketCommentResponseDto> comments = sortedTicketComments.stream()
                .map(comment -> toCommentResponse(comment, authorNames))
                .collect(Collectors.toList());

        return TicketResponseDto.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .createdBy(ticket.getCreatedBy())
                .createdByName(resolveAuthorName(ticket.getCreatedBy()))
                .assignedTo(ticket.getAssignedTo())
                .assignedBy(ticket.getAssignedBy())
                .assignedToName(ticket.getAssignedTo() != null ? resolveAuthorName(ticket.getAssignedTo().toString()) : null)
                .assignedByName(ticket.getAssignedBy() != null ? resolveAuthorName(ticket.getAssignedBy().toString()) : null)
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

    private TicketCommentResponseDto toCommentResponse(TicketComment comment) {
        return toCommentResponse(comment, Map.of(comment.getAuthorId(), resolveAuthorName(comment.getAuthorId())));
    }

    private TicketCommentResponseDto toCommentResponse(TicketComment comment, Map<String, String> authorNames) {
        return TicketCommentResponseDto.builder()
                .id(comment.getId())
                .authorId(comment.getAuthorId())
                .authorName(authorNames.getOrDefault(comment.getAuthorId(), comment.getAuthorId()))
                .content(comment.getContent())
                .createdAt(toOffsetDateTime(comment.getCreatedAt()))
                .updatedAt(toOffsetDateTime(comment.getUpdatedAt()))
                .build();
    }

    private TicketComment findCommentInTicket(Ticket ticket, UUID commentId) {
        return ticket.getTicketComments().stream()
                .filter(comment -> comment.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Comentario no encontrado para este ticket"));
    }

    private Map<String, String> resolveAuthorNames(List<TicketComment> comments) {
        Set<String> authorIds = comments.stream()
                .map(TicketComment::getAuthorId)
                .filter(authorId -> authorId != null && !authorId.isBlank())
                .collect(Collectors.toSet());

        return authorIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        authorId -> {
                            String authorName = resolveAuthorName(authorId);
                            return authorName != null && !authorName.isBlank() ? authorName : authorId;
                        }));
    }

    private String resolveAuthorName(String authorId) {
        if (authorId == null || authorId.isBlank()) {
            return "Usuario";
        }

        try {
            ApiResponse<KeycloakUserResponse> response = authClient.findUserByKeycloakId(authorId);
            KeycloakUserResponse user = response != null ? response.getData() : null;

            if (user == null) {
                return authorId;
            }

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

            return authorId;
        } catch (Exception exception) {
            return authorId;
        }
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
            throw new RuntimeException("No se pudo iniciar carga en archive");
        }

        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("uploadId", init.getUploadId());
        multipart.add("objectName", init.getObjectName());
        multipart.add("partNumber", "1");
        multipart.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setBearerAuth(resolveBearerToken(authentication));
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> uploadEntity = new HttpEntity<>(multipart, uploadHeaders);

        String partUrl = UriComponentsBuilder.fromHttpUrl(archiveBaseUrl)
                .path("/api/v1/archive/files/part")
                .toUriString();

        restTemplate.exchange(partUrl, HttpMethod.POST, uploadEntity, String.class);

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

        throw new RuntimeException("No hay token de autenticacion para solicitar al archive");
    }

    private String resolveBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getTokenValue();
        }

        throw new RuntimeException("No hay token de autenticacion para solicitar al archive");
    }

    private String getPresignedUrl(String objectName) {
        String url = archiveBaseUrl + "/api/v1/archive/files/presigned";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(resolveBearerToken());

        ResponseEntity<ApiResponse<com.sssi.msvc_archive.dto.PresignedUrlResponseDto>> response = restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(url)
                        .queryParam("objectName",
                                UriUtils.encodePath(objectName, java.nio.charset.StandardCharsets.UTF_8))
                        .toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                });

        ApiResponse<com.sssi.msvc_archive.dto.PresignedUrlResponseDto> body = response.getBody();

        if (body == null || body.getData() == null) {
            return null;
        }

        return body.getData().getUrl();
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atZone(TICKET_TIME_ZONE).toOffsetDateTime();
    }
}
