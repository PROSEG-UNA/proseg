package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID>, JpaSpecificationExecutor<Asset> {

    @EntityGraph("Asset.withRelations")
    Page<Asset> findAll(Specification<Asset> spec, Pageable pageable);

    @EntityGraph("Asset.withRelations")
    Optional<Asset> findById(UUID id);

    @EntityGraph("Asset.withRelations")
    Page<Asset> findByLocationId(UUID locationId, Pageable pageable);

    @EntityGraph("Asset.withRelations")
    Page<Asset> findByLocationFloorBuildingCampusId(UUID campusId, Pageable pageable);

    @EntityGraph("Asset.withRelations")
    Page<Asset> findByModelTypeId(UUID typeId, Pageable pageable);

    boolean existsByLocationId(UUID locationId);

    boolean existsByModelId(UUID modelId);

    boolean existsByModelBrandId(UUID brandId);

    boolean existsByModelTypeId(UUID typeId);

    boolean existsBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndIdNot(String serialNumber, UUID id);

    boolean existsByAssetNumber(String assetNumber);

    boolean existsByAssetNumberAndIdNot(String assetNumber, UUID id);
}
