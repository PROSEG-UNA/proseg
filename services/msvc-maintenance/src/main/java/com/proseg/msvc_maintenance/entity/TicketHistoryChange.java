package com.proseg.msvc_maintenance.entity;

import com.proseg.common.entity.BaseEntity;
import com.proseg.msvc_maintenance.entity.enums.TicketHistoryChangeType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "ticket_history_change_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketHistoryChange extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TicketHistoryChangeType type;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "author_id")
    private String authorId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}