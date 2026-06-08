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
@Table(name = "maintenance_record_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE maintenance_record_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class MaintenanceRecord extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maintenance_register_id", nullable = false)
    private MaintenanceRegister maintenanceRegister;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Filterable(type = FilterType.TEXT, nestedPaths = {"name", "legalId"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Filterable(type = FilterType.TEXT)
    @Column(name = "keycloak_user_id", nullable = false)
    private String keycloakUserId;

    @Filterable(type = FilterType.TEXT)
    @Column(name = "user_email", length = 254)
    private String userEmail;

    @Filterable(type = FilterType.TEXT)
    @Column(columnDefinition = "TEXT", nullable = false)
    @Pattern(regexp = ValidationUtils.SAFE_TEXT_REGEX, message = "La descripción contiene caracteres inválidos")
    private String description;
}
