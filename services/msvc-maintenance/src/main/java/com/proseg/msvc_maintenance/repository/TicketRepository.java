package com.proseg.msvc_maintenance.repository;

import com.proseg.msvc_maintenance.entity.Ticket;
import com.proseg.msvc_maintenance.entity.enums.TicketStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    @EntityGraph(value = "Ticket.withRelations")
    Page<Ticket> findAll(Pageable pageable);

    @EntityGraph(value = "Ticket.withRelations")
    Page<Ticket> findByCreatedBy(String createdBy, Pageable pageable);

    @EntityGraph(value = "Ticket.withRelations")
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    @EntityGraph(value = "Ticket.withRelations")
    Page<Ticket> findByCreatedByAndStatus(String createdBy, TicketStatus status, Pageable pageable);
}