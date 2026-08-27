package com.automotive.costing.purchase.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "purchase_request")
public class PurchaseRequest extends BaseAuditableEntity {

    @Id
    @Column(name = "purchase_request_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String purchaseRequestId;

    @Column(name = "pr_number", length = 40)
    private String prNumber;

    @Column(name = "customer_id", length = 36, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "plant_code", length = 40)
    private String plantCode;

    @Column(name = "requester_user_id", length = 36)
    private String requesterUserId;

    @Column(name = "department_code", length = 80)
    private String departmentCode;

    @Column(name = "buyer_user_id", length = 36)
    private String buyerUserId;

    @Column(name = "required_date")
    private LocalDate requiredDate;

    @Column(name = "priority_code", length = 20)
    private String priorityCode;

    @Column(name = "purpose_text", length = 500)
    private String purposeText;

    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "bom_node_id", length = 36, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "make_vs_buy_simulation_id", length = 36, columnDefinition = "CHAR(36)")
    private String makeVsBuySimulationId;

    @Column(name = "linked_rfq_id", length = 36, columnDefinition = "CHAR(36)")
    private String linkedRfqId;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "DRAFT";

    @Column(name = "reporting_currency", length = 3, nullable = false, columnDefinition = "CHAR(3)")
    private String reportingCurrency;

    @Column(name = "manual_override_reason", length = 1000)
    private String manualOverrideReason;

    @Column(name = "decision_code", length = 20)
    private String decisionCode;

    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;

    @Column(name = "decided_by", length = 36)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "released_by", length = 36)
    private String releasedBy;

    @Column(name = "external_po_reference", length = 100)
    private String externalPoReference;

    /** P3-09: NOT_LINKED | LINKED | FAILED | SUPERSEDED */
    @Column(name = "po_link_status", length = 20, nullable = false)
    private String poLinkStatus = "NOT_LINKED";

    @Column(name = "external_po_system", length = 30)
    private String externalPoSystem;

    @Column(name = "po_linked_at")
    private Instant poLinkedAt;

    @Column(name = "po_linked_source", length = 30)
    private String poLinkedSource;

    @Column(name = "po_link_event_id", length = 100)
    private String poLinkEventId;

    @Column(name = "po_link_notes", length = 1000)
    private String poLinkNotes;

    @Column(name = "notes", length = 1000)
    private String notes;
}
