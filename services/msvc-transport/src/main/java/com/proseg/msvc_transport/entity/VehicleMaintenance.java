package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
import com.proseg.common.specification.FilterType;
import com.proseg.common.specification.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vehicle_maintenance_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE vehicle_maintenance_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class VehicleMaintenance extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @Filterable(type = FilterType.TEXT, nestedPaths = {"id", "plate", "brand", "model"})
    private Vehicle vehicle;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String title;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String type;

    @Filterable(type = FilterType.DATE)
    private LocalDate scheduledDate;

    private BigDecimal cost;

    @Filterable(type = FilterType.TEXT)
    @Builder.Default
    private String status = "PLANNED";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Filterable(type = FilterType.DATE)
    private LocalDate startDate;

    @Filterable(type = FilterType.DATE)
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String reason;
}
