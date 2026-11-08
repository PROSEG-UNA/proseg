package com.proseg.msvcinventory.service.impl;

import com.proseg.msvcinventory.dto.response.AssetDashboardResponseDto;
import com.proseg.msvcinventory.dto.response.ExpiryStatsDto;
import com.proseg.msvcinventory.dto.response.TypeCountDto;
import com.proseg.msvcinventory.entity.enums.AssetExpiryCategory;
import com.proseg.msvcinventory.entity.enums.AssetStatus;
import com.proseg.msvcinventory.mapper.AssetMapper;
import com.proseg.msvcinventory.repository.AssetRepository;
import com.proseg.msvcinventory.repository.projection.TypeCountProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetDashboardServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("America/Costa_Rica");

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AssetMapper assetMapper;

    @InjectMocks
    private AssetDashboardServiceImpl service;

    @Test
    void getSummary_mapsWarrantyAndFirmwareCounts() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate limit = today.plusDays(30);
        when(assetRepository.countByTypeAndStatus(AssetStatus.APROBADO)).thenReturn(List.of());
        when(assetRepository.countByStatusAndWarrantyEndDateBefore(AssetStatus.APROBADO, today)).thenReturn(1L);
        when(assetRepository.countByStatusAndWarrantyEndDateBetween(AssetStatus.APROBADO, today, limit)).thenReturn(2L);
        when(assetRepository.countByStatusAndWarrantyEndDateAfter(AssetStatus.APROBADO, limit)).thenReturn(3L);
        when(assetRepository.countByStatusAndWarrantyEndDateIsNull(AssetStatus.APROBADO)).thenReturn(4L);
        when(assetRepository.countByStatusAndFirmwareSupportEndDateBefore(AssetStatus.APROBADO, today)).thenReturn(5L);
        when(assetRepository.countByStatusAndFirmwareSupportEndDateBetween(AssetStatus.APROBADO, today, limit)).thenReturn(6L);
        when(assetRepository.countByStatusAndFirmwareSupportEndDateAfter(AssetStatus.APROBADO, limit)).thenReturn(7L);
        when(assetRepository.countByStatusAndFirmwareSupportEndDateIsNull(AssetStatus.APROBADO)).thenReturn(8L);

        AssetDashboardResponseDto summary = service.getSummary(30);

        assertThat(summary.getWarranty())
                .extracting(ExpiryStatsDto::getExpired, ExpiryStatsDto::getExpiringSoon, ExpiryStatsDto::getValid, ExpiryStatsDto::getNoDate)
                .containsExactly(1L, 2L, 3L, 4L);
        assertThat(summary.getFirmware())
                .extracting(ExpiryStatsDto::getExpired, ExpiryStatsDto::getExpiringSoon, ExpiryStatsDto::getValid, ExpiryStatsDto::getNoDate)
                .containsExactly(5L, 6L, 7L, 8L);
    }

    @Test
    void getSummary_clampsDaysAndBetweenUpperBound() {
        LocalDate today = LocalDate.now(ZONE);
        when(assetRepository.countByTypeAndStatus(AssetStatus.APROBADO)).thenReturn(List.of());

        AssetDashboardResponseDto oneDay = service.getSummary(0);
        AssetDashboardResponseDto maxDays = service.getSummary(1000);

        assertThat(oneDay.getDays()).isEqualTo(1);
        assertThat(maxDays.getDays()).isEqualTo(365);
        verify(assetRepository).countByStatusAndWarrantyEndDateBetween(AssetStatus.APROBADO, today, today.plusDays(1));
        verify(assetRepository).countByStatusAndFirmwareSupportEndDateBetween(AssetStatus.APROBADO, today, today.plusDays(1));
        verify(assetRepository).countByStatusAndWarrantyEndDateBetween(AssetStatus.APROBADO, today, today.plusDays(365));
        verify(assetRepository).countByStatusAndFirmwareSupportEndDateBetween(AssetStatus.APROBADO, today, today.plusDays(365));
    }

    @Test
    void getSummary_limitsByTypeToEight() {
        List<TypeCountProjection> rows = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> typeCount("Tipo " + i, i))
                .toList();
        when(assetRepository.countByTypeAndStatus(AssetStatus.APROBADO)).thenReturn(rows);

        AssetDashboardResponseDto summary = service.getSummary(30);

        assertThat(summary.getByType()).hasSize(8);
        assertThat(summary.getByType())
                .extracting(TypeCountDto::getName, TypeCountDto::getTotal)
                .containsExactly(
                        tuple("Tipo 1", 1L),
                        tuple("Tipo 2", 2L),
                        tuple("Tipo 3", 3L),
                        tuple("Tipo 4", 4L),
                        tuple("Tipo 5", 5L),
                        tuple("Tipo 6", 6L),
                        tuple("Tipo 7", 7L),
                        tuple("Tipo 8", 8L)
                );
    }

    @Test
    @SuppressWarnings("unchecked")
    void findExpiringAssets_sortsWarrantyExpiredByWarrantyEndDateAsc() {
        Pageable requested = PageRequest.of(1, 5, Sort.by("serialNumber").descending());
        when(assetRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.findExpiringAssets(AssetExpiryCategory.WARRANTY_EXPIRED, 30, requested);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(assetRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(1);
        assertThat(used.getPageSize()).isEqualTo(5);
        assertThat(used.getSort()).isEqualTo(Sort.by("warrantyEndDate").ascending());
    }

    private static TypeCountProjection typeCount(String name, long total) {
        return new TypeCountProjection() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }
}
