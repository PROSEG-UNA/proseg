package com.sssi.msvc_maintenance.entity;

import com.sssi.common.entity.BaseEntity;
import com.sssi.common.specification.Filterable;
import com.sssi.common.specification.FilterType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(
        name = "user_company_table",
        uniqueConstraints = @UniqueConstraint(columnNames = {"keycloak_user_id", "company_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE user_company_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class UserCompany extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Filterable(type = FilterType.TEXT)
    @Column(name = "keycloak_user_id", nullable = false)
    private String keycloakUserId;

    @Filterable(type = FilterType.TEXT, nestedPaths = {"name", "legalId"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}