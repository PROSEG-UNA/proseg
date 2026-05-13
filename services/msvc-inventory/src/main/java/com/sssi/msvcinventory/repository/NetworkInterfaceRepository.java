package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.NetworkInterface;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NetworkInterfaceRepository extends JpaRepository<NetworkInterface, UUID> {

    boolean existsByIpAddress(String ipAddress);

    boolean existsByIpAddressAndIdNot(String ipAddress, UUID id);

    boolean existsByMacAddress(String macAddress);

    boolean existsByMacAddressAndIdNot(String macAddress, UUID id);

    Optional<NetworkInterface> findByAssetId(UUID assetId);

    boolean existsByAssetId(UUID assetId);

    @Query(value = "SELECT * FROM network_interface WHERE asset_id = :assetId", nativeQuery = true)
    Optional<NetworkInterface> findByAssetIdIncludingDeleted(@Param("assetId") UUID assetId);

    @Modifying
    @Query(value = """
            UPDATE network_interface SET is_deleted = true, updated_at = NOW()
            WHERE is_deleted = false
            AND asset_id IN (
                SELECT a.id FROM asset_table a
                INNER JOIN model_table m ON a.asset_model_id = m.id
                WHERE m.asset_type_id = :typeId
                AND a.is_deleted = false
                AND m.is_deleted = false
            )
            """, nativeQuery = true)
    void softDeleteAllByAssetTypeId(@Param("typeId") UUID typeId);
}