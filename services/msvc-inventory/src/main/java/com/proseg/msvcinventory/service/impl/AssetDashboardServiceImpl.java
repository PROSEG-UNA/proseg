package com.proseg.msvcinventory.service.impl;

import com.proseg.msvcinventory.dto.response.AssetDashboardResponseDto;
import com.proseg.msvcinventory.dto.response.AssetResponseDto;
import com.proseg.msvcinventory.dto.response.ExpiryStatsDto;
import com.proseg.msvcinventory.dto.response.TypeCountDto;
import com.proseg.msvcinventory.entity.Asset;
import com.proseg.msvcinventory.entity.enums.AssetExpiryCategory;
import com.proseg.msvcinventory.entity.enums.AssetStatus;
import com.proseg.msvcinventory.mapper.AssetMapper;
import com.proseg.msvcinventory.repository.AssetRepository;
import com.proseg.msvcinventory.service.AssetDashboardService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetDashboardServiceImpl implements AssetDashboardService {

    private static final ZoneId ZONE = ZoneId.of("America/Costa_Rica");

    private final AssetRepository assetRepository;
    private final AssetMapper assetMapper;

    @Override
    public AssetDashboardResponseDto getSummary(int days) {
        LocalDate today = LocalDate.now(ZONE);
        int safeDays = Math.max(1, Math.min(days, 365));
        LocalDate limit = today.plusDays(safeDays);

        List<TypeCountDto> byType = assetRepository.countByTypeAndStatus(AssetStatus.APROBADO).stream()
                .limit(8)
                .map(row -> TypeCountDto.builder()
                        .name(row.getName())
                        .total(row.getTotal())
                        .build())
                .toList();

        return AssetDashboardResponseDto.builder()
                .referenceDate(today)
                .days(safeDays)
                .totalAssets(assetRepository.count())
                .activeAssets(assetRepository.countByStatus(AssetStatus.APROBADO))
                .decommissionedAssets(assetRepository.countByStatus(AssetStatus.DE_BAJA))
                .warranty(ExpiryStatsDto.builder()
                        .expired(assetRepository.countByStatusAndWarrantyEndDateBefore(AssetStatus.APROBADO, today))
                        .expiringSoon(assetRepository.countByStatusAndWarrantyEndDateBetween(AssetStatus.APROBADO, today, limit))
                        .valid(assetRepository.countByStatusAndWarrantyEndDateAfter(AssetStatus.APROBADO, limit))
                        .noDate(assetRepository.countByStatusAndWarrantyEndDateIsNull(AssetStatus.APROBADO))
                        .build())
                .firmware(ExpiryStatsDto.builder()
                        .expired(assetRepository.countByStatusAndFirmwareSupportEndDateBefore(AssetStatus.APROBADO, today))
                        .expiringSoon(assetRepository.countByStatusAndFirmwareSupportEndDateBetween(AssetStatus.APROBADO, today, limit))
                        .valid(assetRepository.countByStatusAndFirmwareSupportEndDateAfter(AssetStatus.APROBADO, limit))
                        .noDate(assetRepository.countByStatusAndFirmwareSupportEndDateIsNull(AssetStatus.APROBADO))
                        .build())
                .byType(byType)
                .build();
    }

    @Override
    public Page<AssetResponseDto> findExpiringAssets(AssetExpiryCategory category, int days, Pageable pageable) {
        LocalDate today = LocalDate.now(ZONE);
        int safeDays = Math.max(1, Math.min(days, 365));
        LocalDate limit = today.plusDays(safeDays);
        String dateField = dateField(category);

        Specification<Asset> spec = (root, query, cb) -> {
            Predicate status = cb.equal(root.get("status"), AssetStatus.APROBADO);
            Predicate range = switch (category) {
                case WARRANTY_EXPIRED, FIRMWARE_EXPIRED -> cb.lessThan(root.<LocalDate>get(dateField), today);
                case WARRANTY_EXPIRING, FIRMWARE_EXPIRING -> cb.between(root.<LocalDate>get(dateField), today, limit);
            };
            return cb.and(status, range);
        };

        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(dateField).ascending()
        );

        return assetRepository.findAll(spec, sorted).map(assetMapper::toResponse);
    }

    private String dateField(AssetExpiryCategory category) {
        return switch (category) {
            case WARRANTY_EXPIRED, WARRANTY_EXPIRING -> "warrantyEndDate";
            case FIRMWARE_EXPIRED, FIRMWARE_EXPIRING -> "firmwareSupportEndDate";
        };
    }
}
