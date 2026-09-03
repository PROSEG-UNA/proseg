package com.proseg.msvcinventory.entity;

import com.proseg.common.entity.BaseEntity;
import com.proseg.common.specification.Filterable;
import com.proseg.common.specification.FilterType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.UuidGenerator;

import java.util.List;
import java.util.UUID;

@NamedEntityGraph(
        name = "Building.withCampus",
        attributeNodes = {
                @NamedAttributeNode("campus")
        }
)
@Entity
@Table(name = "building_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE building_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Building extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String name;

    @Filterable(type = FilterType.TEXT, nestedPaths = {"name"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @OneToMany(mappedBy = "building")
    private List<Floor> floors;

    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BuildingEmail> emails;
}
