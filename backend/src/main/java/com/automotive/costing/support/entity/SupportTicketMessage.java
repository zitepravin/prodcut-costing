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
@Table(name = "support_ticket_message")
public class SupportTicketMessage {
    @Id
    @Column(name = "message_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String messageId;

    @Column(name = "ticket_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String ticketId;

    @Column(name = "author_user_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String authorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", length = 20, nullable = false)
    private SupportAuthorType authorType;

    @Column(name = "message_body", length = 4000, nullable = false)
    private String messageBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
