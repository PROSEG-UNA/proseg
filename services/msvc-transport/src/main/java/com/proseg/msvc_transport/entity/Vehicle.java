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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vehicle_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE vehicle_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Vehicle extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false, unique = true)
    private String plate;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String brand;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String model;

    @Filterable(type = FilterType.TEXT)
    @Column(name = "vehicle_year")
    private Integer year;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private Integer capacity;

    @Filterable(type = FilterType.TEXT)
    @Builder.Default
    private String status = "ACTIVE";

    @Filterable(type = FilterType.TEXT)
    private String type;

    @Builder.Default
    private boolean available = true;

    @Builder.Default
    private boolean underMaintenance = false;

    @Filterable(type = FilterType.DATE)
    private LocalDate lastMaintenanceDate;
}
