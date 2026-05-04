package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {

    Page<Image> findByAssetId(UUID assetId, Pageable pageable);

    boolean existsByAssetId(UUID assetId);
}
