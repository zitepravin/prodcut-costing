package com.automotive.costing.support.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "support_ticket_internal_note")
public class SupportTicketInternalNote {
    @Id
    @Column(name = "note_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String noteId;

    @Column(name = "ticket_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String ticketId;

    @Column(name = "author_user_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String authorUserId;

    @Column(name = "note_body", length = 4000, nullable = false)
    private String noteBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
