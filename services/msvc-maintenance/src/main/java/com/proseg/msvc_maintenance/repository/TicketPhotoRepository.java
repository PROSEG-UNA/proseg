package com.proseg.msvc_maintenance.repository;

import com.proseg.msvc_maintenance.entity.TicketPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketPhotoRepository extends JpaRepository<TicketPhoto, UUID> {
}
