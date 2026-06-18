package com.sssi.msvc_maintenance.service.impl;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.msvc_archive.dto.ArchiveUploadInitResponseDto;
import com.sssi.msvc_maintenance.client.AuthClient;
import com.sssi.msvc_maintenance.client.InventoryClient;
import com.sssi.msvc_maintenance.dto.request.TicketAssignedRoleUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCommentCreateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCreateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketPriorityUpdateRequestDto;
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
import org.springframework.data.domain.Pageable;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    public TicketResponseDto create(TicketCreateRequestDto request, List<MultipartFile> photos, Authentication authentication) {
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
                        "El piso " + floor.getName() + " no pertenece al edificio indicado"
                );
            }
        }

        InventoryAssetLocationResponseDto location = null;
        if (request.getLocationId() != null) {
            location = requireLocation(request.getLocationId());
            if (floor == null || location.getFloor() == null || !location.getFloor().getId().equals(floor.getId())) {
                throw new LocationException(
                        HttpStatus.BAD_REQUEST,
                        "LOCATION_FLOOR_MISMATCH",
                        "La ubicación " + location.getId() + " no pertenece al piso indicado"
                );
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
                .createdBy(createdBy)
                .siteId(site.getId())
                .buildingId(building.getId())
                .floorId(floor != null ? floor.getId() : null)
                .locationId(location != null ? location.getId() : null)
                .build();

        Ticket saved = ticketRepository.save(ticket);

        saveTicketAssets(saved, assetIds);
        saveTicketPhotos(saved, photos, authentication);

        TicketResponseDto response = toResponse(saved);

        webSocketManager.broadcast(TicketWebSocketEventDto.builder()
                .type("ticket.created")
                .ticketId(saved.getId())
                .createdBy(saved.getCreatedBy())
                .assignedRole(saved.getAssignedRole())
                .status(saved.getStatus())
                .priority(saved.getPriority())
                .build());

        return response;
    }

    @Override
    @Transactional
    public TicketResponseDto update(UUID id, TicketCreateRequestDto request, List<MultipartFile> photos, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication) && !ticket.getCreatedBy().equals(extractUserId(authentication))) {
            throw new RuntimeException("Acceso denegado");
        }

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
                        "El piso " + floor.getName() + " no pertenece al edificio indicado"
                );
            }
        }

        InventoryAssetLocationResponseDto location = null;
        if (request.getLocationId() != null) {
            location = requireLocation(request.getLocationId());
            if (floor == null || location.getFloor() == null || !location.getFloor().getId().equals(floor.getId())) {
                throw new LocationException(
                        HttpStatus.BAD_REQUEST,
                        "LOCATION_FLOOR_MISMATCH",
                        "La ubicación " + location.getId() + " no pertenece al piso indicado"
                );
            }
        }

        List<UUID> assetIds = resolveValidAssetIds(request.getAssetIds(), location);

        ticket.setTitle(request.getTitle().trim());
        ticket.setDescription(request.getDescription().trim());
        ticket.setSiteId(site.getId());
        ticket.setBuildingId(building.getId());
        ticket.setFloorId(floor != null ? floor.getId() : null);
        ticket.setLocationId(location != null ? location.getId() : null);

        if (request.getPriority() != null && (isAdmin(authentication) || hasSetPriorityPermission(authentication))) {
            ticket.setPriority(request.getPriority());
        }

        List<TicketAsset> currentAssets = new ArrayList<>(ticket.getTicketAssets());
        ticketAssetRepository.deleteAll(currentAssets);
        ticket.getTicketAssets().clear();

        saveTicketAssets(ticket, assetIds);
        saveTicketPhotos(ticket, photos, authentication);

        Ticket saved = ticketRepository.save(ticket);

        if (!java.util.Objects.equals(oldTitle, saved.getTitle())) {
            saveHistory(saved, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType.EDITED, "title", oldTitle, saved.getTitle(), extractUserId(authentication), null);
        }
        if (!java.util.Objects.equals(oldDescription, saved.getDescription())) {
            saveHistory(saved, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType.EDITED, "description", oldDescription, saved.getDescription(), extractUserId(authentication), null);
        }

        webSocketManager.broadcast(TicketWebSocketEventDto.builder()
                .type("ticket.updated")
                .ticketId(saved.getId())
                .createdBy(saved.getCreatedBy())
                .assignedRole(saved.getAssignedRole())
                .status(saved.getStatus())
                .priority(saved.getPriority())
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketListResponseDto> findAll(Pageable pageable, Authentication authentication) {
        if (isAdmin(authentication) || hasViewAllTickets(authentication)) {
            return ticketRepository.findAll(pageable).map(this::toListResponse);
        }

        return ticketRepository.findByCreatedBy(extractUserId(authentication), pageable).map(this::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDto findById(UUID id, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication) && !ticket.getCreatedBy().equals(extractUserId(authentication))) {
            throw new RuntimeException("Acceso denegado");
        }

        return toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponseDto updatePriority(UUID id, TicketPriorityUpdateRequestDto request, Authentication authentication) {
        if (!isAdmin(authentication) && !hasSetPriorityPermission(authentication)) {
            throw new RuntimeException("Solo administradores o usuarios con permiso pueden cambiar la prioridad");
        }

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        com.sssi.msvc_maintenance.entity.enums.TicketPriority oldPriority = ticket.getPriority();
        ticket.setPriority(request.getPriority());
        Ticket saved = ticketRepository.save(ticket);

        if (oldPriority == null || !oldPriority.equals(saved.getPriority())) {
            saveHistory(saved, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType.PRIORITY_CHANGED, "priority", oldPriority == null ? null : oldPriority.name(), saved.getPriority() == null ? null : saved.getPriority().name(), extractUserId(authentication), null);
        }

        webSocketManager.broadcast(TicketWebSocketEventDto.builder()
                .type("ticket.priority.updated")
                .ticketId(saved.getId())
                .createdBy(saved.getCreatedBy())
                .assignedRole(saved.getAssignedRole())
                .status(saved.getStatus())
                .priority(saved.getPriority())
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponseDto updateAssignedRole(UUID id, TicketAssignedRoleUpdateRequestDto request, Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw new RuntimeException("Solo administradores pueden asignar rol responsable");
        }

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        String oldAssignedRole = ticket.getAssignedRole();
        ticket.setAssignedRole(request.getAssignedRole());
        Ticket saved = ticketRepository.save(ticket);

        if (oldAssignedRole == null && saved.getAssignedRole() != null || (oldAssignedRole != null && !oldAssignedRole.equals(saved.getAssignedRole()))) {
            saveHistory(saved, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType.ASSIGNED_ROLE_CHANGED, "assignedRole", oldAssignedRole, saved.getAssignedRole(), extractUserId(authentication), null);
        }

        webSocketManager.broadcast(TicketWebSocketEventDto.builder()
                .type("ticket.assignedRole.updated")
                .ticketId(saved.getId())
                .createdBy(saved.getCreatedBy())
                .assignedRole(saved.getAssignedRole())
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
            if (ticket.getAssignedRole() == null || ticket.getAssignedRole().isBlank()) {
                throw new RuntimeException("No se puede resolver ticket sin rol asignado");
            }

            boolean hasRole = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equalsIgnoreCase(ticket.getAssignedRole()));

            if (!hasRole) {
                throw new RuntimeException("Usuario no autorizado para resolver este ticket");
            }
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.RESOLVED);
        Ticket saved = ticketRepository.save(ticket);

        if (oldStatus == null || !oldStatus.equals(saved.getStatus())) {
            saveHistory(saved, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType.STATUS_CHANGED, "status", oldStatus == null ? null : oldStatus.name(), saved.getStatus() == null ? null : saved.getStatus().name(), extractUserId(authentication), null);
        }

        webSocketManager.broadcast(TicketWebSocketEventDto.builder()
                .type("ticket.resolved")
                .ticketId(saved.getId())
                .createdBy(saved.getCreatedBy())
                .assignedRole(saved.getAssignedRole())
                .status(saved.getStatus())
                .priority(saved.getPriority())
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TicketCommentResponseDto addComment(UUID id, TicketCommentCreateRequestDto request, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (!isAdmin(authentication) && !hasViewAllTickets(authentication) && !ticket.getCreatedBy().equals(extractUserId(authentication))) {
            throw new RuntimeException("Acceso denegado");
        }

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .authorId(extractUserId(authentication))
                .content(request.getContent().trim())
                .build();

        TicketComment saved = ticketCommentRepository.save(comment);
        ticket.getTicketComments().add(saved);

        saveHistory(ticket, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType.COMMENT_ADDED, "comment", null, saved.getContent(), extractUserId(authentication), null);

        return toCommentResponse(saved);
    }

    private void saveHistory(Ticket ticket, com.sssi.msvc_maintenance.entity.enums.TicketHistoryChangeType type, String fieldName, String oldValue, String newValue, String authorId, String metadata) {
        com.sssi.msvc_maintenance.entity.TicketHistoryChange history = com.sssi.msvc_maintenance.entity.TicketHistoryChange.builder()
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

    private void saveTicketPhotos(Ticket ticket, List<MultipartFile> photos, Authentication authentication) {
        if (photos == null || photos.isEmpty()) {
            return;
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
        }
    }

    private List<UUID> resolveValidAssetIds(List<UUID> requestAssetIds, InventoryAssetLocationResponseDto location) {
        List<UUID> assetIds = new ArrayList<>();

        if (requestAssetIds == null || requestAssetIds.isEmpty()) {
            return assetIds;
        }

        for (UUID assetId : requestAssetIds) {
            InventoryAssetResponseDto asset = requireAsset(assetId);

            if (location != null && asset.getLocation() != null && !asset.getLocation().getId().equals(location.getId())) {
                throw new AssetException(
                        HttpStatus.BAD_REQUEST,
                        "ASSET_LOCATION_MISMATCH",
                        "El activo " + asset.getId() + " no pertenece a la ubicación indicada"
                );
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
                .createdAt(toOffsetDateTime(ticket.getCreatedAt()))
                .updatedAt(toOffsetDateTime(ticket.getUpdatedAt()))
                .build();
    }

    private TicketResponseDto toResponse(Ticket ticket) {
        InventoryCampusResponseDto site = ticket.getSiteId() != null ? requireCampus(ticket.getSiteId()) : null;
        InventoryBuildingResponseDto building = ticket.getBuildingId() != null ? requireBuilding(ticket.getBuildingId()) : null;
        InventoryAssetFloorResponseDto floor = ticket.getFloorId() != null ? requireFloor(ticket.getFloorId()) : null;
        InventoryAssetLocationResponseDto location = ticket.getLocationId() != null ? requireLocation(ticket.getLocationId()) : null;

        List<TicketAssetResponseDto> assets = ticket.getTicketAssets().stream()
                .map(ticketAsset -> {
                    InventoryAssetResponseDto asset = requireAsset(ticketAsset.getAssetId());

                    return TicketAssetResponseDto.builder()
                            .id(ticketAsset.getId())
                            .assetId(asset.getId())
                            .assetNumber(asset.getAssetNumber())
                            .serialNumber(asset.getSerialNumber())
                            .assetName(asset.getModel() != null ? asset.getModel().getName() : null)
                            .locationDescription(asset.getLocation() != null ? asset.getLocation().getDescription() : null)
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

                    if (aDate == null && bDate == null) return 0;
                    if (aDate == null) return 1;
                    if (bDate == null) return -1;

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
                .assignedRole(ticket.getAssignedRole())
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
                        }
                ));
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

            String fullName = ((user.firstName() != null ? user.firstName() : "") + " " + (user.lastName() != null ? user.lastName() : "")).trim();

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
        ApiResponse<PageResponse<InventoryBuildingResponseDto>> response = inventoryClient.findBuildingsByCampus(campusId, 0, 1000);
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

                    return auth.equalsIgnoreCase(Privileges.Tickets.VER_TODOS)
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

    private String extractUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getSubject();
        }

        return authentication == null ? "" : authentication.getName();
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
                }
        );

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
                        .queryParam("objectName", UriUtils.encodePath(objectName, java.nio.charset.StandardCharsets.UTF_8))
                        .toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                }
        );

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
