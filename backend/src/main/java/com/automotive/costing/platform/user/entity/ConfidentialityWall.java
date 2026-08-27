package com.automotive.costing.platform.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * SEC-04: "System shall support confidentiality wall configuration per RFQ event to
 * prevent cross-supplier data visibility." Configuration data model ONLY in this session -
 * M09 (RFQ Management, the source of a real RFQ event and the {@code supplier_quote}
 * table this is meant to restrict access to) is Phase 2 and doesn't exist yet.
 * {@code rfqEventReference} is a plain string forward reference (not a FK) to a future
 * {@code rfq.rfq_id}, the same pattern used for {@code ToolingRecord.toolingQuoteReference}
 * (M06) and every other not-yet-built-module reference throughout this project. There is
 * no enforcement point wired up yet - once M09/M10 exist, their supplier-quote queries
 * are where a wall configured here would actually need to be checked.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "confidentiality_wall")
public class ConfidentialityWall {

    @Id
    @Column(name = "wall_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String wallId;

    @Column(name = "rfq_event_reference", length = 100, nullable = false)
    private String rfqEventReference;

    /** Either allowedRoleCode or allowedUserId (or both) may be set - at least one should be, enforced at the service layer. */
    @Column(name = "allowed_role_code", length = 50)
    private String allowedRoleCode;

    @Column(name = "allowed_user_id", length = 36, columnDefinition = "CHAR(36)")
    private String allowedUserId;

    @Column(name = "created_by", length = 36, columnDefinition = "CHAR(36)")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
