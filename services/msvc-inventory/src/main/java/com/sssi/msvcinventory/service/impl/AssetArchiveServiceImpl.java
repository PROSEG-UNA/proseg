package com.sssi.msvcinventory.service.impl;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.msvcinventory.dto.request.AssetArchiveRequestDto;
import com.sssi.msvcinventory.dto.response.AssetArchiveResponseDto;
import com.sssi.msvcinventory.dto.response.PresignedUrlResponseDto;
import com.sssi.msvcinventory.entity.Asset;
import com.sssi.msvcinventory.entity.AssetArchive;
import com.sssi.msvcinventory.exception.AssetArchiveException;
import com.sssi.msvcinventory.exception.AssetException;
import com.sssi.msvcinventory.mapper.AssetArchiveMapper;
import com.sssi.msvcinventory.repository.AssetArchiveRepository;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.service.AssetArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetArchiveServiceImpl implements AssetArchiveService {

    private final AssetArchiveRepository assetArchiveRepository;
    private final AssetRepository assetRepository;
    private final AssetArchiveMapper assetArchiveMapper;
    private final RestTemplate restTemplate;

    @Value("${archive.base-url:http://localhost:8081}")
    private String archiveBaseUrl;

    @Override
    @Transactional
    public AssetArchiveResponseDto create(AssetArchiveRequestDto request) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> AssetException.notFound(request.getAssetId().toString()));

        AssetArchive assetArchive = assetArchiveMapper.toEntity(request);
        assetArchive.setAsset(asset);

        return toResponse(assetArchiveRepository.save(assetArchive));
    }

    @Override
    @Transactional(readOnly = true)
    public AssetArchiveResponseDto findById(UUID id) {
        return assetArchiveRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> AssetArchiveException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetArchiveResponseDto> findByAssetId(UUID assetId, Pageable pageable) {
        if (!assetRepository.existsById(assetId)) {
            throw AssetException.notFound(assetId.toString());
        }
        return assetArchiveRepository.findByAssetId(assetId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetArchiveResponseDto> getAssetArchivesByAssetId(UUID assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> AssetException.notFound(assetId.toString()));

        return assetArchiveRepository.findByAssetId(asset.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AssetArchiveResponseDto update(UUID id, AssetArchiveRequestDto request) {
        AssetArchive assetArchive = assetArchiveRepository.findById(id)
                .orElseThrow(() -> AssetArchiveException.notFound(id.toString()));

        assetArchiveMapper.updateEntityFromRequest(request, assetArchive);
        return toResponse(assetArchiveRepository.save(assetArchive));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        AssetArchive assetArchive = assetArchiveRepository.findById(id)
                .orElseThrow(() -> AssetArchiveException.notFound(id.toString()));
        assetArchiveRepository.delete(assetArchive);
    }

    public String getPresignedGetUrl(String objectName) {
        String url = archiveBaseUrl + "/api/v1/archive/files/presigned?objectName="
                + UriUtils.encodePath(objectName, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(resolveBearerToken());

        ResponseEntity<ApiResponse<PresignedUrlResponseDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                }
        );

        ApiResponse<PresignedUrlResponseDto> body = response.getBody();
        if (body == null || body.getData() == null || body.getData().getUrl() == null) {
            throw new RuntimeException("No fue posible obtener la URL firmada");
        }

        return body.getData().getUrl();
    }

    private AssetArchiveResponseDto toResponse(AssetArchive assetArchive) {
        String presignedUrl = getPresignedGetUrl(assetArchive.getObjectName());
        return AssetArchiveResponseDto.builder()
                .id(assetArchive.getId())
                .caption(assetArchive.getCaption())
                .imageUrl(presignedUrl)
                .build();
    }

    private String resolveBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken().getTokenValue();
        }
        throw new RuntimeException("No hay token de autenticacion para solicitar URL firmada");
    }
}
