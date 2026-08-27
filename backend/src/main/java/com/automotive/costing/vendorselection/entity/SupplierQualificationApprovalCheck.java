package com.automotive.costing.vendorselection.entity;

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
@Table(name = "supplier_qualification_approval_check")
public class SupplierQualificationApprovalCheck extends BaseAuditableEntity {

    @Id
    @Column(name = "check_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String checkId;

    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "qualification_id", length = 36, columnDefinition = "CHAR(36)")
    private String qualificationId;

    @Column(name = "recommendation_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String recommendationId;

    @Column(name = "recommendation_status", length = 30, nullable = false)
    private String recommendationStatus;

    @Column(name = "recommended_supplier_id", length = 36, columnDefinition = "CHAR(36)")
    private String recommendedSupplierId;

    @Column(name = "check_passed", nullable = false)
    private boolean checkPassed;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @Column(name = "checked_by", length = 36, columnDefinition = "CHAR(36)")
    private String checkedBy;
}
