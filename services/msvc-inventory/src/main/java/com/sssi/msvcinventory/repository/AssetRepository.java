package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID>, JpaSpecificationExecutor<Asset> {

    Page<Asset> findByLocationId(UUID locationId, Pageable pageable);

    Page<Asset> findByLocationSiteId(UUID siteId, Pageable pageable);

    Page<Asset> findByModelTypeId(UUID typeId, Pageable pageable);

    boolean existsByLocationId(UUID locationId);

    boolean existsByModelId(UUID modelId);

    boolean existsByModelBrandId(UUID brandId);

    boolean existsByModelTypeId(UUID typeId);
}
