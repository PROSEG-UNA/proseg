package com.sssi.msvc_maintenance.service.impl;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.msvc_archive.dto.ArchiveUploadInitResponseDto;
import com.sssi.msvc_maintenance.client.InventoryClient;
import com.sssi.msvc_maintenance.dto.request.TicketAssignedRoleUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCommentCreateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCreateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketPriorityUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetResponseDto;
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
import com.sssi.msvcinventory.dto.response.BuildingResponseDto;
import com.sssi.msvcinventory.dto.response.CampusResponseDto;
import com.sssi.msvcinventory.dto.response.FloorResponseDto;
import com.sssi.msvcinventory.dto.response.LocationResponseDto;
import com.sssi.msvcinventory.exception.AssetException;
import com.sssi.msvcinventory.exception.BuildingException;
import com.sssi.msvcinventory.exception.CampusException;
import com.sssi.msvcinventory.exception.FloorException;
import com.sssi.msvcinventory.exception.LocationException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.ParameterizedTypeReference;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final ZoneId TICKET_TIME_ZONE = ZoneId.of("America/Costa_Rica");

    private final TicketRepository ticketRepository;
    private final TicketAssetRepository ticketAssetRepository;
    private final TicketPhotoRepository ticketPhotoRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final InventoryClient inventoryClient;
    private final RestTemplate restTemplate;
    private final TicketWebSocketManager webSocketManager;

    private final String archiveBaseUrl = System.getProperty("archive.base-url", "http://localhost:8081");

    @Override
    @Transactional
    public TicketResponseDto create(TicketCreateRequestDto request, List<MultipartFile> photos, Authentication authentication) {
        CampusResponseDto site = requireCampus(request.getSiteId());

        BuildingResponseDto building = requireBuilding(request.getBuildingId());
        if (building.getCampus() == null || !building.getCampus().getId().equals(site.getId())) {
            throw BuildingException.campusDoesNotMatch(building.getId(), site.getId());
        }

        FloorResponseDto floor = null;
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

        LocationResponseDto location = null;
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

        List<UUID> assetIds = new ArrayList<>();
        if (request.getAssetIds() != null && !request.getAssetIds().isEmpty()) {
            for (UUID assetId : request.getAssetIds()) {
                requireAsset(assetId);
                assetIds.add(assetId);
            }
        }

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

        for (UUID assetId : assetIds) {
            TicketAsset ticketAsset = TicketAsset.builder()
                    .ticket(saved)
                    .assetId(assetId)
                    .build();
            ticketAssetRepository.save(ticketAsset);
            saved.getTicketAssets().add(ticketAsset);
        }

        if (photos != null && !photos.isEmpty()) {
            for (MultipartFile file : photos) {
                String objectName = uploadFileToArchive(file, authentication);
                TicketPhoto photo = TicketPhoto.builder()
                        .ticket(saved)
                        .objectName(objectName)
                        .fileName(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .build();
                ticketPhotoRepository.save(photo);
                saved.getTicketPhotos().add(photo);
            }
        }

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

        CampusResponseDto site = requireCampus(request.getSiteId());

        BuildingResponseDto building = requireBuilding(request.getBuildingId());
        if (building.getCampus() == null || !building.getCampus().getId().equals(site.getId())) {
            throw BuildingException.campusDoesNotMatch(building.getId(), site.getId());
        }

        FloorResponseDto floor = null;
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

        LocationResponseDto location = null;
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

        if (request.getAssetIds() != null && !request.getAssetIds().isEmpty()) {
            for (UUID assetId : request.getAssetIds()) {
                requireAsset(assetId);
                TicketAsset ticketAsset = TicketAsset.builder()
                        .ticket(ticket)
                        .assetId(assetId)
                        .build();
                ticketAssetRepository.save(ticketAsset);
                ticket.getTicketAssets().add(ticketAsset);
            }
        }

        if (photos != null && !photos.isEmpty()) {
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

        Ticket saved = ticketRepository.save(ticket);

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
    public Page<TicketResponseDto> findAll(Pageable pageable, Authentication authentication) {
        if (isAdmin(authentication) || hasViewAllTickets(authentication)) {
            return ticketRepository.findAll(pageable).map(this::toResponse);
        }

        return ticketRepository.findByCreatedBy(extractUserId(authentication), pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketListResponseDto> findAllLight(Pageable pageable, Authentication authentication) {
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

        ticket.setPriority(request.getPriority());
        Ticket saved = ticketRepository.save(ticket);

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

        ticket.setAssignedRole(request.getAssignedRole());
        Ticket saved = ticketRepository.save(ticket);

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

        ticket.setStatus(TicketStatus.RESOLVED);
        Ticket saved = ticketRepository.save(ticket);

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

        return toCommentResponse(saved);
    }

    private TicketListResponseDto toListResponse(Ticket ticket) {
        return TicketListResponseDto.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .createdBy(ticket.getCreatedBy())
                .assignedRole(ticket.getAssignedRole())
                .siteId(ticket.getSiteId())
                .siteName(null)
                .buildingId(ticket.getBuildingId())
                .buildingName(null)
                .floorId(ticket.getFloorId())
                .floorName(null)
                .locationId(ticket.getLocationId())
                .locationDescription(null)
                .createdAt(toOffsetDateTime(ticket.getCreatedAt()))
                .updatedAt(toOffsetDateTime(ticket.getUpdatedAt()))
                .assetsCount(ticket.getTicketAssets().size())
                .photosCount(ticket.getTicketPhotos().size())
                .build();
    }

    private TicketResponseDto toResponse(Ticket ticket) {
        CampusResponseDto site = ticket.getSiteId() != null ? requireCampus(ticket.getSiteId()) : null;
        BuildingResponseDto building = ticket.getBuildingId() != null ? requireBuilding(ticket.getBuildingId()) : null;
        FloorResponseDto floor = ticket.getFloorId() != null ? requireFloor(ticket.getFloorId()) : null;
        LocationResponseDto location = ticket.getLocationId() != null ? requireLocation(ticket.getLocationId()) : null;

        List<TicketAssetResponseDto> assets = ticket.getTicketAssets().stream()
                .map(ticketAsset -> {
                    InventoryAssetResponseDto asset = requireAsset(ticketAsset.getAssetId());
                    return TicketAssetResponseDto.builder()
                            .id(ticketAsset.getId())
                            .assetId(asset.getId())
                            .assetNumber(asset.getAssetNumber())
                            .serialNumber(asset.getSerialNumber())
                            .assetName(asset.getModel() != null ? asset.getModel().getName() : null)
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

        List<TicketCommentResponseDto> comments = ticket.getTicketComments().stream()
                .sorted((a, b) -> {
                    LocalDateTime aDate = a.getCreatedAt();
                    LocalDateTime bDate = b.getCreatedAt();
                    if (aDate == null && bDate == null) return 0;
                    if (aDate == null) return -1;
                    if (bDate == null) return 1;
                    return aDate.compareTo(bDate);
                })
                .map(this::toCommentResponse)
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
        return TicketCommentResponseDto.builder()
                .id(comment.getId())
                .authorId(comment.getAuthorId())
                .content(comment.getContent())
                .createdAt(toOffsetDateTime(comment.getCreatedAt()))
                .updatedAt(toOffsetDateTime(comment.getUpdatedAt()))
                .build();
    }

    private CampusResponseDto requireCampus(UUID id) {
        ApiResponse<CampusResponseDto> response = inventoryClient.findCampusById(id);
        CampusResponseDto campus = response != null ? response.getData() : null;

        if (campus == null) {
            throw CampusException.notFound(id.toString());
        }

        return campus;
    }

    private BuildingResponseDto requireBuilding(UUID id) {
        ApiResponse<BuildingResponseDto> response = inventoryClient.findBuildingById(id);
        BuildingResponseDto building = response != null ? response.getData() : null;

        if (building == null) {
            throw BuildingException.notFound(id.toString());
        }

        return building;
    }

    private FloorResponseDto requireFloor(UUID id) {
        ApiResponse<FloorResponseDto> response = inventoryClient.findFloorById(id);
        FloorResponseDto floor = response != null ? response.getData() : null;

        if (floor == null) {
            throw FloorException.notFound(id.toString());
        }

        return floor;
    }

    private LocationResponseDto requireLocation(UUID id) {
        ApiResponse<LocationResponseDto> response = inventoryClient.findLocationById(id);
        LocationResponseDto location = response != null ? response.getData() : null;

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