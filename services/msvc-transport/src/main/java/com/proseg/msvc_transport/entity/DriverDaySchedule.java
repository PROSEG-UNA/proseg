package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "driver_day_schedule_table",
        uniqueConstraints = @UniqueConstraint(name = "uk_driver_day_schedule", columnNames = {"driver_id", "schedule_date"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverDaySchedule extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate date;

    @Column(name = "ordinary_start", nullable = false)
    private LocalTime ordinaryStart;

    @Column(name = "ordinary_end", nullable = false)
    private LocalTime ordinaryEnd;
}
