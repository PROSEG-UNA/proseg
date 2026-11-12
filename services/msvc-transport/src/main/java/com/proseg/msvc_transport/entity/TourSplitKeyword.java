package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
import com.proseg.msvc_transport.dto.tourprocessing.TourSplitMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(
        name = "tour_split_keyword_table",
        uniqueConstraints = @UniqueConstraint(name = "uk_tour_split_keyword_normalized_text", columnNames = "normalized_text")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourSplitKeyword extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String text;

    @Column(name = "normalized_text", nullable = false, length = 100)
    private String normalizedText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TourSplitMode mode;
}
