package com.proseg.msvc_maintenance.entity;

import com.proseg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "ticket_comment_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketComment extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
}
