package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.DriverDaySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DriverDayScheduleRepository extends JpaRepository<DriverDaySchedule, UUID> {

    List<DriverDaySchedule> findAllByDateBetween(LocalDate from, LocalDate to);

    List<DriverDaySchedule> findAllByDriverIdAndDateBetween(UUID driverId, LocalDate from, LocalDate to);
}
