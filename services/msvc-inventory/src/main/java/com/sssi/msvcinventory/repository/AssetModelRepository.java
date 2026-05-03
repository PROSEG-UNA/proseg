package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.AssetModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AssetModelRepository extends JpaRepository<AssetModel, UUID> {

    boolean existsByNameIgnoreCaseAndBrandId(String name, UUID brandId);

    boolean existsByNameIgnoreCaseAndBrandIdAndIdNot(String name, UUID brandId, UUID id);

    boolean existsByAssetTypeId(UUID assetTypeId);

    boolean existsByBrandId(UUID brandId);

    Page<AssetModel> findByBrandId(UUID brandId, Pageable pageable);

    Page<AssetModel> findByAssetTypeId(UUID assetTypeId, Pageable pageable);
}
