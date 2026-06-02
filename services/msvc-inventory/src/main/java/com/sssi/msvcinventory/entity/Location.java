package com.sssi.msvcinventory.entity;

import com.sssi.common.entity.BaseEntity;
import com.sssi.common.specification.Filterable;
import com.sssi.common.specification.FilterType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "location_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE location_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Location extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Filterable(type = FilterType.TEXT, nestedPaths = {"name", "building.name", "building.campus.name"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String description;
}