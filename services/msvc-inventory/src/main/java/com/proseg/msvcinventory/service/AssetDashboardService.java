package com.proseg.msvcinventory.service;

import com.proseg.msvcinventory.dto.response.AssetDashboardResponseDto;
import com.proseg.msvcinventory.dto.response.AssetResponseDto;
import com.proseg.msvcinventory.entity.enums.AssetExpiryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssetDashboardService {

    AssetDashboardResponseDto getSummary(int days);

    Page<AssetResponseDto> findExpiringAssets(AssetExpiryCategory category, int days, Pageable pageable);
}
