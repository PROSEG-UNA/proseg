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

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "company_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE company_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Company extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    @Pattern(regexp = ValidationUtils.SAFE_TEXT_REGEX, message = "El nombre contiene caracteres inválidos")
    private String name;

    @Filterable(type = FilterType.TEXT)
    @Column(name = "legal_id", unique = true, length = 50)
    @Pattern(regexp = ValidationUtils.LEGAL_ID_REGEX, message = "La cédula jurídica contiene caracteres inválidos")
    private String legalId;

    @Filterable(type = FilterType.TEXT)
    @Column(name = "contact_email")
    @Pattern(regexp = ValidationUtils.EMAIL_REGEX, message = "El correo electrónico contiene caracteres inválidos")
    private String contactEmail;

    @Filterable(type = FilterType.TEXT)
    @Column(name = "contact_phone", length = 50)
    @Pattern(regexp = ValidationUtils.PHONE_REGEX, message = "El teléfono contiene caracteres inválidos")
    private String contactPhone;

    @Filterable(type = FilterType.TEXT)
    @Column(columnDefinition = "TEXT")
    @Pattern(regexp = ValidationUtils.SAFE_TEXT_REGEX, message = "La dirección contiene caracteres inválidos")
    private String address;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserCompany> userCompanies;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MaintenanceRequest> maintenanceRequests;
}