package com.sssi.msvc_maintenance.repository;

import com.sssi.msvc_maintenance.entity.TicketHistoryChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketHistoryChangeRepository extends JpaRepository<TicketHistoryChange, UUID> {
    Page<TicketHistoryChange> findByTicketIdOrderByCreatedAtDesc(UUID ticketId, Pageable pageable);
}