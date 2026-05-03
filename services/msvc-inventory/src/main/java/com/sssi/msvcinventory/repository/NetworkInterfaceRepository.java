package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.NetworkInterface;
import org.springframework.data.jpa.repository.JpaRepository;
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
}