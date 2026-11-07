package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.DriverMonthBusinessDays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverMonthBusinessDaysRepository extends JpaRepository<DriverMonthBusinessDays, UUID> {

    List<DriverMonthBusinessDays> findAllByYearAndMonth(Integer year, Integer month);

    Optional<DriverMonthBusinessDays> findByDriverIdAndYearAndMonth(UUID driverId, Integer year, Integer month);
}
