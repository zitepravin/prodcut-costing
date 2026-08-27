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
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "capability_expiry_run")
public class CapabilityExpiryRun extends BaseAuditableEntity {
    @Id
    @Column(name = "run_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String runId;
    @Column(name = "ran_at", nullable = false)
    private Instant ranAt;
    @Column(name = "ran_by", length = 36, columnDefinition = "CHAR(36)")
    private String ranBy;
    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;
    @Column(name = "capabilities_expired", nullable = false)
    private int capabilitiesExpired;
    @Column(name = "trigger_source", length = 20, nullable = false)
    private String triggerSource = "MANUAL";
    @Column(name = "notes", length = 1000)
    private String notes;
}
