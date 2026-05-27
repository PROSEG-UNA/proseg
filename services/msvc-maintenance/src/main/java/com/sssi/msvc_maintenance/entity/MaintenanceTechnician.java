package com.sssi.msvc_maintenance.entity;

import com.sssi.common.entity.BaseEntity;
import com.sssi.common.specification.Filterable;
import com.sssi.common.specification.FilterType;
import com.sssi.common.utils.ValidationUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "maintenance_technician_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE maintenance_technician_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class MaintenanceTechnician extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Filterable(type = FilterType.TEXT, nestedPaths = {"title"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maintenance_request_id", nullable = false)
    private MaintenanceRequest maintenanceRequest;

    @Filterable(type = FilterType.TEXT)
    @Column(name = "full_name", nullable = false)
    @Pattern(regexp = ValidationUtils.SAFE_TEXT_REGEX, message = "El nombre contiene caracteres inválidos")
    private String fullName;

    @Filterable(type = FilterType.TEXT)
    @Column(length = 100)
    @Pattern(regexp = ValidationUtils.SAFE_TEXT_REGEX, message = "El puesto contiene caracteres inválidos")
    private String position;

    @Filterable(type = FilterType.TEXT)
    @Pattern(regexp = ValidationUtils.EMAIL_REGEX, message = "El correo electrónico contiene caracteres inválidos")
    private String email;

    @Filterable(type = FilterType.TEXT)
    @Column(length = 50)
    @Pattern(regexp = ValidationUtils.PHONE_REGEX, message = "El teléfono contiene caracteres inválidos")
    private String phone;

    @Builder.Default
    @Column(nullable = false)
    private boolean leader = false;
}