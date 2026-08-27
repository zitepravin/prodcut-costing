package com.automotive.costing.rfq.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rfq_clarification_request")
public class RfqClarificationRequest extends BaseAuditableEntity {
    @Id
    @Column(name = "clarification_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String clarificationId;
    @Column(name = "rfq_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String rfqId;
    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;
    @Column(name = "question_text", length = 2000, nullable = false)
    private String questionText;
    @Column(name = "status", length = 20, nullable = false)
    private String status = "OPEN";
    @Column(name = "buyer_response", length = 2000)
    private String buyerResponse;
    @Column(name = "responded_by", length = 36)
    private String respondedBy;
    @Column(name = "responded_at")
    private Instant respondedAt;
}
