package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.AssetImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AssetImageRepository extends JpaRepository<AssetImage, UUID> {

    Page<AssetImage> findByAssetId(UUID assetId, Pageable pageable);

    boolean existsByAssetId(UUID assetId);
}
