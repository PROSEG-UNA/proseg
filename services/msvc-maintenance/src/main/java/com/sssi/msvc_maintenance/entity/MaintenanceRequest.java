package com.sssi.msvc_maintenance.entity;

import com.sssi.common.entity.BaseEntity;
import com.sssi.common.specification.Filterable;
import com.sssi.common.specification.FilterType;
import com.sssi.common.utils.ValidationUtils;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalTime;
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
    @Column(columnDefinition = "TEXT")
    @Pattern(regexp = ValidationUtils.SAFE_TEXT_REGEX, message = "La descripción contiene caracteres inválidos")
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "maintenance_request_email",
            joinColumns = @JoinColumn(name = "maintenance_request_id"),
            inverseJoinColumns = @JoinColumn(name = "maintenance_email_id")
    )
    private List<MaintenanceEmail> emails;

    @Filterable(type = FilterType.ENUM)
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    @Builder.Default
    private MaintenanceStatus status = MaintenanceStatus.PENDING;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "campus_id", nullable = false)
    private UUID campusId;

    @Column(name = "building_id")
    private UUID buildingId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "maintenance_request_user_company",
            joinColumns = @JoinColumn(name = "maintenance_request_id"),
            inverseJoinColumns = @JoinColumn(name = "user_company_id")
    )
    private List<UserCompany> assignedTechnicians;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_company_id")
    private UserCompany responsibleUserCompany;
}