package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
import com.proseg.common.specification.FilterType;
import com.proseg.common.specification.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tour_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE tour_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Tour extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String name;

    @Filterable(type = FilterType.TEXT)
    @Column(length = 50)
    private String externalNumber;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String origin;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String destination;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private LocalDateTime startDate;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private LocalDateTime endDate;

    @Filterable(type = FilterType.TEXT)
    @Builder.Default
    private String status = "PLANNED";

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private Integer priority;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private Integer passengers;

    @Filterable(type = FilterType.TEXT)
    @Column(length = 100)
    private String requestedVehicle;

    @Filterable(type = FilterType.TEXT)
    @Column(length = 100)
    private String requestedVehicleType;

    @Filterable(type = FilterType.TEXT)
    @Column(length = 200)
    private String requestedDriver;

    @Filterable(type = FilterType.TEXT)
    private String responsible;

    @Filterable(type = FilterType.TEXT)
    private String executingUnit;

    @Filterable(type = FilterType.TEXT)
    @Column(length = 50)
    private String modality;

    private Integer durationDays;

    @Column(length = 20)
    private String departureTime;

    @Column(length = 20)
    private String returnTime;

    @Column(columnDefinition = "TEXT")
    private String observations;
}
