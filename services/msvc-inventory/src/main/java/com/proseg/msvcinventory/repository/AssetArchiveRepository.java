package com.proseg.msvcinventory.repository;

import com.proseg.msvcinventory.entity.AssetArchive;
import com.proseg.msvcinventory.repository.projection.AssetCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssetArchiveRepository extends JpaRepository<AssetArchive, UUID> {

    Page<AssetArchive> findByAssetId(UUID assetId, Pageable pageable);

    List<AssetArchive> findByAssetId(UUID assetId);

    boolean existsByAssetId(UUID assetId);

    @Query("""
            SELECT archive.asset.id AS assetId, COUNT(archive) AS total
            FROM AssetArchive archive
            WHERE archive.asset.id IN :assetIds
            GROUP BY archive.asset.id
            """)
    List<AssetCountProjection> countByAssetIds(@Param("assetIds") Collection<UUID> assetIds);
}
