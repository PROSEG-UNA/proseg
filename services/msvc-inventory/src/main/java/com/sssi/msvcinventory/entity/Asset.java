package com.sssi.msvcinventory.entity;

import com.sssi.common.entity.BaseEntity;
import com.sssi.common.specification.Filterable;
import com.sssi.common.specification.FilterType;
import com.sssi.msvcinventory.entity.enums.AssetStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@NamedEntityGraph(
        name = "Asset.withRelations",
        attributeNodes = {
                @NamedAttributeNode(value = "model", subgraph = "model-subgraph"),
                @NamedAttributeNode(value = "location", subgraph = "location-subgraph"),
                @NamedAttributeNode("networkInterface")
        },
        subgraphs = {
                @NamedSubgraph(name = "model-subgraph", attributeNodes = {
                        @NamedAttributeNode("brand"),
                        @NamedAttributeNode("type")
                }),
                @NamedSubgraph(name = "location-subgraph", attributeNodes = {
                        @NamedAttributeNode("site")
                })
        }
)
@Entity
@Table(name = "asset_table")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "asset_kind", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("GENERIC")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE asset_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Asset extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String name;

    @Filterable(type = FilterType.TEXT)
    private String description;

    @Filterable(type = FilterType.TEXT, nestedPaths = {"name", "brand.name", "type.name"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_model_id", nullable = false)
    private Model model;

    @Filterable(type = FilterType.TEXT, nestedPaths = {"name"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Filterable(type = FilterType.ENUM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status;

    @Filterable(type = FilterType.TEXT)
    @Column(name = "status_description")
    private String statusDescription;

    @Filterable(type = FilterType.DATE)
    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Filterable(type = FilterType.DATE)
    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Filterable(type = FilterType.DATE)
    @Column(name = "firmware_support_end_date")
    private LocalDate firmwareSupportEndDate;

    @OneToOne(mappedBy = "asset", cascade = CascadeType.ALL)
    private NetworkInterface networkInterface;

    @Filterable(type = FilterType.TEXT)
    @Column(name = "asset_number")
    private String assetNumber;

    @Filterable(type = FilterType.TEXT)
    @Column(name = "serial_number", unique = true)
    private String serialNumber;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;
}