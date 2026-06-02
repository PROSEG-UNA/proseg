package com.sssi.msvc_maintenance.repository;

import com.sssi.msvc_maintenance.entity.TicketAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketAssetRepository extends JpaRepository<TicketAsset, UUID> {
}
