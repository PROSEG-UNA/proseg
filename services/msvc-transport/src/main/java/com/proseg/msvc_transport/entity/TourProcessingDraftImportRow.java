package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowResponseDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity
@Table(name = "tour_processing_draft_import_row_table", indexes = @Index(name = "idx_draft_import_row_draft", columnList = "draft_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourProcessingDraftImportRow extends BaseEntity {
    @Id
    private UUID id;
    @Column(name = "draft_id", nullable = false)
    private UUID draftId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private TourProcessingDraftRowResponseDto payload;
}
