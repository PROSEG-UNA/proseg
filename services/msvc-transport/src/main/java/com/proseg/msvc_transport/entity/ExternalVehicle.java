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

import java.util.UUID;

@Entity
@Table(name = "external_vehicle_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE external_vehicle_table SET is_deleted = true WHERE id = ?")
public class ExternalVehicle extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false, length = 200)
    private String provider;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false, unique = true, length = 100)
    private String plate;

    @Filterable(type = FilterType.TEXT)
    private Integer capacity;

    @Builder.Default
    private boolean available = true;
}
