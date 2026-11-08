package com.proseg.msvcinventory.repository;

import com.proseg.msvcinventory.entity.Asset;
import com.proseg.msvcinventory.entity.enums.AssetStatus;
import com.proseg.msvcinventory.repository.projection.TypeCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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

    boolean existsByExecutingUnitId(UUID executingUnitId);

    boolean existsByEmployeeId(UUID employeeId);

    boolean existsBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndIdNot(String serialNumber, UUID id);

    boolean existsByAssetNumber(String assetNumber);

    boolean existsByAssetNumberAndIdNot(String assetNumber, UUID id);

    long countByStatus(AssetStatus status);

    long countByStatusAndWarrantyEndDateBefore(AssetStatus status, LocalDate date);

    long countByStatusAndWarrantyEndDateBetween(AssetStatus status, LocalDate from, LocalDate to);

    long countByStatusAndWarrantyEndDateAfter(AssetStatus status, LocalDate date);

    long countByStatusAndWarrantyEndDateIsNull(AssetStatus status);

    long countByStatusAndFirmwareSupportEndDateBefore(AssetStatus status, LocalDate date);

    long countByStatusAndFirmwareSupportEndDateBetween(AssetStatus status, LocalDate from, LocalDate to);

    long countByStatusAndFirmwareSupportEndDateAfter(AssetStatus status, LocalDate date);

    long countByStatusAndFirmwareSupportEndDateIsNull(AssetStatus status);

    @Query("""
            SELECT a.model.type.name AS name, COUNT(a) AS total
            FROM Asset a
            WHERE a.status = :status
            GROUP BY a.model.type.name
            ORDER BY COUNT(a) DESC
            """)
    List<TypeCountProjection> countByTypeAndStatus(@Param("status") AssetStatus status);
}
