package com.sssi.msvc_maintenance.entity;

import com.sssi.common.entity.BaseEntity;
import com.sssi.msvc_maintenance.entity.enums.MaintenancePriority;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "maintenance_request_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE maintenance_request_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class MaintenanceRequest extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /**
     * Cross-service reference to asset in msvc-inventory — no DB-level FK constraint.
     */
    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    @Builder.Default
    private MaintenanceStatus status = MaintenanceStatus.PENDING;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    @Builder.Default
    private MaintenancePriority priority = MaintenancePriority.LOW;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @OneToMany(mappedBy = "maintenanceRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MaintenanceTechnician> technicians;
}