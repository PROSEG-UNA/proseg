package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.DriverMonth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverMonthRepository extends JpaRepository<DriverMonth, UUID> {

    Optional<DriverMonth> findByDriverIdAndYearAndMonth(UUID driverId, Integer year, Integer month);

    List<DriverMonth> findByDriverIdOrderByYearAscMonthAsc(UUID driverId);

    List<DriverMonth> findByYearAndMonth(Integer year, Integer month);
}
