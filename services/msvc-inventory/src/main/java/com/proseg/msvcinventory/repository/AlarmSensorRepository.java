package com.proseg.msvcinventory.repository;

import com.proseg.msvcinventory.entity.AlarmSensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AlarmSensorRepository extends JpaRepository<AlarmSensor, UUID> {
}
