package com.sssi.msvc_transport.repository;

import com.sssi.msvc_transport.entity.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface TourRepository extends JpaRepository<Tour, UUID>, JpaSpecificationExecutor<Tour> {

    List<Tour> findAllByStatusIn(Collection<String> statuses);

    List<Tour> findAllByStartDateBetween(LocalDateTime from, LocalDateTime to);
}
