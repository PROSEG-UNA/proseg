package com.proseg.msvc_forms.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.proseg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "form_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormRecord extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_type_id", nullable = false)
    private FormType formType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb", nullable = false)
    private JsonNode data;
}
