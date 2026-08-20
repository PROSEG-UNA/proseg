package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.AssetComponent;
import com.sssi.msvcinventory.repository.projection.AssetCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssetComponentRepository extends JpaRepository<AssetComponent, UUID> {

    List<AssetComponent> findByAssetId(UUID assetId);

    @Query("""
            SELECT component.asset.id AS assetId, COUNT(component) AS total
            FROM AssetComponent component
            WHERE component.asset.id IN :assetIds
            GROUP BY component.asset.id
            """)
    List<AssetCountProjection> countByAssetIds(@Param("assetIds") Collection<UUID> assetIds);
}

