package com.sssi.msvc_maintenance.entity;

import com.sssi.common.entity.BaseEntity;
import com.sssi.common.specification.Filterable;
import com.sssi.common.specification.FilterType;
import com.sssi.common.utils.ValidationUtils;
import com.sssi.msvc_maintenance.entity.enums.MaintenancePriority;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
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

    @Filterable(type = FilterType.TEXT, nestedPaths = {"name", "legalId"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Filterable(type = FilterType.TEXT)
    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    @Pattern(regexp = ValidationUtils.SAFE_TEXT_REGEX, message = "El título contiene caracteres inválidos")
    private String title;

    @Filterable(type = FilterType.TEXT)
    @Column(columnDefinition = "TEXT")
    @Pattern(regexp = ValidationUtils.SAFE_TEXT_REGEX, message = "La descripción contiene caracteres inválidos")
    private String description;

    @Filterable(type = FilterType.ENUM)
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    @Builder.Default
    private MaintenanceStatus status = MaintenanceStatus.PENDING;

    @Filterable(type = FilterType.ENUM)
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    @Builder.Default
    private MaintenancePriority priority = MaintenancePriority.LOW;

    @Filterable(type = FilterType.DATE)
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Filterable(type = FilterType.TEXT)
    @Column(columnDefinition = "TEXT")
    @Pattern(regexp = ValidationUtils.SAFE_TEXT_REGEX, message = "Las observaciones contienen caracteres inválidos")
    private String observations;

    @OneToMany(mappedBy = "maintenanceRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MaintenanceTechnician> technicians;
}