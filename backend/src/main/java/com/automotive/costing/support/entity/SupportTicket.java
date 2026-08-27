package com.automotive.costing.support.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "support_ticket")
public class SupportTicket extends BaseAuditableEntity {
    @Id
    @Column(name = "ticket_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String ticketId;

    @Column(name = "ticket_number", length = 20, nullable = false, unique = true)
    private String ticketNumber;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    /**
     * Ticket opener user id. Same physical column as {@link #getCreatedBy()}
     * ({@code created_by}). Do not map a second JPA property to that column —
     * Hibernate 6 rejects duplicate column mappings (startup MappingException).
     */
    public String getCreatedByUserId() {
        return getCreatedBy();
    }

    public void setCreatedByUserId(String createdByUserId) {
        setCreatedBy(createdByUserId);
    }

    @Column(name = "subject", length = 200, nullable = false)
    private String subject;

    @Column(name = "description", length = 4000, nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 40, nullable = false)
    private SupportTicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20, nullable = false)
    private SupportTicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private SupportTicketStatus status;

    @Column(name = "assigned_to", length = 36, columnDefinition = "CHAR(36)")
    private String assignedToUserId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;
}
