package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.AssetComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssetComponentRepository extends JpaRepository<AssetComponent, UUID> {

    List<AssetComponent> findByAssetId(UUID assetId);
}

