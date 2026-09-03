package com.proseg.msvc_maintenance.repository;

import com.proseg.msvc_maintenance.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {
}
